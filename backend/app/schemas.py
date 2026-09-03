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

class LocationStat(BaseModel):
    country: str
    country_code: Optional[str] = None
    city: Optional[str] = None
    lat: Optional[float] = None
    lng: Optional[float] = None
    count: int

class PlatformStat(BaseModel):
    platform: str
    count: int

class DailyTraffic(BaseModel):
    date: str
    requests: int
    unique_visitors: int

class RecentLog(BaseModel):
    timestamp: str
    ip: str
    platform: str
    url: str
    location: Optional[str] = None

class AdminDashboardResponse(BaseModel):
    total_requests: int
    total_unique_visitors: int
    top_locations: List[LocationStat]
    platforms: List[PlatformStat]
    traffic_history: List[DailyTraffic]
    recent_logs: List[RecentLog]

class VisitorLocation(BaseModel):
    city: Optional[str] = None
    state: Optional[str] = None
    country: Optional[str] = None
    lat: Optional[float] = None
    lng: Optional[float] = None

class VisitorItem(BaseModel):
    ip: str
    first_seen: str
    last_seen: str
    platforms: List[str]
    location: Optional[VisitorLocation] = None
    total_requests: int

class VisitorListResponse(BaseModel):
    visitors: List[VisitorItem]
    total_count: int

class VisitorRequestDetail(BaseModel):
    timestamp: str
    url: str
    platform: str

class VisitorRequestsResponse(BaseModel):
    ip: str
    total_requests: int
    requests: List[VisitorRequestDetail]

class AdminLoginRequest(BaseModel):
    username: str
    password: str

class AdminLoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    expires_in: int

class AdminUserResponse(BaseModel):
    username: str
    authenticated: bool


