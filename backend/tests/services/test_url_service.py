import pytest
import respx
import httpx
from app.services.url_service import fetch_url_redirects

@pytest.mark.asyncio
@respx.mock
async def test_standard_http_redirect():
    url = "https://short.com/standard"
    final_url = "https://target.com/page"
    
    respx.get(url).mock(return_value=httpx.Response(301, headers={"Location": final_url}))
    respx.get(final_url).mock(return_value=httpx.Response(200, text="Final Content"))
    
    chain, resulting_url = await fetch_url_redirects(url, timeout=5.0)
    
    assert resulting_url == final_url
    assert chain == [url]

@pytest.mark.asyncio
@respx.mock
async def test_linkedin_interstitial_redirect():
    url = "https://lnkd.in/test"
    final_url = "https://www.real-target.com/article"
    
    # Mock LinkedIn interstitial page
    html_content = f"""
    <html>
      <body>
        <a data-tracking-control-name="external_url_click" href="{final_url}">Target</a>
      </body>
    </html>
    """
    
    respx.get(url).mock(return_value=httpx.Response(200, headers={"Content-Type": "text/html"}, text=html_content))
    respx.get(final_url).mock(return_value=httpx.Response(200, text="Final Content"))
    
    chain, resulting_url = await fetch_url_redirects(url, timeout=5.0)
    
    assert resulting_url == final_url
    assert chain == [url]

@pytest.mark.asyncio
@respx.mock
async def test_meta_refresh_redirect():
    url = "https://short.com/meta"
    final_url = "https://www.target.com/meta-page"
    
    # Mock Meta Refresh page
    html_content = f"""
    <html>
      <head>
        <meta http-equiv="refresh" content="0; url={final_url}" />
      </head>
      <body>
        Redirecting...
      </body>
    </html>
    """
    
    respx.get(url).mock(return_value=httpx.Response(200, headers={"Content-Type": "text/html"}, text=html_content))
    respx.get(final_url).mock(return_value=httpx.Response(200, text="Final Content"))
    
    chain, resulting_url = await fetch_url_redirects(url, timeout=5.0)
    
    assert resulting_url == final_url
    assert chain == [url]

@pytest.mark.asyncio
@respx.mock
async def test_max_client_side_redirects():
    # Should stop after 5 client-side hops
    url_base = "https://short.com/loop"
    
    for i in range(6):
        current_url = f"{url_base}{i}" if i > 0 else url_base
        next_url = f"{url_base}{i+1}"
        html_content = f"""<meta http-equiv="refresh" content="0; url={next_url}" />"""
        
        # We need to register responses for up to 6 hops
        respx.get(current_url).mock(return_value=httpx.Response(200, headers={"Content-Type": "text/html"}, text=html_content))
        
    chain, resulting_url = await fetch_url_redirects(url_base, timeout=5.0)
    
    # 5 iterations x client side checks, means it follows 5 times.
    # So the resulting_url shouldn't reach loop6. It should be loop5.
    assert "loop5" in resulting_url
    assert len(chain) == 5

@pytest.mark.asyncio
@respx.mock
async def test_large_html_body_truncation():
    url = "https://short.com/large"
    
    # Simulate a very large HTML page that doesn't have a redirect
    # The code breaks after 50KB to preserve bandwidth.
    large_html = "A" * (60 * 1024) # 60KB
    respx.get(url).mock(return_value=httpx.Response(200, headers={"Content-Type": "text/html"}, text=large_html))
    
    chain, resulting_url = await fetch_url_redirects(url, timeout=5.0)
    
    assert resulting_url == url
    assert chain == []
