from fastapi import FastAPI, HTTPException, Request, BackgroundTasks, Depends
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
import time
import os
from pathlib import Path
from datetime import datetime, timezone
from typing import List, Optional
from .utils.logging import setup_logging

# Initialize logging before other imports
setup_logging()

from .schemas import (
    URLRequest, URLResponse, ErrorResponse,
    GoogleLoginRequest, UserResponse, TokenResponse,
    StatusResponse, HistoryItemResponse
)
from .services.url_service import unshorten_url
from dotenv import load_dotenv

load_dotenv()

# Load version from environment variable (injected via Docker/CI)
__version__ = os.getenv("APP_VERSION", "local-dev")

from contextlib import asynccontextmanager
import asyncio
from google.oauth2 import id_token
from google.auth.transport import requests as google_requests
from .services.cache_service import cache_service
from .services.tracking_service import tracking_service
from .services.database_service import db_service
from .services.security_service import urlhaus_sync_loop
from .services.rate_limiter_service import rate_limiter
from .services.auth_service import (
    create_access_token, get_current_user, get_current_user_optional
)
from .utils.network import get_client_ip

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize cache and DB settings now that .env is loaded
    cache_service.initialize()
    db_service.initialize()
    
    # Start the background sync loop for URLhaus
    sync_task = asyncio.create_task(urlhaus_sync_loop())
    
    yield
    # Clean up connections on shutdown
    sync_task.cancel()
    try:
        await sync_task
    except asyncio.CancelledError:
        pass
        
    await cache_service.close()
    await db_service.close()

app = FastAPI(
    title="Unshorten It API",
    description="A simple API to unshorten URLs and view the redirect chain and response times.",
    version=__version__,
    lifespan=lifespan,
)

# Best practice to add CORS middleware if this will be consumed by a frontend
allow_origins_str = os.getenv("ALLOW_ORIGINS", "")
allow_origins_list = [origin.strip() for origin in allow_origins_str.split(",") if origin.strip()]
print(allow_origins_list)
app.add_middleware(
    CORSMiddleware,
    allow_origins=allow_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    if isinstance(exc.detail, dict):
        return JSONResponse(
            status_code=exc.status_code,
            content={
                "error": {
                    "code": exc.detail.get("code", "HTTP_ERROR"),
                    "message": exc.detail.get("message", str(exc.detail)),
                    "details": exc.detail.get("details")
                }
            }
        )
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": {"code": "HTTP_ERROR", "message": str(exc.detail)}}
    )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=400,
        content={
            "error": {
                "code": "VALIDATION_ERROR",
                "message": "Invalid request payload",
                "details": exc.errors()
            }
        }
    )

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"error": {"code": "INTERNAL_SERVER_ERROR", "message": "An unexpected error occurred."}}
    )

@app.get("/health", tags=["Health"])
async def health_check():
    return {
        "status": "ok",
        "version": __version__,
        "timestamp": time.time()
    }

@app.post(
    "/api/v1/unshorten", 
    response_model=URLResponse, 
    tags=["URL Operations"],
    responses={
        400: {"model": ErrorResponse, "description": "Invalid URL or Request Error"},
        422: {"model": ErrorResponse, "description": "Validation Error"},
        500: {"model": ErrorResponse, "description": "Internal Server Error"}
    }
)
async def unshorten(
    request: URLRequest, 
    raw_request: Request, 
    background_tasks: BackgroundTasks,
    current_user: Optional[dict] = Depends(get_current_user_optional),
    _ = Depends(rate_limiter.check_rate_limit)
):
    """
    Unshorten a given URL and follow its redirect chain.
    """
    # Track the request
    # Extract IP address securely
    client_ip = get_client_ip(raw_request)
        
    # Extract platform from header 'X-App-Platform'
    platform = raw_request.headers.get("X-App-Platform", "android")
    
    background_tasks.add_task(tracking_service.track_request, client_ip, platform, str(request.url))
    
    result = await unshorten_url(str(request.url))
    
    if result.get("error"):
        raise HTTPException(
            status_code=400,
            detail=result["error"]
        )
        
    # Save search to user history if logged in
    if current_user and db_service.db is not None:
        history_item = {
            "user_id": str(current_user["_id"]),
            "original_url": result["original_url"],
            "final_url": result["final_url"],
            "cleaned_url": result["cleaned_url"],
            "redirect_chain": result["redirect_chain"],
            "response_time_ms": result["response_time_ms"],
            "timestamp": datetime.now(timezone.utc),
            "preview": result.get("preview"),
            "security": result.get("security")
        }
        try:
            await db_service.db.history.insert_one(history_item)
        except Exception as e:
            print(f"Failed to log search history: {e}")
            
    return result

