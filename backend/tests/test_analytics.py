import pytest
from httpx import AsyncClient
from app.services.analytics_service import analytics_service
from app.services.database_service import db_service
from app.schemas import AdminDashboardResponse

@pytest.mark.asyncio
async def test_analytics_service_fallback():
    """
    Test that AnalyticsService returns a valid AdminDashboardResponse
    even when db is None or uninitialized.
    """
    original_db = db_service.db
    try:
        db_service.db = None
        result = await analytics_service.get_admin_dashboard_metrics()
        assert isinstance(result, AdminDashboardResponse)
        assert result.total_requests >= 0
        assert result.total_unique_visitors >= 0
        assert isinstance(result.top_locations, list)
        assert isinstance(result.platforms, list)
        assert isinstance(result.traffic_history, list)
        assert isinstance(result.recent_logs, list)
    finally:
        db_service.db = original_db

@pytest.mark.asyncio
async def test_admin_auth_endpoints(async_client: AsyncClient):
    """
    Test admin login failure and success, and token verification on /api/v1/admin/me.
    """
    # 1. Bad credentials
    bad_login = await async_client.post(
        "/api/v1/admin/login",
        json={"username": "admin", "password": "wrongpassword"}
    )
    assert bad_login.status_code == 401
    assert "error" in bad_login.json()

    # 2. Good credentials
    good_login = await async_client.post(
        "/api/v1/admin/login",
        json={"username": "admin", "password": "adminpassword123"}
    )
    assert good_login.status_code == 200
    login_data = good_login.json()
    assert "access_token" in login_data
    token = login_data["access_token"]
    assert token

    # 3. Verify on /api/v1/admin/me
    headers = {"Authorization": f"Bearer {token}"}
    me_resp = await async_client.get("/api/v1/admin/me", headers=headers)
    assert me_resp.status_code == 200
    assert me_resp.json()["authenticated"] is True
    assert me_resp.json()["username"] == "admin"

@pytest.mark.asyncio
async def test_admin_unauthorized_access(async_client: AsyncClient):
    """
    Test that admin endpoints strictly reject requests without a valid token (401).
    """
    endpoints = [
        "/api/v1/admin/analytics/dashboard",
        "/api/v1/admin/analytics/visitors",
        "/api/v1/admin/analytics/visitors/127.0.0.1/requests",
    ]
    for endpoint in endpoints:
        resp = await async_client.get(endpoint)
        assert resp.status_code == 401
        assert "error" in resp.json()

@pytest.mark.asyncio
async def test_admin_analytics_endpoint_authenticated(async_client: AsyncClient):
    """
    Test GET /api/v1/admin/analytics/dashboard endpoint returns 200
    when authenticated with Bearer token.
    """
    login_resp = await async_client.post(
        "/api/v1/admin/login",
        json={"username": "admin", "password": "adminpassword123"}
    )
    token = login_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    response = await async_client.get("/api/v1/admin/analytics/dashboard", headers=headers)
    assert response.status_code == 200
    
    data = response.json()
    assert "total_requests" in data
    assert "total_unique_visitors" in data
    assert "top_locations" in data
    assert "platforms" in data
    assert "traffic_history" in data
    assert "recent_logs" in data
    assert isinstance(data["total_requests"], int)
    assert isinstance(data["total_unique_visitors"], int)
    assert isinstance(data["top_locations"], list)
    assert isinstance(data["platforms"], list)
    assert isinstance(data["traffic_history"], list)
    assert isinstance(data["recent_logs"], list)

@pytest.mark.asyncio
async def test_admin_visitors_endpoint_authenticated(async_client: AsyncClient):
    """
    Test GET /api/v1/admin/analytics/visitors returns 200
    when authenticated with Bearer token.
    """
    login_resp = await async_client.post(
        "/api/v1/admin/login",
        json={"username": "admin", "password": "adminpassword123"}
    )
    token = login_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    response = await async_client.get("/api/v1/admin/analytics/visitors", headers=headers)
    assert response.status_code == 200

    data = response.json()
    assert "visitors" in data
    assert "total_count" in data
    assert isinstance(data["visitors"], list)
    assert isinstance(data["total_count"], int)

@pytest.mark.asyncio
async def test_admin_visitor_requests_endpoint_authenticated(async_client: AsyncClient):
    """
    Test GET /api/v1/admin/analytics/visitors/{ip}/requests returns 200
    when authenticated with Bearer token.
    """
    login_resp = await async_client.post(
        "/api/v1/admin/login",
        json={"username": "admin", "password": "adminpassword123"}
    )
    token = login_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    test_ip = "127.0.0.1"
    response = await async_client.get(
        f"/api/v1/admin/analytics/visitors/{test_ip}/requests",
        headers=headers
    )
    assert response.status_code == 200

    data = response.json()
    assert data["ip"] == test_ip
    assert "total_requests" in data
    assert "requests" in data
    assert isinstance(data["requests"], list)
    assert isinstance(data["total_requests"], int)

def test_auth_service_fails_when_env_vars_missing(monkeypatch):
    """
    Test that AuthService fails immediately with RuntimeError
    if any required env var is missing and has no defaults.
    """
    from app.services.auth_service import AuthService

    # Test missing username
    with monkeypatch.context() as m:
        m.delenv("ADMIN_USERNAME", raising=False)
        with pytest.raises(RuntimeError, match="Missing required environment variable"):
            AuthService()

    # Test missing password
    with monkeypatch.context() as m:
        m.delenv("ADMIN_PASSWORD", raising=False)
        with pytest.raises(RuntimeError, match="Missing required environment variable"):
            AuthService()

    # Test missing secret key
    with monkeypatch.context() as m:
        m.delenv("ADMIN_SECRET_KEY", raising=False)
        with pytest.raises(RuntimeError, match="Missing required environment variable"):
            AuthService()

