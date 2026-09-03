import os
import tempfile
import shutil

# Configure required environment variables for the test suite before importing any application code.
# This ensures tests run in isolated environments (such as CI runners) where .env is not present.
test_cache_dir = tempfile.mkdtemp(prefix="unshorten_test_cache_")
os.environ["ADMIN_USERNAME"] = "admin"
os.environ["ADMIN_PASSWORD"] = "adminpassword123"
os.environ["ADMIN_SECRET_KEY"] = "test-admin-secret-key-1234567890abcdef"
os.environ["DISKCACHE_DIR"] = test_cache_dir
os.environ["REDIS_URL"] = ""

import pytest
import pytest_asyncio
import httpx
from app.main import app
from app.services.cache_service import cache_service

@pytest.fixture(scope="session", autouse=True)
def setup_test_environment():
    """
    Ensure tests use temporary test-specific cache directory and cleanup afterwards.
    """
    yield
    
    # Cleanup after tests finish
    if os.path.exists(test_cache_dir):
        shutil.rmtree(test_cache_dir, ignore_errors=True)

@pytest_asyncio.fixture
async def test_app():
    yield app

@pytest_asyncio.fixture
async def async_client(test_app):
    async with httpx.AsyncClient(
        transport=httpx.ASGITransport(app=test_app), 
        base_url="http://test"
    ) as client:
        yield client
