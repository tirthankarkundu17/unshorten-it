import { useState, useEffect, useCallback, useRef } from 'react';
import type { AdminDashboardResponse } from '../types/analytics';
import { fetchAdminDashboardMetrics } from '../api/adminApi';

interface UseAdminAnalyticsResult {
  data: AdminDashboardResponse | null;
  isLoading: boolean;
  isRefreshing: boolean;
  error: string | null;
  lastUpdated: Date | null;
  refetch: () => Promise<void>;
  autoRefresh: boolean;
  setAutoRefresh: (val: boolean) => void;
}

export function useAdminAnalytics(
  enabled = true,
  refreshIntervalMs = 30000
): UseAdminAnalyticsResult {
  const [data, setData] = useState<AdminDashboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [autoRefresh, setAutoRefresh] = useState<boolean>(true);
  const isMountedRef = useRef<boolean>(true);

  const loadData = useCallback(async (isInitial = false) => {
    if (!enabled) return;

    if (isInitial) {
      setIsLoading(true);
    } else {
      setIsRefreshing(true);
    }
    setError(null);

    try {
      const result = await fetchAdminDashboardMetrics();
      if (isMountedRef.current) {
        setData(result);
        setLastUpdated(new Date());
      }
    } catch (err: unknown) {
      if (isMountedRef.current) {
        const message = err instanceof Error ? err.message : 'An unexpected error occurred while fetching analytics.';
        setError(message);
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    }
  }, [enabled]);

  const refetch = useCallback(async () => {
    await loadData(false);
  }, [loadData]);

  useEffect(() => {
    isMountedRef.current = true;
    if (enabled) {
      loadData(true);
    }

    return () => {
      isMountedRef.current = false;
    };
  }, [enabled, loadData]);

  useEffect(() => {
    if (!enabled || !autoRefresh) return;

    const intervalId = setInterval(() => {
      loadData(false);
    }, refreshIntervalMs);

    return () => clearInterval(intervalId);
  }, [enabled, autoRefresh, refreshIntervalMs, loadData]);

  return {
    data,
    isLoading,
    isRefreshing,
    error,
    lastUpdated,
    refetch,
    autoRefresh,
    setAutoRefresh,
  };
}
