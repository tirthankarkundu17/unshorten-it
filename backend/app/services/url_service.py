import os
import time
import httpx
import re
from bs4 import BeautifulSoup
from typing import Dict, Any, List, Optional, Tuple
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

# Extract timeout to be injected via env variables
REQUEST_TIMEOUT = float(os.getenv("REQUEST_TIMEOUT", "15.0"))

# Common browser headers to avoid being flagged as a bot/crawler
DEFAULT_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
    "Accept-Language": "en-US,en;q=0.9",
    "Cache-Control": "no-cache",
    "Pragma": "no-cache",
}

# Keywords that indicate we've hit a bot-detection page or error page instead of real content
INVALID_PREVIEW_KEYWORDS = [
    "just a moment", 
    "cloudflare", 
    "attention required", 
    "checking your browser", 
    "access denied", 
    "403 forbidden",
    "ddos protection", 
    "verify you are human",
    "one more step"
]

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    retry=retry_if_exception_type(httpx.RequestError),
    reraise=True
)
async def fetch_url_redirects(url: str, timeout: float) -> Tuple[List[str], str, Optional[Dict[str, Any]]]:
    redirect_chain: List[str] = []
    final_url = url
    page_preview: Optional[Dict[str, Any]] = None
    
    async with httpx.AsyncClient(timeout=timeout, follow_redirects=True, headers=DEFAULT_HEADERS) as client:
        # Allow up to 5 client-side redirects (e.g. meta refresh or interstitials like LinkedIn)
        for _ in range(5):
            page_preview = None
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

                    # Only parse metadata if we have a successful 200 OK response
                    # This avoids showing previews of 404/500/403 pages
                    if response.status_code == 200:
                        soup = BeautifulSoup(text_str, "html.parser")
                        extracted_title = soup.title.string.strip() if (soup.title and soup.title.string) else ""
                        
                        # Validate that the title doesn't look like a bot-blocker
                        is_invalid = any(kw in extracted_title.lower() for kw in INVALID_PREVIEW_KEYWORDS)
                        
                        if extracted_title and not is_invalid:
                            page_preview = {"title": extracted_title}
                            
                            desc_meta = soup.find("meta", attrs={"name": "description"}) or soup.find("meta", attrs={"property": "og:description"})
                            if desc_meta and desc_meta.get("content"):
                                page_preview["description"] = desc_meta["content"].strip()
                                
                            img_meta = soup.find("meta", attrs={"property": "og:image"}) or soup.find("meta", attrs={"itemprop": "image"})
                            if img_meta and img_meta.get("content"):
                                img_url = img_meta["content"].strip()
                                if img_url.startswith('http'):
                                    page_preview["image_url"] = img_url
                                elif img_url.startswith('//'):
                                    page_preview["image_url"] = f"https:{img_url}"
                            
                            if not page_preview.get("title") and not page_preview.get("description"):
                                page_preview = None
                        else:
                            page_preview = None
                    else:
                        page_preview = None
                
                final_url = current_url
                break

    return redirect_chain, final_url, page_preview

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
            "cached": True,
            "preview": cached_result.get("preview")
        }
    
    try:
        redirect_chain, final_url, preview = await fetch_url_redirects(url, REQUEST_TIMEOUT)
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
        "cached": False,
        "preview": preview
    }

    # Save to cache asynchronously without blocking the return
    await cache_service.set_cached_url(url, {
        "final_url": final_url,
        "redirect_chain": redirect_chain,
        "preview": preview
    })

    return result
