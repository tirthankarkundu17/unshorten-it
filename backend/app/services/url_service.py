import os
import time
import httpx
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
    async with httpx.AsyncClient(timeout=timeout, follow_redirects=True) as client:
        # We use stream("GET") so that we don't download the body of the final URL
        # if it happens to be a large file, but we still trigger all redirects
        # that might require a GET request.
        async with client.stream("GET", url) as response:
            # response.history contains the intermediate responses
            for resp in response.history:
                redirect_chain.append(str(resp.url))
            
            final_url = str(response.url)
    return redirect_chain, final_url

async def unshorten_url(url: str) -> Dict[str, Any]:
    start_time = time.perf_counter()
    
    try:
        redirect_chain, final_url = await fetch_url_redirects(url, REQUEST_TIMEOUT)
    except httpx.RequestError as exc:
        end_time = time.perf_counter()
        return {
            "error": f"Request failed after retries: {str(exc)}"
        }

    end_time = time.perf_counter()

    return {
        "original_url": url,
        "final_url": final_url,
        "redirect_chain": redirect_chain,
        "response_time_ms": round((end_time - start_time) * 1000, 2)
    }
