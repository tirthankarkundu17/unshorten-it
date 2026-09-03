from fastapi import FastAPI, HTTPException, Request, BackgroundTasks, Depends
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
import time
import os
import logging
from pathlib import Path
from .utils.logging import setup_logging

# Initialize logging before other imports
setup_logging()
logger = logging.getLogger(__name__)

from .schemas import (
    URLRequest,
    URLResponse,
    ErrorResponse,
    AdminDashboardResponse,
    VisitorListResponse,
    VisitorRequestsResponse,
    AdminLoginRequest,
    AdminLoginResponse,
    AdminUserResponse,
)
from .services.url_service import unshorten_url
from .services.analytics_service import analytics_service
from .services.auth_service import auth_service, verify_admin_token
from dotenv import load_dotenv

load_dotenv()

# Load version from environment variable (injected via Docker/CI)
__version__ = os.getenv("APP_VERSION", "local-dev")

from contextlib import asynccontextmanager
import asyncio
from .services.cache_service import cache_service
from .services.tracking_service import tracking_service
from .services.database_service import db_service
from .services.security_service import urlhaus_sync_loop
from .services.rate_limiter_service import rate_limiter
from .utils.network import get_client_ip

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize cache and DB settings now that .env is loaded
    cache_service.initialize()
    db_service.initialize()
    await db_service.create_indexes()
    
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
# Add default local dev origins if not already present
for dev_origin in ["http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"]:
    if dev_origin not in allow_origins_list:
        allow_origins_list.append(dev_origin)
logger.info(f"Configured CORS allowed origins: {allow_origins_list}")
app.add_middleware(
    CORSMiddleware,
    allow_origins=allow_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
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
        
    return result

@app.post(
    "/api/v1/admin/login",
    response_model=AdminLoginResponse,
    tags=["Admin Authentication"],
    responses={
        401: {"model": ErrorResponse, "description": "Invalid credentials"},
        429: {"model": ErrorResponse, "description": "Too Many Requests"},
    }
)
async def admin_login(
    creds: AdminLoginRequest,
    _ = Depends(rate_limiter.check_login_rate_limit),
):
    """
    Authenticate admin credentials and issue a signed Bearer token.
    """
    if not auth_service.authenticate_admin(creds.username, creds.password):
        raise HTTPException(status_code=401, detail="Invalid username or password")
    
    token = auth_service.create_access_token(creds.username)
    return AdminLoginResponse(
        access_token=token,
        token_type="bearer",
        expires_in=86400,
    )

@app.get(
    "/api/v1/admin/me",
    response_model=AdminUserResponse,
    tags=["Admin Authentication"],
    responses={
        401: {"model": ErrorResponse, "description": "Unauthorized"}
    }
)
async def admin_me(username: str = Depends(verify_admin_token)):
    """
    Verify current admin authentication status.
    """
    return AdminUserResponse(username=username, authenticated=True)

@app.get(
    "/api/v1/admin/analytics/dashboard",
    response_model=AdminDashboardResponse,
    tags=["Admin Analytics"],
    responses={
        401: {"model": ErrorResponse, "description": "Unauthorized"},
        500: {"model": ErrorResponse, "description": "Internal Server Error"}
    }
)
async def get_admin_analytics_dashboard(_: str = Depends(verify_admin_token)):
    """
    Retrieve admin metrics for users, traffic history, geolocations, and request logs.
    """
    return await analytics_service.get_admin_dashboard_metrics()

@app.get(
    "/api/v1/admin/analytics/visitors",
    response_model=VisitorListResponse,
    tags=["Admin Analytics"],
    responses={
        401: {"model": ErrorResponse, "description": "Unauthorized"},
        500: {"model": ErrorResponse, "description": "Internal Server Error"}
    }
)
async def get_admin_analytics_visitors(
    limit: int = 50,
    skip: int = 0,
    _: str = Depends(verify_admin_token)
):
    """
    Retrieve list of visitors with geolocation and request counts.
    """
    return await analytics_service.get_visitors(limit=limit, skip=skip)

@app.get(
    "/api/v1/admin/analytics/visitors/{ip:path}/requests",
    response_model=VisitorRequestsResponse,
    tags=["Admin Analytics"],
    responses={
        401: {"model": ErrorResponse, "description": "Unauthorized"},
        500: {"model": ErrorResponse, "description": "Internal Server Error"}
    }
)
async def get_admin_analytics_visitor_requests(
    ip: str,
    limit: int = 100,
    _: str = Depends(verify_admin_token)
):
    """
    Retrieve all unshorten URL requests performed by a specific visitor IP.
    """
    return await analytics_service.get_visitor_requests(ip=ip, limit=limit)



