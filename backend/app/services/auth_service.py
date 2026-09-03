import os
import time
import json
import base64
import hmac
import hashlib
import secrets
from typing import Optional
from fastapi import Request, HTTPException

from dotenv import load_dotenv

load_dotenv()

def _b64encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode().rstrip("=")

def _b64decode(data: str) -> bytes:
    padding = 4 - (len(data) % 4)
    if padding != 4:
        data += "=" * padding
    return base64.urlsafe_b64decode(data.encode())

class AuthService:
    def __init__(self):
        self._token_expiry_seconds = 86400  # 24 hours
        self.validate_environment()

    def validate_environment(self) -> None:
        """
        Ensures all required admin authentication environment variables are present.
        Fails fast and prevents application startup if any variable is missing.
        """
        missing_vars = []
        if not os.getenv("ADMIN_USERNAME"):
            missing_vars.append("ADMIN_USERNAME")
        if not os.getenv("ADMIN_PASSWORD"):
            missing_vars.append("ADMIN_PASSWORD")
        if not os.getenv("ADMIN_SECRET_KEY"):
            missing_vars.append("ADMIN_SECRET_KEY")

        if missing_vars:
            raise RuntimeError(
                f"Critical configuration error: Missing required environment variable(s): {', '.join(missing_vars)}. "
                "The application cannot start without admin credentials configured in the environment or .env file."
            )

    @property
    def admin_username(self) -> str:
        val = os.getenv("ADMIN_USERNAME")
        if not val:
            raise RuntimeError("ADMIN_USERNAME is not configured")
        return val

    @property
    def admin_password(self) -> str:
        val = os.getenv("ADMIN_PASSWORD")
        if not val:
            raise RuntimeError("ADMIN_PASSWORD is not configured")
        return val

    @property
    def secret_key(self) -> str:
        val = os.getenv("ADMIN_SECRET_KEY")
        if not val:
            raise RuntimeError("ADMIN_SECRET_KEY is not configured")
        return val


    def authenticate_admin(self, username: str, password: str) -> bool:
        """
        Constant-time comparison against configured admin credentials.
        """
        user_matches = secrets.compare_digest(username.strip(), self.admin_username)
        pass_matches = secrets.compare_digest(password, self.admin_password)
        return user_matches and pass_matches

    def create_access_token(self, username: str) -> str:
        """
        Generates an HMAC-SHA256 signed token with expiration.
        """
        now = int(time.time())
        payload = {
            "sub": username,
            "iat": now,
            "exp": now + self._token_expiry_seconds,
        }
        payload_bytes = json.dumps(payload).encode("utf-8")
        payload_b64 = _b64encode(payload_bytes)

        sig = hmac.new(
            self.secret_key.encode("utf-8"),
            payload_b64.encode("utf-8"),
            hashlib.sha256,
        ).hexdigest()

        return f"{payload_b64}.{sig}"

    def decode_and_verify_token(self, token: str) -> str:
        """
        Validates token signature and expiration. Returns username.
        """
        parts = token.strip().split(".")
        if len(parts) != 2:
            raise ValueError("Malformed token format")

        payload_b64, provided_sig = parts

        expected_sig = hmac.new(
            self.secret_key.encode("utf-8"),
            payload_b64.encode("utf-8"),
            hashlib.sha256,
        ).hexdigest()

        if not secrets.compare_digest(provided_sig, expected_sig):
            raise ValueError("Invalid token signature")

        try:
            payload_bytes = _b64decode(payload_b64)
            payload = json.loads(payload_bytes.decode("utf-8"))
        except Exception:
            raise ValueError("Corrupt token payload")

        exp = payload.get("exp", 0)
        if int(time.time()) > exp:
            raise ValueError("Token has expired")

        username = payload.get("sub")
        if not username:
            raise ValueError("Token missing subject")

        return username

auth_service = AuthService()

async def verify_admin_token(request: Request) -> str:
    """
    FastAPI dependency that enforces a valid admin Bearer token.
    """
    auth_header = request.headers.get("Authorization")
    if not auth_header:
        raise HTTPException(
            status_code=401,
            detail="Missing Authorization header"
        )

    parts = auth_header.strip().split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=401,
            detail="Invalid Authorization scheme. Expected 'Bearer <token>'"
        )

    token = parts[1]
    try:
        username = auth_service.decode_and_verify_token(token)
        return username
    except ValueError as e:
        raise HTTPException(
            status_code=401,
            detail=f"Authentication failed: {str(e)}"
        )
