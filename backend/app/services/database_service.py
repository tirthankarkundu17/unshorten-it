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
            logger.info(f"Initialized MongoDB at {mongo_uri}, database: {mongo_db_name}")
        except Exception as e:
            logger.error(f"Failed to initialize MongoDB: {e}")

    async def close(self):
        if self.client:
            self.client.close()
            logger.info("MongoDB connection closed.")

db_service = DatabaseService()
