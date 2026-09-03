import pytest
from httpx import AsyncClient
from unittest.mock import patch, AsyncMock
from app.services.rate_limiter_service import rate_limiter

@pytest.mark.asyncio
async def test_rate_limiting_allowed(async_client: AsyncClient):
    """
    Test that requests within the rate limit are allowed (Status 200).
    """
    with patch("app.services.rate_limiter_service.cache_service.increment", new_callable=AsyncMock) as mock_increment:
        # Mock that we are at request #5 (well below the limit of 60)
        mock_increment.return_value = 5
        
        # We need to mock the unshorten_url service call as well to avoid real network requests
        with patch("app.main.unshorten_url", new_callable=AsyncMock) as mock_unshorten:
            mock_unshorten.return_value = {
                "original_url": "https://google.com",
                "final_url": "https://example.com", 
                "cleaned_url": "https://example.com",
                "redirect_chain": [],
                "response_time_ms": 12.34,
                "cached": False,
                "security": {"is_safe": True, "threat_type": None}
            }
            
            response = await async_client.post("/api/v1/unshorten", json={"url": "https://google.com"})
            
            assert response.status_code == 200
            mock_increment.assert_called_once()


@pytest.mark.asyncio
async def test_rate_limiting_exceeded(async_client: AsyncClient):
    """
    Test that requests exceeding the rate limit are blocked (Status 429).
    """
    with patch("app.services.rate_limiter_service.cache_service.increment", new_callable=AsyncMock) as mock_increment:
        # Mock that we just hit request #61 (above the limit of 60)
        mock_increment.return_value = 61
        
        response = await async_client.post("/api/v1/unshorten", json={"url": "https://google.com"})
        
        # Should return 429 Too Many Requests
        assert response.status_code == 429
        data = response.json()
        assert "Too many requests" in data["error"]["message"]
        mock_increment.assert_called_once()


@pytest.mark.asyncio
async def test_admin_login_rate_limiting_exceeded(async_client: AsyncClient):
    """
    Test that admin login requests exceeding the login rate limit return HTTP 429.
    """
    with patch("app.services.rate_limiter_service.cache_service.increment", new_callable=AsyncMock) as mock_increment:
        # Mock exceeding login rate limit
        mock_increment.return_value = 11

        response = await async_client.post(
            "/api/v1/admin/login",
            json={"username": "admin", "password": "anypassword"}
        )

        assert response.status_code == 429
        data = response.json()
        assert "error" in data
        assert "Too many requests" in data["error"]["message"]
        # Verify key used was rl:login prefix
        assert mock_increment.call_args[0][0].startswith("rl:login:")
