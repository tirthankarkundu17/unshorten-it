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
        self.requests_limit: int = int(self._get_env_var("RATE_LIMIT_REQUESTS", 60))
        self.window_seconds: int = int(self._get_env_var("RATE_LIMIT_WINDOW", 60))
        self.login_requests_limit: int = int(self._get_env_var("RATE_LIMIT_LOGIN_REQUESTS", 10))
        self.login_window_seconds: int = int(self._get_env_var("RATE_LIMIT_LOGIN_WINDOW", 60))
        self.enabled: bool = str(self._get_env_var("RATE_LIMIT_ENABLED", "true")).lower() == "true"

    def _get_env_var(self, var_name: str, default_value: Optional[int | str] = None) -> int | str:
        """Helper function to get environment variable with default value"""
        value = os.getenv(var_name, default_value)
        if isinstance(default_value, int):
            try:
                return int(value)
            except (ValueError, TypeError):
                return default_value
        return value if value is not None else (default_value if default_value is not None else "")

    async def check_rate_limit(
        self,
        request: Request,
        key_prefix: str = "rl",
        limit: Optional[int] = None,
        window: Optional[int] = None,
    ):
        if not self.enabled:
            return

        # Extract client IP securely
        client_ip = get_client_ip(request)
        key = f"{key_prefix}:{client_ip}"
        eff_limit = limit if limit is not None else self.requests_limit
        eff_window = window if window is not None else self.window_seconds

        current_count = await cache_service.increment(key, eff_window)

        if current_count > eff_limit:
            logger.warning(
                f"Rate limit exceeded for {client_ip} on '{key_prefix}': {current_count}/{eff_limit}"
            )
            raise HTTPException(
                status_code=429,
                detail="Too many requests. Please try again later.",
                headers={"Retry-After": str(eff_window)},
            )

    async def check_login_rate_limit(self, request: Request):
        """
        Enforce dedicated rate limits on admin login attempts to prevent brute-force attacks.
        """
        await self.check_rate_limit(
            request,
            key_prefix="rl:login",
            limit=self.login_requests_limit,
            window=self.login_window_seconds,
        )

rate_limiter = RateLimiterService()