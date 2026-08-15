import pytest
import pytest_asyncio
import datetime
from httpx import AsyncClient
from app.services.database_service import db_service

@pytest_asyncio.fixture(autouse=True)
async def clean_database():
    """
    Clean up users and history collections before each test.
    """
    db_service.initialize()
    if db_service.db is not None:
        await db_service.db.users.delete_many({})
        await db_service.db.history.delete_many({})
    yield
    if db_service.db is not None:
        await db_service.db.users.delete_many({})
        await db_service.db.history.delete_many({})

@pytest.mark.asyncio
async def test_register_success(async_client: AsyncClient):
    response = await async_client.post(
        "/api/v1/auth/register",
        json={"username": "testuser", "password": "password123"}
    )
    assert response.status_code == 201
    data = response.json()
    assert data["username"] == "testuser"
    assert "id" in data

@pytest.mark.asyncio
async def test_register_duplicate_username(async_client: AsyncClient):
    # Register first user
    response = await async_client.post(
        "/api/v1/auth/register",
        json={"username": "testuser", "password": "password123"}
    )
    assert response.status_code == 201
    
    # Register second user with same username
    response = await async_client.post(
        "/api/v1/auth/register",
        json={"username": "testuser", "password": "differentpassword"}
    )
    assert response.status_code == 400
    data = response.json()
    assert data["error"]["code"] == "USER_ALREADY_EXISTS"

@pytest.mark.asyncio
async def test_login_success(async_client: AsyncClient):
    # Register user
    await async_client.post(
        "/api/v1/auth/register",
        json={"username": "testuser", "password": "password123"}
    )
    
    # Login
    response = await async_client.post(
        "/api/v1/auth/login",
        json={"username": "testuser", "password": "password123"}
    )
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"

@pytest.mark.asyncio
async def test_login_invalid_password(async_client: AsyncClient):
    # Register user
    await async_client.post(
        "/api/v1/auth/register",
        json={"username": "testuser", "password": "password123"}
    )
    
    # Login with wrong password
    response = await async_client.post(
        "/api/v1/auth/login",
        json={"username": "testuser", "password": "wrongpassword"}
    )
    assert response.status_code == 401
    data = response.json()
    assert data["error"]["code"] == "INVALID_CREDENTIALS"

@pytest.mark.asyncio
async def test_get_me(async_client: AsyncClient):
    # Register user
    await async_client.post(
        "/api/v1/auth/register",
        json={"username": "testuser", "password": "password123"}
    )
    
    # Login
    login_response = await async_client.post(
        "/api/v1/auth/login",
        json={"username": "testuser", "password": "password123"}
    )
    token = login_response.json()["access_token"]
    
    # Get profile
    headers = {"Authorization": f"Bearer {token}"}
    response = await async_client.get("/api/v1/auth/me", headers=headers)
    assert response.status_code == 200
    data = response.json()
    assert data["username"] == "testuser"

@pytest.mark.asyncio
async def test_history_sync(async_client: AsyncClient):
    # Register user
    await async_client.post(
        "/api/v1/auth/register",
        json={"username": "testuser", "password": "password123"}
    )
    
    # Login
    login_response = await async_client.post(
        "/api/v1/auth/login",
        json={"username": "testuser", "password": "password123"}
    )
    token = login_response.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}
    
    # Check history is empty initially
    history_response = await async_client.get("/api/v1/history", headers=headers)
    assert history_response.status_code == 200
    assert len(history_response.json()) == 0
    
    # Insert a dummy history document
    user = await db_service.db.users.find_one({"username": "testuser"})
    dummy_item = {
        "user_id": str(user["_id"]),
        "original_url": "https://bit.ly/test",
        "final_url": "https://example.com/target",
        "cleaned_url": "https://example.com/target",
        "redirect_chain": ["https://bit.ly/test", "https://example.com/target"],
        "response_time_ms": 150.0,
        "timestamp": datetime.datetime.now(datetime.timezone.utc)
    }
    await db_service.db.history.insert_one(dummy_item)
    
    # Fetch history again
    history_response = await async_client.get("/api/v1/history", headers=headers)
    assert history_response.status_code == 200
    history = history_response.json()
    assert len(history) == 1
    assert history[0]["original_url"] == "https://bit.ly/test"
    assert history[0]["final_url"] == "https://example.com/target"
    
    # Clear history
    clear_response = await async_client.post("/api/v1/history/clear", headers=headers)
    assert clear_response.status_code == 200
    assert clear_response.json()["status"] == "ok"
    
    # Verify cleared
    history_response = await async_client.get("/api/v1/history", headers=headers)
    assert len(history_response.json()) == 0
