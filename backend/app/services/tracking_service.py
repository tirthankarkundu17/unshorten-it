import logging
from datetime import datetime, timezone
import geocoder
import asyncio
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
                
                # Fetch geolocation data for the IP address asynchronously
                location_data = None
                if ip_address and ip_address not in ("127.0.0.1", "::1", "localhost", "unknown"):
                    try:
                        g = await asyncio.to_thread(geocoder.ip, ip_address)
                        if g.ok:
                            location_data = {
                                "city": g.city,
                                "state": g.state,
                                "country": g.country,
                                "lat": g.latlng[0] if g.latlng and len(g.latlng) == 2 else None,
                                "lng": g.latlng[1] if g.latlng and len(g.latlng) == 2 else None,
                            }
                    except Exception as geo_err:
                        logger.warning(f"Geocoding failed for {ip_address}: {geo_err}")

                update_fields = {"last_seen": datetime.now(timezone.utc)}
                if location_data:
                    update_fields["location"] = location_data

                await db_service.db.visitors.update_one(
                    {"_id": ip_address},
                    {
                        "$setOnInsert": {"first_seen": datetime.now(timezone.utc)},
                        "$set": update_fields,
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
