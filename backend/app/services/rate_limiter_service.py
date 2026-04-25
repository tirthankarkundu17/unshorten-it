import os
import logging
from fastapi import Request, HTTPException
from .cache_service import cache_service
from ..utils.network import get_client_ip
from typing import Optional

logger = logging.getLogger(__name__)

class RateLimiterService:
    def __init__(self):
        # Default limits, can be overridden by env vars
        # Format: X requests per Y seconds per IP
        self.requests_limit: int = self._get_env_var("RATE_LIMIT_REQUESTS", 60)
        self.window_seconds: int = self._get_env_var("RATE_LIMIT_WINDOW", 60)
        self.enabled: bool = self._get_env_var("RATE_LIMIT_ENABLED", "true").lower() == "true"

    def _get_env_var(self, var_name: str, default_value: Optional[int | str] = None) -> int | str:
        """Helper function to get environment variable with default value"""
        value = os.getenv(var_name, default_value)
        if isinstance(default_value, int):
            try:
                return int(value)
            except ValueError:
                return default_value
        return value

    async def check_rate_limit(self, request: Request):
        if not self.enabled:
            return

        # Extract client IP securely
        client_ip = get_client_ip(request)
        key = f"rl:{client_ip}"
        current_count = await cache_service.increment(key, self.window_seconds)

        if current_count > self.requests_limit:
            logger.warning(f"Rate limit exceeded for {client_ip}: {current_count}/{self.requests_limit}")
            raise HTTPException(
                status_code=429,
                detail="Too many requests. Please try again later.",
                headers={"Retry-After": str(self.window_seconds)}
            )

rate_limiter = RateLimiterService()