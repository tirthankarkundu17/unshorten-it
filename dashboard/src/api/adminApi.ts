import type {
  AdminDashboardResponse,
  VisitorListResponse,
  VisitorRequestsResponse,
} from '../types/analytics';

const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || '';

export async function fetchAdminDashboardMetrics(): Promise<AdminDashboardResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/analytics/dashboard`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    let errorMessage = `Failed to fetch admin metrics (${response.status})`;
    try {
      const errorBody: { error?: { message?: string } } = await response.json();
      if (errorBody.error?.message) {
        errorMessage = errorBody.error.message;
      }
    } catch {
      // Ignore JSON parse error and use status message
    }
    throw new Error(errorMessage);
  }

  const data: AdminDashboardResponse = await response.json();
  return data;
}

export async function fetchAdminVisitors(limit = 100): Promise<VisitorListResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/analytics/visitors?limit=${limit}`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    let errorMessage = `Failed to fetch visitors (${response.status})`;
    try {
      const errorBody: { error?: { message?: string } } = await response.json();
      if (errorBody.error?.message) {
        errorMessage = errorBody.error.message;
      }
    } catch {
      // Ignore JSON parse error
    }
    throw new Error(errorMessage);
  }

  const data: VisitorListResponse = await response.json();
  return data;
}

export async function fetchVisitorRequests(ip: string, limit = 100): Promise<VisitorRequestsResponse> {
  const encodedIp = encodeURIComponent(ip);
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/analytics/visitors/${encodedIp}/requests?limit=${limit}`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    let errorMessage = `Failed to fetch requests for visitor ${ip} (${response.status})`;
    try {
      const errorBody: { error?: { message?: string } } = await response.json();
      if (errorBody.error?.message) {
        errorMessage = errorBody.error.message;
      }
    } catch {
      // Ignore JSON parse error
    }
    throw new Error(errorMessage);
  }

  const data: VisitorRequestsResponse = await response.json();
  return data;
}

