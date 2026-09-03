export interface LocationStat {
  country: string;
  country_code: string | null;
  city: string | null;
  lat: number | null;
  lng: number | null;
  count: number;
}

export interface PlatformStat {
  platform: string;
  count: number;
}

export interface DailyTraffic {
  date: string;
  requests: number;
  unique_visitors: number;
}

export interface RecentLog {
  timestamp: string;
  ip: string;
  platform: string;
  url: string;
  location: string | null;
}

export interface AdminDashboardResponse {
  total_requests: number;
  total_unique_visitors: number;
  top_locations: LocationStat[];
  platforms: PlatformStat[];
  traffic_history: DailyTraffic[];
  recent_logs: RecentLog[];
}

export interface VisitorLocation {
  city: string | null;
  state: string | null;
  country: string | null;
  lat: number | null;
  lng: number | null;
}

export interface VisitorItem {
  ip: string;
  first_seen: string;
  last_seen: string;
  platforms: string[];
  location: VisitorLocation | null;
  total_requests: number;
}

export interface VisitorListResponse {
  visitors: VisitorItem[];
  total_count: number;
}

export interface VisitorRequestDetail {
  timestamp: string;
  url: string;
  platform: string;
}

export interface VisitorRequestsResponse {
  ip: string;
  total_requests: number;
  requests: VisitorRequestDetail[];
}

