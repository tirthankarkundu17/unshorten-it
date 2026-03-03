import os
import time
import httpx
import re
from typing import Dict, Any, List
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

# Extract timeout to be injected via env variables
REQUEST_TIMEOUT = float(os.getenv("REQUEST_TIMEOUT", "15.0"))

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    retry=retry_if_exception_type(httpx.RequestError),
    reraise=True
)
async def fetch_url_redirects(url: str, timeout: float) -> tuple[List[str], str]:
    redirect_chain: List[str] = []
    final_url = url
    
    async with httpx.AsyncClient(timeout=timeout, follow_redirects=True) as client:
        # Allow up to 5 client-side redirects (e.g. meta refresh or interstitials like LinkedIn)
        for _ in range(5):
            # We use stream("GET") so that we don't download the body of the final URL
            # if it happens to be a large file, but we still trigger all redirects
            # that might require a GET request.
            async with client.stream("GET", final_url) as response:
                # response.history contains the intermediate responses
                for resp in response.history:
                    redirect_chain.append(str(resp.url))
                
                current_url = str(response.url)
                
                # Check for client-side redirects or interstitials if it's text/html
                content_type = response.headers.get("content-type", "")
                if "text/html" in content_type:
                    text_bytes = b""
                    async for chunk in response.aiter_bytes(chunk_size=4096):
                        text_bytes += chunk
                        if len(text_bytes) > 50 * 1024:  # Read at most 50 KB
                            break
                    
                    text_str = text_bytes.decode('utf-8', errors='ignore')
                    
                    # 1. LinkedIn Interstitial Redirect
                    match = re.search(r'data-tracking-control-name="external_url_click"[^>]*href="([^"]+)"', text_str)
                    if match:
                        redirect_chain.append(current_url)
                        final_url = match.group(1).replace("&amp;", "&")
                        continue
                    
                    # 2. Meta Refresh Redirect
                    meta_refresh = re.search(r'<meta[^>]+http-equiv=["\']refresh["\'][^>]+content=["\']\d+;\s*url=["\']?([^"\'>]+)["\']?', text_str, re.IGNORECASE)
                    if meta_refresh:
                        redirect_chain.append(current_url)
                        final_url = meta_refresh.group(1).replace("&amp;", "&")
                        continue
                
                final_url = current_url
                break

    return redirect_chain, final_url

from .cache_service import cache_service

async def unshorten_url(url: str) -> Dict[str, Any]:
    start_time = time.perf_counter()
    
    # Check cache first
    cached_result = await cache_service.get_cached_url(url)
    if cached_result:
        end_time = time.perf_counter()
        return {
            "original_url": url,
            "final_url": cached_result["final_url"],
            "redirect_chain": cached_result["redirect_chain"],
            "response_time_ms": round((end_time - start_time) * 1000, 2),
            "cached": True
        }
    
    try:
        redirect_chain, final_url = await fetch_url_redirects(url, REQUEST_TIMEOUT)
    except httpx.RequestError as exc:
        end_time = time.perf_counter()
        return {
            "error": f"Request failed after retries: {str(exc)}"
        }

    end_time = time.perf_counter()
    
    result = {
        "original_url": url,
        "final_url": final_url,
        "redirect_chain": redirect_chain,
        "response_time_ms": round((end_time - start_time) * 1000, 2),
        "cached": False
    }

    # Save to cache asynchronously without blocking the return
    await cache_service.set_cached_url(url, {
        "final_url": final_url,
        "redirect_chain": redirect_chain
    })

    return result
