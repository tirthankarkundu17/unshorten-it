import os
import json
import logging
import diskcache
import redis.asyncio as redis
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
if not logger.handlers:
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(logging.Formatter('%(levelname)s:\t  %(message)s'))
    logger.addHandler(console_handler)

class CacheService:
    def __init__(self):
        self.redis_client: Optional[redis.Redis] = None
        self.disk_cache: Optional[diskcache.Cache] = None
        self.default_cache_expire: int = 24 * 60 * 60
        # Initialization is deferred

    def initialize(self):
        try:
            self.default_cache_expire = int(os.getenv("CACHE_EXPIRE_SECONDS", str(24 * 60 * 60)))
        except ValueError:
            self.default_cache_expire = 24 * 60 * 60
            
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
        cache_dir = os.getenv("DISKCACHE_DIR", "/tmp/cacheservice")
        
        # Load max cache size in MB (default 500 MB)
        try:
            size_limit_mb = int(os.getenv("DISKCACHE_SIZE_LIMIT_MB", "500"))
        except ValueError:
            size_limit_mb = 500
            
        size_limit_bytes = size_limit_mb * 1024 * 1024
        
        try:
            os.makedirs(cache_dir, exist_ok=True)
            # diskcache is thread-safe and process-safe (uses SQLite)
            self.disk_cache = diskcache.Cache(cache_dir, size_limit=size_limit_bytes)
            logger.info(f"Initialized DiskCache at {cache_dir} with max size {size_limit_mb}MB")
        except Exception as e:
            logger.error(f"Failed to initialize DiskCache: {e}")

    async def get_cached_url(self, url: str) -> Optional[Dict[str, Any]]:
        try:
            if self.redis_client is not None:
                data = await self.redis_client.get(f"url:{url}")
                if data:
                    logger.info(f"Cache HIT (Redis) for: {url}")
                    return json.loads(data)
            elif self.disk_cache is not None:
                # diskcache is synchronous, but fast enough for this purpose
                data = self.disk_cache.get(f"url:{url}")
                if data:
                    logger.info(f"Cache HIT (DiskCache) for: {url}")
                    return json.loads(data) if isinstance(data, str) else data
            
            logger.info(f"Cache MISS for: {url}")
        except Exception as e:
            logger.error(f"Error reading from cache: {e}")
        
        return None

    async def set_cached_url(self, url: str, data: Dict[str, Any], expire: Optional[int] = None):
        if expire is None:
            expire = self.default_cache_expire
        try:
            json_data = json.dumps(data)
            if self.redis_client is not None:
                await self.redis_client.setex(f"url:{url}", expire, json_data)
                logger.info(f"Saved to cache (Redis): {url}")
            elif self.disk_cache is not None:
                self.disk_cache.set(f"url:{url}", json_data, expire=expire)
                logger.info(f"Saved to cache (DiskCache): {url}")
        except Exception as e:
            logger.error(f"Error writing to cache: {e}")
            
    async def close(self):
        if self.redis_client:
            await self.redis_client.aclose()
        if self.disk_cache:
            self.disk_cache.close()

# Global cache instance
cache_service = CacheService()
