from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import time

from .schemas import URLRequest, URLResponse
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

@app.get("/health", tags=["Health"])
async def health_check():
    return {"status": "healthy", "timestamp": time.time()}

@app.post("/api/v1/unshorten", response_model=URLResponse, tags=["URL Operations"])
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
