import React, { useState, useEffect } from 'react';
import { useAdminAnalytics } from './hooks/useAdminAnalytics';
import { MetricCard } from './components/MetricCard';
import { TrafficChart } from './components/TrafficChart';
import { GeoDistribution } from './components/GeoDistribution';
import { PlatformBreakdown } from './components/PlatformBreakdown';
import { RecentLogsTable } from './components/RecentLogsTable';
import { VisitorExplorer } from './components/VisitorExplorer';
import { VisitorDetailModal } from './components/VisitorDetailModal';
import { LoginScreen } from './components/LoginScreen';
import { ThemeSelector } from './components/ThemeSelector';
import { useTheme } from './hooks/useTheme';
import {
  getAuthToken,
  clearAuthToken,
  verifyAdminSession,
} from './api/adminApi';
import type { VisitorItem } from './types/analytics';
import {
  Activity,
  Users,
  Globe,
  Smartphone,
  RefreshCw,
  AlertTriangle,
  ShieldCheck,
  Server,
  Database,
  LayoutDashboard,
  UserCheck,
  LogOut,
  User,
  Download,
} from 'lucide-react';
import './App.css';

export const App: React.FC = () => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => Boolean(getAuthToken()));
  const [isCheckingAuth, setIsCheckingAuth] = useState<boolean>(true);
  const [adminUsername, setAdminUsername] = useState<string>('admin');
  const [activeTab, setActiveTab] = useState<'overview' | 'visitors'>('overview');
  const [selectedVisitor, setSelectedVisitor] = useState<VisitorItem | null>(null);
  const [installPrompt, setInstallPrompt] = useState<{ prompt: () => Promise<void>; userChoice: Promise<{ outcome: string }> } | null>(null);
  const { themePreference, setTheme } = useTheme();

  useEffect(() => {
    const handleBeforeInstall = (e: Event) => {
      e.preventDefault();
      setInstallPrompt(e as unknown as { prompt: () => Promise<void>; userChoice: Promise<{ outcome: string }> });
    };
    window.addEventListener('beforeinstallprompt', handleBeforeInstall);
    return () => window.removeEventListener('beforeinstallprompt', handleBeforeInstall);
  }, []);

  const handleInstallClick = async () => {
    if (!installPrompt) return;
    await installPrompt.prompt();
    const choice = await installPrompt.userChoice;
    if (choice.outcome === 'accepted') {
      setInstallPrompt(null);
    }
  };

  useEffect(() => {
    const token = getAuthToken();
    if (!token) {
      setIsAuthenticated(false);
      setIsCheckingAuth(false);
      return;
    }

    verifyAdminSession()
      .then((user) => {
        setIsAuthenticated(true);
        setAdminUsername(user.username);
      })
      .catch(() => {
        setIsAuthenticated(false);
      })
      .finally(() => {
        setIsCheckingAuth(false);
      });
  }, []);

  const {
    data,
    isLoading,
    isRefreshing,
    error,
    lastUpdated,
    refetch,
    autoRefresh,
    setAutoRefresh,
  } = useAdminAnalytics(isAuthenticated, 30000);

  // Catch unauthenticated API errors and redirect to login
  useEffect(() => {
    if (error && (error.includes('401') || error.toLowerCase().includes('unauthorized') || error.toLowerCase().includes('expired'))) {
      clearAuthToken();
      setIsAuthenticated(false);
    }
  }, [error]);

  const handleLogout = () => {
    clearAuthToken();
    setIsAuthenticated(false);
  };

  if (isCheckingAuth) {
    return (
      <div className="login-wrapper">
        <div className="loading-spinner" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <LoginScreen
        onLoginSuccess={(user) => {
          setAdminUsername(user);
          setIsAuthenticated(true);
        }}
        currentTheme={themePreference}
        onThemeChange={setTheme}
      />
    );
  }

  const topLocation = data?.top_locations && data.top_locations.length > 0
    ? `${data.top_locations[0].country}${data.top_locations[0].city ? ` (${data.top_locations[0].city})` : ''}`
    : 'None';

  const topPlatform = data?.platforms && data.platforms.length > 0
    ? `${data.platforms[0].platform.toUpperCase()} (${data.platforms[0].count.toLocaleString()})`
    : 'None';

  return (
    <div className="admin-container">
      {/* Top Navbar */}
      <header className="admin-header">
        <div className="brand-section">
          <div className="brand-logo">
            <ShieldCheck size={26} color="#6366f1" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <h1 className="brand-title">Unshorten It</h1>
              <span className="admin-badge">ADMIN</span>
            </div>
            <p className="brand-subtitle">Telemetry & Traffic Intelligence</p>
          </div>
        </div>

        {/* Center Nav Tabs */}
        <nav className="nav-tabs-container">
          <button
            className={`nav-tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            <LayoutDashboard size={16} />
            <span>Overview Analytics</span>
          </button>
          <button
            className={`nav-tab-btn ${activeTab === 'visitors' ? 'active' : ''}`}
            onClick={() => setActiveTab('visitors')}
          >
            <UserCheck size={16} />
            <span>Visitor Explorer</span>
          </button>
        </nav>

        {/* Action Controls */}
        <div className="controls-section">
          {lastUpdated && activeTab === 'overview' && (
            <span className="last-updated-text">
              Updated: {lastUpdated.toLocaleTimeString()}
            </span>
          )}

          {activeTab === 'overview' && (
            <label className="auto-refresh-toggle" title="Auto refresh every 30 seconds">
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
              />
              <span className="toggle-label">Auto (30s)</span>
            </label>
          )}

          {activeTab === 'overview' && (
            <button
              onClick={() => refetch()}
              disabled={isLoading || isRefreshing}
              className={`refresh-btn ${isRefreshing ? 'rotating' : ''}`}
              title="Refresh metrics"
            >
              <RefreshCw size={16} />
              <span>Refresh</span>
            </button>
          )}

          {installPrompt && (
            <button
              onClick={handleInstallClick}
              className="refresh-btn"
              style={{
                background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.25), rgba(139, 92, 246, 0.25))',
                borderColor: 'var(--border-highlight)',
                color: '#c7d2fe',
              }}
              title="Install Admin Dashboard as a Desktop / Mobile PWA App"
            >
              <Download size={16} />
              <span>Install App</span>
            </button>
          )}

          <ThemeSelector currentTheme={themePreference} onThemeChange={setTheme} />

          <div className="admin-user-pill">
            <User size={14} color="var(--accent-cyan)" />
            <span>{adminUsername}</span>
          </div>

          <button
            onClick={handleLogout}
            className="logout-btn"
            title="Log out of admin console"
          >
            <LogOut size={16} />
            <span>Logout</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="admin-content">
        {activeTab === 'overview' ? (
          <>
            {error && (
              <div className="error-banner animate-fade-in">
                <AlertTriangle size={20} />
                <div style={{ flex: 1 }}>
                  <strong>Failed to load telemetry data:</strong> {error}
                </div>
                <button onClick={() => refetch()} className="retry-btn">
                  Try Again
                </button>
              </div>
            )}

            {isLoading && !data ? (
              <div className="loading-container">
                <div className="loading-spinner" />
                <p style={{ color: 'var(--text-secondary)', marginTop: '1rem', fontSize: '0.95rem' }}>
                  Aggregating cluster telemetry and visitor metrics...
                </p>
              </div>
            ) : data ? (
              <div className="dashboard-grid animate-fade-in">
                {/* KPI Metric Cards */}
                <section className="metrics-row">
                  <MetricCard
                    title="TOTAL REQUESTS"
                    value={data.total_requests}
                    subtitle="Cumulative processed URLs"
                    icon={Activity}
                    color="indigo"
                  />
                  <MetricCard
                    title="UNIQUE VISITORS"
                    value={data.total_unique_visitors}
                    subtitle="Identified client IP addresses"
                    icon={Users}
                    color="cyan"
                  />
                  <MetricCard
                    title="TOP GEOLOCATION"
                    value={topLocation}
                    subtitle="Highest traffic region"
                    icon={Globe}
                    color="emerald"
                  />
                  <MetricCard
                    title="DOMINANT PLATFORM"
                    value={topPlatform}
                    subtitle="Primary source application"
                    icon={Smartphone}
                    color="purple"
                  />
                </section>

                {/* Middle Section: Traffic Trends & Geo Breakdown */}
                <section className="analytics-middle-grid">
                  <div className="middle-left-col">
                    <TrafficChart history={data.traffic_history} />
                    <GeoDistribution
                      locations={data.top_locations}
                      totalUniqueVisitors={data.total_unique_visitors}
                    />
                  </div>

                  <div className="middle-right-col">
                    <PlatformBreakdown
                      platforms={data.platforms}
                      totalRequests={data.total_requests}
                    />

                    {/* System Status Panel */}
                    <div className="system-status-card">
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                        <Server size={18} color="var(--accent-primary)" />
                        <h3 style={{ fontSize: '1rem', fontWeight: 600 }}>Backend & Ingestion Health</h3>
                      </div>

                      <div className="status-item">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <Database size={15} color="var(--accent-emerald)" />
                          <span>MongoDB Cluster</span>
                        </div>
                        <span className="status-pill status-online">Connected</span>
                      </div>

                      <div className="status-item">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <Activity size={15} color="var(--accent-emerald)" />
                          <span>Telemetry Ingestion</span>
                        </div>
                        <span className="status-pill status-online">Active</span>
                      </div>

                      <div className="status-item">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <Globe size={15} color="var(--accent-cyan)" />
                          <span>GeoIP Resolution</span>
                        </div>
                        <span className="status-pill status-online">Async Geocoder</span>
                      </div>
                    </div>
                  </div>
                </section>

                {/* Bottom Section: Live Request Stream */}
                <section className="logs-row">
                  <RecentLogsTable logs={data.recent_logs} />
                </section>
              </div>
            ) : null}
          </>
        ) : (
          /* Visitor Explorer Tab */
          <VisitorExplorer
            onSelectVisitor={(visitor) => setSelectedVisitor(visitor)}
          />
        )}
      </main>

      {/* Visitor Detail Modal */}
      <VisitorDetailModal
        visitor={selectedVisitor}
        onClose={() => setSelectedVisitor(null)}
      />
    </div>
  );
};

export default App;
