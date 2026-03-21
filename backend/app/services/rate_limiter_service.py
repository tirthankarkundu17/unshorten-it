import os
import logging
from fastapi import Request, HTTPException
from .cache_service import cache_service

logger = logging.getLogger(__name__)

class RateLimiterService:
    def __init__(self):
        # Default limits, can be overridden by env vars
        # Format: X requests per Y seconds per IP
        try:
            self.requests_limit = int(os.getenv("RATE_LIMIT_REQUESTS", "60"))
            self.window_seconds = int(os.getenv("RATE_LIMIT_WINDOW", "60")) # 1 request per second on average
        except ValueError:
            self.requests_limit = 60
            self.window_seconds = 60
            
        self.enabled = os.getenv("RATE_LIMIT_ENABLED", "true").lower() == "true"

    async def check_rate_limit(self, request: Request):
        if not self.enabled:
            return

        # Extract client IP
        client_ip = request.client.host if request.client else "unknown"
        x_forwarded_for = request.headers.get("X-Forwarded-For")
        if x_forwarded_for:
            client_ip = x_forwarded_for.split(",")[0].strip()

        key = f"rl:{client_ip}"
        
        current_count = await cache_service.increment(key, self.window_seconds)
        
        if current_count > self.requests_limit:
            logger.warning(f"Rate limit exceeded for {client_ip}: {current_count}/{self.requests_limit}")
            raise HTTPException(
                status_code=429,
                detail="Too many requests. Please try again later."
            )

rate_limiter = RateLimiterService()
