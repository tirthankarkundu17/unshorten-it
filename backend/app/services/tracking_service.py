import logging
from datetime import datetime, timezone
from .database_service import db_service

logger = logging.getLogger(__name__)

class TrackingService:
    @staticmethod
    async def track_request(ip_address: str, platform: str):
        """
        Track a unique IP address and increment platform request count in MongoDB.
        """
        try:
            # Normalize platform name
            platform = platform.lower() if platform else "unknown"
            
            if db_service.db is not None:
                # 1. Log each request (for flexibility, e.g., daily unique IPs)
                request_log = {
                    "ip": ip_address,
                    "platform": platform,
                    "timestamp": datetime.now(timezone.utc)
                }
                await db_service.db.requests.insert_one(request_log)
                
                # 2. Track global unique IPs (as an id in a dedicated collection)
                # This ensures we have a quick way to count unique visitors.
                await db_service.db.visitors.update_one(
                    {"_id": ip_address},
                    {
                        "$setOnInsert": {"first_seen": datetime.now(timezone.utc)},
                        "$set": {"last_seen": datetime.now(timezone.utc)},
                        "$addToSet": {"platforms": platform} # Track platforms this IP uses
                    },
                    upsert=True
                )
                
                # 3. Track platform counts in a simple summary document
                await db_service.db.stats.update_one(
                    {"_id": "global_counts"},
                    {
                        "$inc": {
                            f"platform_counts.{platform}": 1,
                            "total_requests": 1
                        }
                    },
                    upsert=True
                )
                
                logger.debug(f"Tracked request from {ip_address} on {platform} (MongoDB)")
            else:
                logger.warning("MongoDB is not initialized. Skipping tracking.")
                
        except Exception as e:
            logger.error(f"Failed to track request in MongoDB: {e}")

tracking_service = TrackingService()
