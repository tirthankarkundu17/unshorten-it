import os
import logging
from motor.motor_asyncio import AsyncIOMotorClient
from typing import Optional

logger = logging.getLogger(__name__)

class DatabaseService:
    def __init__(self):
        self.client: Optional[AsyncIOMotorClient] = None
        self.db = None
        
    def initialize(self):
        mongo_uri = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
        mongo_db_name = os.getenv("MONGODB_DATABASE", "unshorten_it")
        
        try:
            self.client = AsyncIOMotorClient(mongo_uri)
            self.db = self.client[mongo_db_name]
            logger.info(f"Initialized MongoDB database: {mongo_db_name}")
        except Exception as e:
            logger.error(f"Failed to initialize MongoDB: {e}")

    async def create_indexes(self):
        """
        Ensures necessary indexes on requests and visitors collections
        for fast queries, sorting, and aggregations.
        """
        if self.db is None:
            return

        try:
            # 1. Indexes for requests collection
            # Used for recent request sorting: .sort("timestamp", -1)
            await self.db.requests.create_index([("timestamp", -1)])
            # Used for visitor request history: find({"ip": ip}).sort("timestamp", -1)
            await self.db.requests.create_index([("ip", 1), ("timestamp", -1)])

            # 2. Indexes for visitors collection
            # Used for visitor list: .sort("last_seen", -1)
            await self.db.visitors.create_index([("last_seen", -1)])
            # Used for top locations aggregation: {"location.country": {"$exists": True, "$ne": None}}
            await self.db.visitors.create_index([("location.country", 1), ("location.city", 1)])
            logger.info("Successfully verified/created MongoDB indexes.")
        except Exception as e:
            logger.error(f"Failed to create MongoDB indexes: {e}")

    async def close(self):
        if self.client:
            self.client.close()
            logger.info("MongoDB connection closed.")

db_service = DatabaseService()
