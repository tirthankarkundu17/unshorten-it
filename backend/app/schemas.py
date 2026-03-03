from pydantic import BaseModel, HttpUrl
from typing import List, Optional, Any, Dict

class ErrorDetail(BaseModel):
    code: str
    message: str
    details: Optional[Any] = None

class ErrorResponse(BaseModel):
    error: ErrorDetail

class URLRequest(BaseModel):
    url: HttpUrl

class URLResponse(BaseModel):
    original_url: str
    final_url: str
    redirect_chain: List[str]
    response_time_ms: float
    cached: bool = False
