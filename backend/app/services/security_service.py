import os
import httpx
import logging
import zipfile
import io
import csv
import asyncio
from typing import Dict, Any

from .database_service import db_service
from motor.motor_asyncio import AsyncIOMotorCollection
from pymongo import UpdateOne, IndexModel, ASCENDING

from .cache_service import cache_service

logger = logging.getLogger(__name__)

URLHAUS_ZIP_URL = "https://urlhaus.abuse.ch/downloads/csv/"
SYNC_INTERVAL_SECONDS = 3600 * 24 # Sync once a day, or every hour

async def sync_urlhaus_feed():
    """Background task to download and sync URLhaus CSV zip to MongoDB"""
    if db_service.db is None:
        logger.warning("DB not initialized, skipping URLhaus sync")
        return
        
    collection: AsyncIOMotorCollection = db_service.db["urlhaus_threats"]
    
    # Ensure URL index exists for fast lookups
    await collection.create_indexes([
        IndexModel([("url", ASCENDING)], unique=True)
    ])
    
    logger.info("Starting URLhaus threat feed download...")
    
    async with httpx.AsyncClient(timeout=60.0, follow_redirects=True) as client:
        try:
            response = await client.get(URLHAUS_ZIP_URL)
            response.raise_for_status()
            
            # Read zipped content
            zip_data = io.BytesIO(response.content)
            with zipfile.ZipFile(zip_data, 'r') as zf:
                # The zip usually contains a single file, usually csv.txt
                csv_filename = zf.namelist()[0]
                with zf.open(csv_filename) as f:
                    content = f.read().decode('utf-8')
                    
            lines = content.splitlines()
            valid_lines = [line for line in lines if not line.startswith('#') and line.strip()]
            
            reader = csv.reader(valid_lines)
            
            # CSV Format: id,dateadded,url,url_status,last_online,threat,tags,urlhaus_link,reporter
            
            bulk_ops = []
            for row in reader:
                if len(row) < 9:
                    continue
                    
                threat_id = row[0]
                dateadded = row[1]
                threat_url = row[2]
                url_status = row[3]
                last_online = row[4]
                threat = row[5]
                tags = row[6]
                urlhaus_link = row[7]
                reporter = row[8]
                
                # Only keeping active/online threats could save space, 
                # but we'll store all for better coverage and just update them
                op = UpdateOne(
                    {"url": threat_url},
                    {"$set": {
                        "urlhaus_id": threat_id,
                        "dateadded": dateadded,
                        "url": threat_url,
                        "status": url_status,
                        "last_online": last_online,
                        "threat_type": threat,
                        "tags": tags,
                        "urlhaus_link": urlhaus_link,
                        "reporter": reporter
                    }},
                    upsert=True
                )
                bulk_ops.append(op)
                
                # Execute in batches of 5000 to save memory
                if len(bulk_ops) >= 5000:
                    await collection.bulk_write(bulk_ops, ordered=False)
                    bulk_ops.clear()
                    
            if bulk_ops:
                await collection.bulk_write(bulk_ops, ordered=False)
                
            logger.info("URLhaus sync completed successfully.")
            
        except Exception as e:
            logger.error(f"Failed to sync URLhaus feed: {e}")

async def urlhaus_sync_loop():
    """Infinite loop for the sync task"""
    while True:
        try:
            await sync_urlhaus_feed()
        except Exception as e:
            logger.error(f"Error in URLhaus sync loop: {e}")
            
        await asyncio.sleep(SYNC_INTERVAL_SECONDS)

async def check_url_security(url: str) -> Dict[str, Any]:
    """
    Checks if the URL is flagged as malicious by checking the local MongoDB caching table.
    """
    cache_key = f"threat:{url}"
    cached_result = await cache_service.get_json(cache_key)
    if cached_result is not None:
        return cached_result

    if db_service.db is None:
        result = {"is_safe": True, "threat_type": None}
        await cache_service.set_json(cache_key, result, expire=3600)
        return result
        
    collection: AsyncIOMotorCollection = db_service.db["urlhaus_threats"]
    
    # Exact match check
    threat = await collection.find_one({"url": url})
    
    if threat:
        result = {
            "is_safe": False, 
            "threat_type": threat.get("threat_type", "MALWARE")
        }
    else:
        # Note: We can expand this later to check domain-level matches if needed
        result = {"is_safe": True, "threat_type": None}

    # Cache the result for 1 hour to heavily reduce MongoDB loads on repetitive urls
    await cache_service.set_json(cache_key, result, expire=3600)
    return result
