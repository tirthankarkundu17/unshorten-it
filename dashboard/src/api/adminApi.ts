import type {
  AdminDashboardResponse,
  VisitorListResponse,
  VisitorRequestsResponse,
  AdminLoginRequest,
  AdminLoginResponse,
  AdminUserResponse,
} from '../types/analytics';

const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || '';
const TOKEN_KEY = 'unshorten_admin_token';

export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setAuthToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAuthToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

function getAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'Accept': 'application/json',
  };
  const token = getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

export async function loginAdmin(creds: AdminLoginRequest): Promise<AdminLoginResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
    body: JSON.stringify(creds),
  });

  if (!response.ok) {
    let errorMessage = 'Invalid admin credentials';
    try {
      const errorBody: { error?: { message?: string } } = await response.json();
      if (errorBody.error?.message) {
        errorMessage = errorBody.error.message;
      }
    } catch {
      // Ignore JSON error
    }
    throw new Error(errorMessage);
  }

  const data: AdminLoginResponse = await response.json();
  setAuthToken(data.access_token);
  return data;
}

export async function verifyAdminSession(): Promise<AdminUserResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/me`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    clearAuthToken();
    throw new Error('Session expired or unauthorized');
  }

  const data: AdminUserResponse = await response.json();
  return data;
}

export async function fetchAdminDashboardMetrics(): Promise<AdminDashboardResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/admin/analytics/dashboard`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    if (response.status === 401) {
      clearAuthToken();
    }
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
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    if (response.status === 401) {
      clearAuthToken();
    }
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
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    if (response.status === 401) {
      clearAuthToken();
    }
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
