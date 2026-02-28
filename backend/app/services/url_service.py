import time
import httpx
from typing import Dict, Any, List

async def unshorten_url(url: str) -> Dict[str, Any]:
    start_time = time.perf_counter()
    redirect_chain: List[str] = []
    
    try:
        # Using AsyncClient with follow_redirects=True
        async with httpx.AsyncClient(timeout=15.0, follow_redirects=True) as client:
            # We use stream("GET") so that we don't download the body of the final URL
            # if it happens to be a large file, but we still trigger all redirects
            # that might require a GET request.
            async with client.stream("GET", url) as response:
                # response.history contains the intermediate responses
                for resp in response.history:
                    redirect_chain.append(str(resp.url))
                
                final_url = str(response.url)
            
    except httpx.RequestError as exc:
        end_time = time.perf_counter()
        return {
            "error": f"Request failed: {str(exc)}"
        }

    end_time = time.perf_counter()

    return {
        "original_url": url,
        "final_url": final_url,
        "redirect_chain": redirect_chain,
        "response_time_ms": round((end_time - start_time) * 1000, 2)
    }
