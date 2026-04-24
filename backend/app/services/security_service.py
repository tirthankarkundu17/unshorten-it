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
URLHAUS_RECENT_CSV_URL = "https://urlhaus.abuse.ch/downloads/csv_recent/"
SYNC_INTERVAL_SECONDS = 3600 * 1 # Sync once a day, or every hour

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

    backfill = os.getenv("URLHAUS_BACKFILL", "false").lower() == "true"
    url = URLHAUS_ZIP_URL if backfill else URLHAUS_RECENT_CSV_URL

    logger.info(f"Starting URLhaus {'full backfill' if backfill else 'recent'} threat feed download from {url}...")

    async with httpx.AsyncClient(timeout=60.0, follow_redirects=True) as client:
        try:
            response = await client.get(url)
            response.raise_for_status()

            if backfill:
                # Read zipped content
                zip_data = io.BytesIO(response.content)
                with zipfile.ZipFile(zip_data, 'r') as zf:
                    # The zip usually contains a single file, usually csv.txt
                    if not zf.namelist():
                         logger.error("Zip file is empty.")
                         return
                    csv_filename = zf.namelist()[0]
                    with zf.open(csv_filename) as f:
                        content = f.read().decode('utf-8')
            else:
                # Recent feed is plain CSV
                content = response.text

            lines = content.splitlines()
            # Filter out comments and empty lines
            valid_lines = [line for line in lines if not line.startswith('#') and line.strip()]

            reader = csv.reader(valid_lines)

            # CSV Format: id,dateadded,url,url_status,last_online,threat,tags,urlhaus_link,reporter
            
            bulk_ops = []
            row_count = 0
            for row in reader:
                try:
                    if len(row) < 9:
                        logger.warning(f"Skipping malformed row: {row}")
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
                    row_count += 1

                    # Execute in batches of 5000 to save memory
                    if len(bulk_ops) >= 5000:
                        await collection.bulk_write(bulk_ops, ordered=False)
                        bulk_ops.clear()

                except Exception as e:
                    logger.error(f"Error processing row {row_count + 1}: {e}. Row skipped.")

            if bulk_ops:
                await collection.bulk_write(bulk_ops, ordered=False)

            logger.info(f"URLhaus sync completed successfully. Processed {row_count} records.")

        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP Error during URLhaus sync: {e}")
        except Exception as e:
            logger.error(f"Failed to sync URLhaus feed due to an unexpected error: {e}")


async def urlhaus_sync_loop():
    """Infinite loop for the sync task"""
    while True:
        try:
            await sync_urlhaus_feed()
        except Exception as e:
            logger.error(f"Error in URLhaus sync loop: {e}")

        await asyncio.sleep(SYNC_INTERVAL_SECONDS)

from urllib.parse import urlparse, urlunparse

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

    # Check the full URL provided.
    urls_to_check = [url]

    # Check the base URL (scheme://netloc/path) by stripping query params and fragments.
    # This handles cases like example.com/?q=1 or example.com/path#anchor
    parsed = urlparse(url)
    base_url = urlunparse((parsed.scheme, parsed.netloc, parsed.path, parsed.params, '', ''))
    
    # Only add base_url if it's different from the original URL AND it's not empty/just a fragment
    if base_url and base_url != url:
        urls_to_check.append(base_url)

    # Deduplicate the list if the original URL already matches the base_url (shouldn't happen with the above logic, but safe to keep)
    urls_to_check = list(dict.fromkeys(urls_to_check))

    threat = await collection.find_one({"url": {"$in": urls_to_check}})

    if threat:
        result = {
            "is_safe": False, 
            "threat_type": threat.get("threat_type", "MALWARE")
        }
    else:
        result = {"is_safe": True, "threat_type": None}

    # Cache the result for 1 hour
    await cache_service.set_json(cache_key, result, expire=3600)
    return result