import logging
from datetime import datetime, timezone, timedelta
from typing import List, Dict, Any, Optional
from .database_service import db_service
from ..schemas import (
    AdminDashboardResponse,
    LocationStat,
    PlatformStat,
    DailyTraffic,
    RecentLog,
    VisitorLocation,
    VisitorItem,
    VisitorListResponse,
    VisitorRequestDetail,
    VisitorRequestsResponse,
)

logger = logging.getLogger(__name__)

class AnalyticsService:
    @staticmethod
    async def get_admin_dashboard_metrics() -> AdminDashboardResponse:
        """
        Aggregates usage metrics, visitor geolocations, traffic history,
        and recent logs from MongoDB collections.
        """
        # Fallback response when MongoDB is not connected or initialized
        fallback_response = AdminDashboardResponse(
            total_requests=0,
            total_unique_visitors=0,
            top_locations=[],
            platforms=[],
            traffic_history=[],
            recent_logs=[],
        )

        if db_service.db is None:
            logger.warning("MongoDB is not initialized. Returning empty analytics metrics.")
            return fallback_response

        try:
            db = db_service.db

            # 1. Total Requests & Platform Stats
            stats_doc = await db.stats.find_one({"_id": "global_counts"})
            total_requests = 0
            platform_counts: Dict[str, int] = {}

            if stats_doc:
                total_requests = int(stats_doc.get("total_requests", 0))
                platform_counts = stats_doc.get("platform_counts", {})

            # Fallback for total_requests if stats doc is missing or 0
            if total_requests == 0:
                total_requests = await db.requests.count_documents({})

            # If platform counts not in stats, aggregate from requests
            if not platform_counts and total_requests > 0:
                platform_agg = await db.requests.aggregate([
                    {"$group": {"_id": "$platform", "count": {"$sum": 1}}}
                ]).to_list(length=20)
                for item in platform_agg:
                    platform_name = item.get("_id") or "unknown"
                    platform_counts[str(platform_name)] = item.get("count", 0)

            platforms = [
                PlatformStat(platform=p, count=c)
                for p, c in sorted(platform_counts.items(), key=lambda x: x[1], reverse=True)
            ]

            # 2. Total Unique Visitors
            total_unique_visitors = await db.visitors.count_documents({})

            # 3. Top Locations
            top_locations: List[LocationStat] = []
            location_pipeline = [
                {
                    "$match": {
                        "location.country": {"$exists": True, "$ne": None}
                    }
                },
                {
                    "$group": {
                        "_id": {
                            "country": "$location.country",
                            "city": "$location.city",
                        },
                        "count": {"$sum": 1},
                        "lat": {"$first": "$location.lat"},
                        "lng": {"$first": "$location.lng"},
                    }
                },
                {"$sort": {"count": -1}},
                {"$limit": 15},
            ]

            location_results = await db.visitors.aggregate(location_pipeline).to_list(length=15)
            for loc in location_results:
                group_id = loc.get("_id", {})
                country = group_id.get("country") or "Unknown"
                city = group_id.get("city")
                lat = loc.get("lat")
                lng = loc.get("lng")
                top_locations.append(
                    LocationStat(
                        country=country,
                        city=city,
                        lat=float(lat) if lat is not None else None,
                        lng=float(lng) if lng is not None else None,
                        count=int(loc.get("count", 0)),
                    )
                )

            # 4. Traffic History (Past 14 days)
            cutoff_date = datetime.now(timezone.utc) - timedelta(days=14)
            traffic_pipeline = [
                {"$match": {"timestamp": {"$gte": cutoff_date}}},
                {
                    "$group": {
                        "_id": {
                            "$dateToString": {
                                "format": "%Y-%m-%d",
                                "date": "$timestamp",
                            }
                        },
                        "requests": {"$sum": 1},
                        "unique_ips": {"$addToSet": "$ip"},
                    }
                },
                {"$sort": {"_id": 1}},
            ]

            traffic_results = await db.requests.aggregate(traffic_pipeline).to_list(length=30)
            traffic_history: List[DailyTraffic] = []
            for item in traffic_results:
                date_str = item.get("_id")
                if date_str:
                    traffic_history.append(
                        DailyTraffic(
                            date=date_str,
                            requests=int(item.get("requests", 0)),
                            unique_visitors=len(item.get("unique_ips", [])),
                        )
                    )

            # 5. Recent Request Logs (Last 15)
            recent_docs = await db.requests.find().sort("timestamp", -1).limit(15).to_list(length=15)
            recent_logs: List[RecentLog] = []

            if recent_docs:
                unique_ips = list({doc.get("ip") for doc in recent_docs if doc.get("ip")})
                visitors_map: Dict[str, Any] = {}
                if unique_ips:
                    visitor_cursor = db.visitors.find({"_id": {"$in": unique_ips}})
                    async for v in visitor_cursor:
                        visitors_map[v["_id"]] = v.get("location")

                for doc in recent_docs:
                    ip = doc.get("ip", "unknown")
                    loc_info = visitors_map.get(ip)
                    location_label = None
                    if loc_info:
                        city = loc_info.get("city")
                        country = loc_info.get("country")
                        if city and country:
                            location_label = f"{city}, {country}"
                        elif country or city:
                            location_label = country or city

                    raw_ts = doc.get("timestamp")
                    if isinstance(raw_ts, datetime):
                        ts_str = raw_ts.isoformat()
                    elif raw_ts:
                        ts_str = str(raw_ts)
                    else:
                        ts_str = datetime.now(timezone.utc).isoformat()

                    recent_logs.append(
                        RecentLog(
                            timestamp=ts_str,
                            ip=ip,
                            platform=doc.get("platform", "unknown"),
                            url=doc.get("url", ""),
                            location=location_label,
                        )
                    )

            return AdminDashboardResponse(
                total_requests=total_requests,
                total_unique_visitors=total_unique_visitors,
                top_locations=top_locations,
                platforms=platforms,
                traffic_history=traffic_history,
                recent_logs=recent_logs,
            )

        except Exception as e:
            logger.error(f"Error fetching admin analytics dashboard metrics: {e}", exc_info=True)
            return fallback_response

    @staticmethod
    async def get_visitors(limit: int = 50, skip: int = 0) -> VisitorListResponse:
        """
        Returns list of visitors (IPs) with location, first/last seen, platforms,
        and total link requests count.
        """
        if db_service.db is None:
            return VisitorListResponse(visitors=[], total_count=0)

        try:
            db = db_service.db
            total_count = await db.visitors.count_documents({})
            docs = await db.visitors.find().sort("last_seen", -1).skip(skip).limit(limit).to_list(length=limit)

            ips = [doc["_id"] for doc in docs if "_id" in doc]

            # Aggregate request counts for these visitors
            count_map: Dict[str, int] = {}
            if ips:
                pipeline = [
                    {"$match": {"ip": {"$in": ips}}},
                    {"$group": {"_id": "$ip", "count": {"$sum": 1}}}
                ]
                count_results = await db.requests.aggregate(pipeline).to_list(length=len(ips))
                count_map = {item["_id"]: int(item.get("count", 0)) for item in count_results}

            visitors: List[VisitorItem] = []
            for doc in docs:
                ip = str(doc.get("_id", "unknown"))
                loc_raw = doc.get("location")
                loc_obj = None
                if loc_raw and isinstance(loc_raw, dict):
                    loc_obj = VisitorLocation(
                        city=loc_raw.get("city"),
                        state=loc_raw.get("state"),
                        country=loc_raw.get("country"),
                        lat=float(loc_raw["lat"]) if loc_raw.get("lat") is not None else None,
                        lng=float(loc_raw["lng"]) if loc_raw.get("lng") is not None else None,
                    )

                first_seen_raw = doc.get("first_seen")
                first_seen_str = first_seen_raw.isoformat() if isinstance(first_seen_raw, datetime) else str(first_seen_raw or "")

                last_seen_raw = doc.get("last_seen")
                last_seen_str = last_seen_raw.isoformat() if isinstance(last_seen_raw, datetime) else str(last_seen_raw or "")

                visitors.append(
                    VisitorItem(
                        ip=ip,
                        first_seen=first_seen_str,
                        last_seen=last_seen_str,
                        platforms=doc.get("platforms", []),
                        location=loc_obj,
                        total_requests=count_map.get(ip, 0),
                    )
                )

            return VisitorListResponse(visitors=visitors, total_count=total_count)

        except Exception as e:
            logger.error(f"Error fetching visitors list: {e}", exc_info=True)
            return VisitorListResponse(visitors=[], total_count=0)

    @staticmethod
    async def get_visitor_requests(ip: str, limit: int = 100) -> VisitorRequestsResponse:
        """
        Returns all link unshortening requests performed by a specific visitor IP.
        """
        if db_service.db is None:
            return VisitorRequestsResponse(ip=ip, total_requests=0, requests=[])

        try:
            db = db_service.db
            total_requests = await db.requests.count_documents({"ip": ip})
            req_docs = await db.requests.find({"ip": ip}).sort("timestamp", -1).limit(limit).to_list(length=limit)

            requests_list: List[VisitorRequestDetail] = []
            for doc in req_docs:
                ts_raw = doc.get("timestamp")
                ts_str = ts_raw.isoformat() if isinstance(ts_raw, datetime) else str(ts_raw or "")
                requests_list.append(
                    VisitorRequestDetail(
                        timestamp=ts_str,
                        url=str(doc.get("url", "")),
                        platform=str(doc.get("platform", "unknown")),
                    )
                )

            return VisitorRequestsResponse(
                ip=ip,
                total_requests=total_requests,
                requests=requests_list,
            )

        except Exception as e:
            logger.error(f"Error fetching requests for visitor {ip}: {e}", exc_info=True)
            return VisitorRequestsResponse(ip=ip, total_requests=0, requests=[])

analytics_service = AnalyticsService()

