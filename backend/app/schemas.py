from pydantic import BaseModel, HttpUrl
from typing import List, Optional, Any, Dict
from datetime import datetime

class ErrorDetail(BaseModel):
    code: str
    message: str
    details: Optional[Any] = None

class ErrorResponse(BaseModel):
    error: ErrorDetail

class URLRequest(BaseModel):
    url: HttpUrl

class PagePreview(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    image_url: Optional[str] = None

class SecurityCheck(BaseModel):
    is_safe: bool
    threat_type: Optional[str] = None

class URLResponse(BaseModel):
    original_url: str
    final_url: str
    cleaned_url: str
    redirect_chain: List[str]
    response_time_ms: float
    cached: bool = False
    preview: Optional[PagePreview] = None
    security: Optional[SecurityCheck] = None

class GoogleLoginRequest(BaseModel):
    token: str

class UserResponse(BaseModel):
    id: str
    username: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"

class StatusResponse(BaseModel):
    status: str
    message: str

class HistoryItemResponse(BaseModel):
    id: str
    original_url: str
    final_url: str
    cleaned_url: str
    redirect_chain: List[str]
    response_time_ms: float
    timestamp: datetime
    preview: Optional[PagePreview] = None
    security: Optional[SecurityCheck] = None

