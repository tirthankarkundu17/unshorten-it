from pydantic import BaseModel, HttpUrl
from typing import List, Optional

class URLRequest(BaseModel):
    url: HttpUrl

class URLResponse(BaseModel):
    original_url: str
    final_url: str
    redirect_chain: List[str]
    response_time_ms: float
    error: Optional[str] = None
