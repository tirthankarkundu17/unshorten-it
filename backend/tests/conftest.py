import pytest
import pytest_asyncio
import os
import shutil
import httpx
from app.main import app
from app.services.cache_service import cache_service
from app.services.database_service import db_service

@pytest_asyncio.fixture(scope="session", autouse=True)
async def initialize_db_and_cache(setup_test_environment):
    cache_service.initialize()
    db_service.initialize()
    yield
    await cache_service.close()
    await db_service.close()


@pytest.fixture(scope="session", autouse=True)
def setup_test_environment():
    """
    Ensure tests use a temporary test-specific cache directory
    instead of production cache/redis.
    """
    # Force diskcache to use a temp directory for tests
    test_cache_dir = "/tmp/unshorten_test_cache"
    os.environ["DISKCACHE_DIR"] = test_cache_dir
    os.environ["REDIS_URL"] = "" # Ensure redis isn't used
    os.environ["MONGODB_DATABASE"] = "unshorten_it_test"
    
    yield
    
    # Cleanup after tests finish
    if os.path.exists(test_cache_dir):
        shutil.rmtree(test_cache_dir)

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
