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
async def test_admin_analytics_endpoint(async_client: AsyncClient):
    """
    Test GET /api/v1/admin/analytics/dashboard endpoint returns 200
    and strictly validates against AdminDashboardResponse schema.
    """
    response = await async_client.get("/api/v1/admin/analytics/dashboard")
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
async def test_admin_visitors_endpoint(async_client: AsyncClient):
    """
    Test GET /api/v1/admin/analytics/visitors returns 200
    and strictly validates against VisitorListResponse schema.
    """
    response = await async_client.get("/api/v1/admin/analytics/visitors")
    assert response.status_code == 200

    data = response.json()
    assert "visitors" in data
    assert "total_count" in data
    assert isinstance(data["visitors"], list)
    assert isinstance(data["total_count"], int)

@pytest.mark.asyncio
async def test_admin_visitor_requests_endpoint(async_client: AsyncClient):
    """
    Test GET /api/v1/admin/analytics/visitors/{ip}/requests returns 200
    and strictly validates against VisitorRequestsResponse schema.
    """
    test_ip = "127.0.0.1"
    response = await async_client.get(f"/api/v1/admin/analytics/visitors/{test_ip}/requests")
    assert response.status_code == 200

    data = response.json()
    assert data["ip"] == test_ip
    assert "total_requests" in data
    assert "requests" in data
    assert isinstance(data["requests"], list)
    assert isinstance(data["total_requests"], int)

