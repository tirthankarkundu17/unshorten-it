from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
import time

from .schemas import URLRequest, URLResponse, ErrorResponse
from .services.url_service import unshorten_url

app = FastAPI(
    title="Unshorten It API",
    description="A simple API to unshorten URLs and view the redirect chain and response times.",
    version="1.0.0",
)

# Best practice to add CORS middleware if this will be consumed by a frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
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
        status_code=422,
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
    return {"status": "ok", "timestamp": time.time()}

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
async def unshorten(request: URLRequest):
    """
    Unshorten a given URL and follow its redirect chain.
    """
    result = await unshorten_url(str(request.url))
    
    if result.get("error"):
        raise HTTPException(
            status_code=400,
            detail=result["error"]
        )
        
    return result
