import pytest
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_health_check(async_client: AsyncClient):
    """
    Test the /health endpoint in main.py
    """
    response = await async_client.get("/health")
    assert response.status_code == 200
    
    data = response.json()
    assert data["status"] == "ok"
    assert "version" in data
    assert "timestamp" in data

@pytest.mark.asyncio
async def test_unshorten_bad_url_xss(async_client: AsyncClient):
    """
    Test the /api/v1/unshorten endpoint with a malformed URL containing an XSS payload.
    This should fail Pydantic validation and return a 400 error.
    """
    bad_url = "javascript:alert(1)" # Updated bad_url to trigger validation error
    response = await async_client.post("/api/v1/unshorten", json={"url": bad_url})
    
    # We expect 400 Bad Request due to pydantic validation
    assert response.status_code == 400
    
    data = response.json()
    assert data["error"]["code"] == "VALIDATION_ERROR"
    assert "Invalid request payload" in data["error"]["message"]

