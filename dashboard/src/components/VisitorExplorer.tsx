import React, { useState, useEffect } from 'react';
import type { VisitorItem } from '../types/analytics';
import { fetchAdminVisitors } from '../api/adminApi';
import {
  Users,
  Search,
  MapPin,
  Clock,
  RefreshCw,
  ChevronRight,
  Shield,
} from 'lucide-react';

interface VisitorExplorerProps {
  onSelectVisitor: (visitor: VisitorItem) => void;
}

export const VisitorExplorer: React.FC<VisitorExplorerProps> = ({ onSelectVisitor }) => {
  const [visitors, setVisitors] = useState<VisitorItem[]>([]);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>('');

  const loadVisitors = async (isInitial = false) => {
    if (isInitial) {
      setIsLoading(true);
    } else {
      setIsRefreshing(true);
    }
    setError(null);

    try {
      const res = await fetchAdminVisitors(100);
      setVisitors(res.visitors);
      setTotalCount(res.total_count);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load visitors.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadVisitors(true);
  }, []);

  const filteredVisitors = visitors.filter((v) => {
    const q = searchQuery.toLowerCase();
    const ipMatch = v.ip.toLowerCase().includes(q);
    const countryMatch = v.location?.country?.toLowerCase().includes(q) ?? false;
    const cityMatch = v.location?.city?.toLowerCase().includes(q) ?? false;
    const platformMatch = v.platforms.some((p) => p.toLowerCase().includes(q));
    return ipMatch || countryMatch || cityMatch || platformMatch;
  });

  const formatLastSeen = (isoStr: string) => {
    try {
      const date = new Date(isoStr);
      return date.toLocaleString([], {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoStr;
    }
  };

  return (
    <div
      style={{
        background: 'var(--bg-card)',
        backdropFilter: 'blur(16px)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-md)',
        padding: '1.75rem',
        boxShadow: 'var(--shadow-card)',
        display: 'flex',
        flexDirection: 'column',
        gap: '1.5rem',
      }}
      className="animate-fade-in"
    >
      {/* Top Header & Search */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <div
              style={{
                background: 'rgba(99, 102, 241, 0.15)',
                border: '1px solid rgba(99, 102, 241, 0.3)',
                color: 'var(--accent-primary)',
                padding: '0.4rem',
                borderRadius: 'var(--radius-sm)',
              }}
            >
              <Users size={20} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                Visitor Explorer ({totalCount.toLocaleString()} Total)
              </h2>
              <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Inspect client IP addresses, geolocations, and click any row to reveal all requested URLs
              </p>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div className="search-input-wrapper">
            <Search size={15} color="var(--text-muted)" />
            <input
              type="text"
              className="search-input"
              placeholder="Search IP, country, city..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <button
            onClick={() => loadVisitors(false)}
            disabled={isLoading || isRefreshing}
            className={`refresh-btn ${isRefreshing ? 'rotating' : ''}`}
            title="Refresh visitors"
          >
            <RefreshCw size={15} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner">
          <span>{error}</span>
          <button onClick={() => loadVisitors(false)} className="retry-btn">
            Retry
          </button>
        </div>
      )}

      {isLoading && visitors.length === 0 ? (
        <div style={{ padding: '4rem 1rem', textAlign: 'center' }}>
          <div className="loading-spinner" style={{ margin: '0 auto 1rem' }} />
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Loading visitors catalog and resolved geolocations...
          </p>
        </div>
      ) : filteredVisitors.length === 0 ? (
        <div style={{ padding: '4rem 1rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <Shield size={36} style={{ opacity: 0.3, marginBottom: '0.5rem' }} />
          <p>No visitors found {searchQuery ? 'matching your search criteria' : 'in database'}.</p>
        </div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
            <thead>
              <tr
                style={{
                  borderBottom: '1px solid var(--border-subtle)',
                  color: 'var(--text-muted)',
                  textAlign: 'left',
                  fontSize: '0.75rem',
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                }}
              >
                <th style={{ padding: '0.75rem 0.75rem', fontWeight: 600 }}>Visitor IP</th>
                <th style={{ padding: '0.75rem 0.75rem', fontWeight: 600 }}>Location</th>
                <th style={{ padding: '0.75rem 0.75rem', fontWeight: 600 }}>Platforms</th>
                <th style={{ padding: '0.75rem 0.75rem', fontWeight: 600 }}>Links Requested</th>
                <th style={{ padding: '0.75rem 0.75rem', fontWeight: 600 }}>Last Active</th>
                <th style={{ padding: '0.75rem 0.75rem', textAlign: 'right', fontWeight: 600 }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredVisitors.map((visitor) => {
                const locationLabel = visitor.location
                  ? [visitor.location.city, visitor.location.country].filter(Boolean).join(', ')
                  : 'Unknown';

                return (
                  <tr
                    key={visitor.ip}
                    onClick={() => onSelectVisitor(visitor)}
                    className="visitor-row"
                    title={`Click to view all URLs requested by ${visitor.ip}`}
                  >
                    <td style={{ padding: '0.85rem 0.75rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span className="visitor-ip-badge">{visitor.ip}</span>
                      </div>
                    </td>

                    <td style={{ padding: '0.85rem 0.75rem', color: 'var(--text-primary)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                        <MapPin size={13} color="var(--accent-cyan)" />
                        <span>{locationLabel}</span>
                      </div>
                    </td>

                    <td style={{ padding: '0.85rem 0.75rem' }}>
                      <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                        {visitor.platforms.map((p) => (
                          <span key={p} className="visitor-platform-pill">
                            {p}
                          </span>
                        ))}
                      </div>
                    </td>

                    <td style={{ padding: '0.85rem 0.75rem' }}>
                      <span
                        style={{
                          fontWeight: 600,
                          color: 'var(--accent-emerald)',
                          fontFamily: 'var(--font-mono)',
                        }}
                      >
                        {visitor.total_requests.toLocaleString()}
                      </span>
                    </td>

                    <td style={{ padding: '0.85rem 0.75rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', fontSize: '0.8rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                        <Clock size={12} />
                        <span>{formatLastSeen(visitor.last_seen)}</span>
                      </div>
                    </td>

                    <td style={{ padding: '0.85rem 0.75rem', textAlign: 'right' }}>
                      <button className="view-links-action-btn">
                        <span>View Links</span>
                        <ChevronRight size={14} />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
