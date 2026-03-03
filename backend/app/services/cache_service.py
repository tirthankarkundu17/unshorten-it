import os
import json
import logging
import diskcache
import redis.asyncio as redis
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)

# Cache duration: 24 hours (86400 seconds) by default
try:
    DEFAULT_CACHE_EXPIRE = int(os.getenv("CACHE_EXPIRE_SECONDS", str(24 * 60 * 60)))
except ValueError:
    DEFAULT_CACHE_EXPIRE = 24 * 60 * 60

class CacheService:
    def __init__(self):
        self.redis_client: Optional[redis.Redis] = None
        self.disk_cache: Optional[diskcache.Cache] = None
        self._initialize_cache()

    def _initialize_cache(self):
        redis_url = os.getenv("REDIS_URL")
        
        if redis_url:
            try:
                # Initialize Async Redis connection pool
                self.redis_client = redis.from_url(redis_url, decode_responses=True)
                logger.info(f"Initialized Redis cache at {redis_url}")
            except Exception as e:
                logger.error(f"Failed to initialize Redis cache: {e}")
                self._fallback_to_diskcache()
        else:
            self._fallback_to_diskcache()

    def _fallback_to_diskcache(self):
        # Fallback to local disk cache, accessible across workers
        cache_dir = os.getenv("DISKCACHE_DIR", "/tmp/unshorten_cache")
        try:
            os.makedirs(cache_dir, exist_ok=True)
            # diskcache is thread-safe and process-safe (uses SQLite)
            self.disk_cache = diskcache.Cache(cache_dir)
            logger.info(f"Initialized DiskCache at {cache_dir}")
        except Exception as e:
            logger.error(f"Failed to initialize DiskCache: {e}")

    async def get_cached_url(self, url: str) -> Optional[Dict[str, Any]]:
        try:
            if self.redis_client:
                data = await self.redis_client.get(f"url:{url}")
                if data:
                    return json.loads(data)
            elif self.disk_cache:
                # diskcache is synchronous, but fast enough for this purpose
                data = self.disk_cache.get(f"url:{url}")
                if data:
                    return json.loads(data) if isinstance(data, str) else data
        except Exception as e:
            logger.error(f"Error reading from cache: {e}")
        
        return None

    async def set_cached_url(self, url: str, data: Dict[str, Any], expire: int = DEFAULT_CACHE_EXPIRE):
        try:
            json_data = json.dumps(data)
            if self.redis_client:
                await self.redis_client.setex(f"url:{url}", expire, json_data)
            elif self.disk_cache:
                self.disk_cache.set(f"url:{url}", json_data, expire=expire)
        except Exception as e:
            logger.error(f"Error writing to cache: {e}")
            
    async def close(self):
        if self.redis_client:
            await self.redis_client.aclose()
        if self.disk_cache:
            self.disk_cache.close()

# Global cache instance
cache_service = CacheService()