@app.post(
    "/api/v1/auth/google",
    response_model=TokenResponse,
    tags=["Authentication"],
    responses={
        400: {"model": ErrorResponse, "description": "Invalid Google Token"},
        500: {"model": ErrorResponse, "description": "Internal Server Error"}
    }
)
async def google_login(payload: GoogleLoginRequest):
    """
    Log in using Google SSO.
    """
    if db_service.db is None:
        raise HTTPException(
            status_code=500,
            detail={"code": "DATABASE_ERROR", "message": "Database not initialized"}
        )
        
    try:
        # Check if we are running in a test/mock environment
        if os.getenv("APP_ENV") == "test" and payload.token.startswith("mock-google-token"):
            username = "testuser"
            email = "testuser@example.com"
            google_id = "mock-google-id-123"
        else:
            GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID")
            id_info = id_token.verify_oauth2_token(
                payload.token,
                google_requests.Request(),
                GOOGLE_CLIENT_ID
            )
            email = id_info.get("email")
            google_id = id_info.get("sub")
            username = email.split("@")[0] if email else "google_user"
            
        if not email or not google_id:
            raise HTTPException(
                status_code=400,
                detail={"code": "INVALID_GOOGLE_TOKEN", "message": "Invalid Google token payload"}
            )
            
        # Find or create user
        user = await db_service.db.users.find_one({"google_id": google_id})
        if not user:
            # Check by email in case they existed
            user = await db_service.db.users.find_one({"email": email})
            if user:
                await db_service.db.users.update_one(
                    {"_id": user["_id"]},
                    {"$set": {"google_id": google_id, "auth_provider": "google"}}
                )
            else:
                user_doc = {
                    "username": username,
                    "email": email,
                    "google_id": google_id,
                    "auth_provider": "google",
                    "created_at": datetime.now(timezone.utc)
                }
                result = await db_service.db.users.insert_one(user_doc)
                user = await db_service.db.users.find_one({"_id": result.inserted_id})
                
        access_token = create_access_token(data={"sub": user["username"]})
        return TokenResponse(access_token=access_token)
        
    except ValueError as e:
        raise HTTPException(
            status_code=400,
            detail={"code": "INVALID_GOOGLE_TOKEN", "message": f"Token verification failed: {str(e)}"}
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"code": "INTERNAL_SERVER_ERROR", "message": f"Authentication failed: {str(e)}"}
        )

@app.get(
    "/api/v1/auth/me",
    response_model=UserResponse,
    tags=["Authentication"],
    responses={
        401: {"model": ErrorResponse, "description": "Unauthorized"},
        500: {"model": ErrorResponse, "description": "Internal Server Error"}
    }
)
async def get_me(current_user: dict = Depends(get_current_user)):
    """
    Get current user profile information.
    """
    return UserResponse(id=str(current_user["_id"]), username=current_user["username"])

@app.get(
    "/api/v1/history",
    response_model=List[HistoryItemResponse],
    tags=["User Operations"],
    responses={
        401: {"model": ErrorResponse, "description": "Unauthorized"},
        500: {"model": ErrorResponse, "description": "Internal Server Error"}
    }
)
async def get_history(current_user: dict = Depends(get_current_user)):
    """
    Get history of URLs unshortened by the authenticated user.
    """
    if db_service.db is None:
        raise HTTPException(
            status_code=500,
            detail={"code": "DATABASE_ERROR", "message": "Database not initialized"}
        )
    
    cursor = db_service.db.history.find({"user_id": str(current_user["_id"])}).sort("timestamp", -1)
    history_items = []
    async for doc in cursor:
        history_items.append(
            HistoryItemResponse(
                id=str(doc["_id"]),
                original_url=doc["original_url"],
                final_url=doc["final_url"],
                cleaned_url=doc["cleaned_url"],
                redirect_chain=doc["redirect_chain"],
                response_time_ms=doc["response_time_ms"],
                timestamp=doc["timestamp"],
                preview=doc.get("preview"),
                security=doc.get("security")
            )
        )
    return history_items

@app.post(
    "/api/v1/history/clear",
    response_model=StatusResponse,
    tags=["User Operations"],
    responses={
        401: {"model": ErrorResponse, "description": "Unauthorized"},
        500: {"model": ErrorResponse, "description": "Internal Server Error"}
    }
)
async def clear_history(current_user: dict = Depends(get_current_user)):
    """
    Clear all unshorten history for the authenticated user.
    """
    if db_service.db is None:
        raise HTTPException(
            status_code=500,
            detail={"code": "DATABASE_ERROR", "message": "Database not initialized"}
        )
    
    await db_service.db.history.delete_many({"user_id": str(current_user["_id"])})
    return StatusResponse(status="ok", message="History cleared successfully")
