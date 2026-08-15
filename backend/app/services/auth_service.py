import os
import jwt
import logging
from datetime import datetime, timedelta, timezone
from typing import Optional
from fastapi import Depends, HTTPException, Security, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from .database_service import db_service

logger = logging.getLogger(__name__)

SECRET_KEY = os.getenv("JWT_SECRET_KEY", "unshorten-it-default-secret-key-1234567890")
ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("JWT_ACCESS_TOKEN_EXPIRE_MINUTES", "10080")) # Default 7 days

security = HTTPBearer(auto_error=False)


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """
    Create a JWT access token.
    """
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def decode_access_token(token: str) -> Optional[dict]:
    """
    Decode and validate a JWT access token.
    """
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except jwt.PyJWTError as e:
        logger.debug(f"JWT decode error: {e}")
        return None

async def get_current_user_optional(credentials: Optional[HTTPAuthorizationCredentials] = Security(security)) -> Optional[dict]:
    """
    FastAPI dependency to optionally authenticate a user. Returns None if credentials are missing or invalid.
    """
    if not credentials:
        return None
    token = credentials.credentials
    payload = decode_access_token(token)
    if not payload:
        return None
    username = payload.get("sub")
    if not username:
        return None
    if db_service.db is None:
        return None
    user = await db_service.db.users.find_one({"username": username})
    return user

async def get_current_user(credentials: Optional[HTTPAuthorizationCredentials] = Security(security)) -> dict:
    """
    FastAPI dependency to require authentication. Raises 401 if missing or invalid.
    """
    if not credentials:
        raise HTTPException(
            status_code=401,
            detail={"code": "UNAUTHORIZED", "message": "Authentication required"}
        )
    token = credentials.credentials
    payload = decode_access_token(token)
    if not payload:
        raise HTTPException(
            status_code=401,
            detail={"code": "INVALID_TOKEN", "message": "Access token is invalid or expired"}
        )
    username = payload.get("sub")
    if not username:
        raise HTTPException(
            status_code=401,
            detail={"code": "INVALID_TOKEN", "message": "Access token payload is invalid"}
        )
    if db_service.db is None:
        raise HTTPException(
            status_code=500,
            detail={"code": "DATABASE_ERROR", "message": "Database connection is not initialized"}
        )
    user = await db_service.db.users.find_one({"username": username})
    if not user:
        raise HTTPException(
            status_code=401,
            detail={"code": "USER_NOT_FOUND", "message": "User account could not be found"}
        )
    return user
