import pytest
from unittest.mock import AsyncMock, patch
from app.services.security_service import check_url_security

@pytest.mark.asyncio
async def test_check_url_security_cached():
    url = "https://github.com/cached-malware"
    cached_response = {"is_safe": False, "threat_type": "cached_malware"}
    
    with patch("app.services.security_service.cache_service.get_json", new_callable=AsyncMock) as mock_get_json:
        mock_get_json.return_value = cached_response
        
        result = await check_url_security(url)
        
        mock_get_json.assert_called_once_with(f"threat:{url}")
        assert result == cached_response

@pytest.mark.asyncio
async def test_check_url_security_db_not_initialized():
    url = "https://example.com/safe"
    
    with patch("app.services.security_service.cache_service.get_json", new_callable=AsyncMock) as mock_get_json, \
         patch("app.services.security_service.cache_service.set_json", new_callable=AsyncMock) as mock_set_json, \
         patch("app.services.security_service.db_service") as mock_db_service:
         
        mock_get_json.return_value = None
        mock_db_service.db = None
        
        result = await check_url_security(url)
        
        assert result == {"is_safe": True, "threat_type": None}
        mock_set_json.assert_called_once_with(f"threat:{url}", result, expire=3600)

@pytest.mark.asyncio
async def test_check_url_security_exact_match_found():
    url = "http://malware.com/payload.exe"
    db_threat = {"url": url, "threat_type": "malware_download"}
    
    # Create mock for collection.find_one
    mock_collection = AsyncMock()
    mock_collection.find_one.return_value = db_threat
    
    with patch("app.services.security_service.cache_service.get_json", new_callable=AsyncMock) as mock_get_json, \
         patch("app.services.security_service.cache_service.set_json", new_callable=AsyncMock) as mock_set_json, \
         patch("app.services.security_service.db_service") as mock_db_service:
         
        mock_get_json.return_value = None
        mock_db_service.db = {"urlhaus_threats": mock_collection}
        
        result = await check_url_security(url)
        
        # Should check both full url and base url (which in this case are the same)
        mock_collection.find_one.assert_called_once_with({"url": {"$in": [url]}})
        assert result == {"is_safe": False, "threat_type": "malware_download"}
        mock_set_json.assert_called_once_with(f"threat:{url}", result, expire=3600)

@pytest.mark.asyncio
async def test_check_url_security_base_url_match_found_with_query_params():
    url_with_params = "https://github.com/malware_repo/payload.zip?tracking=1&abc=2"
    base_url = "https://github.com/malware_repo/payload.zip"
    db_threat = {"url": base_url, "threat_type": "payload_delivery"}
    
    mock_collection = AsyncMock()
    mock_collection.find_one.return_value = db_threat
    
    with patch("app.services.security_service.cache_service.get_json", new_callable=AsyncMock) as mock_get_json, \
         patch("app.services.security_service.cache_service.set_json", new_callable=AsyncMock) as mock_set_json, \
         patch("app.services.security_service.db_service") as mock_db_service:
         
        mock_get_json.return_value = None
        mock_db_service.db = {"urlhaus_threats": mock_collection}
        
        result = await check_url_security(url_with_params)
        
        # Verify it queries BOTH the parameterized url AND the base url
        mock_collection.find_one.assert_called_once()
        query_args = mock_collection.find_one.call_args[0][0]
        assert "$in" in query_args["url"]
        assert url_with_params in query_args["url"]["$in"]
        assert base_url in query_args["url"]["$in"]
        
        assert result == {"is_safe": False, "threat_type": "payload_delivery"}
        mock_set_json.assert_called_once_with(f"threat:{url_with_params}", result, expire=3600)

@pytest.mark.asyncio
async def test_check_url_security_no_match():
    url = "https://google.com/safe_page?q=test"
    base_url = "https://google.com/safe_page"
    
    mock_collection = AsyncMock()
    mock_collection.find_one.return_value = None  # No threat found
    
    with patch("app.services.security_service.cache_service.get_json", new_callable=AsyncMock) as mock_get_json, \
         patch("app.services.security_service.cache_service.set_json", new_callable=AsyncMock) as mock_set_json, \
         patch("app.services.security_service.db_service") as mock_db_service:
         
        mock_get_json.return_value = None
        mock_db_service.db = {"urlhaus_threats": mock_collection}
        
        result = await check_url_security(url)
        
        mock_collection.find_one.assert_called_once()
        assert result == {"is_safe": True, "threat_type": None}
        mock_set_json.assert_called_once_with(f"threat:{url}", result, expire=3600)
