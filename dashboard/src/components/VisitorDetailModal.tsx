import React, { useEffect, useState } from 'react';
import type { VisitorItem, VisitorRequestsResponse } from '../types/analytics';
import { fetchVisitorRequests } from '../api/adminApi';
import {
  X,
  MapPin,
  Clock,
  ExternalLink,
  Copy,
  Check,
  Smartphone,
  Globe,
  Radio,
  Search,
} from 'lucide-react';

interface VisitorDetailModalProps {
  visitor: VisitorItem | null;
  onClose: () => void;
}

export const VisitorDetailModal: React.FC<VisitorDetailModalProps> = ({
  visitor,
  onClose,
}) => {
  const [data, setData] = useState<VisitorRequestsResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [copiedUrl, setCopiedUrl] = useState<string | null>(null);
  const [filterQuery, setFilterQuery] = useState<string>('');

  useEffect(() => {
    if (!visitor) {
      setData(null);
      return;
    }

    let isMounted = true;
    setIsLoading(true);
    setError(null);
    setFilterQuery('');

    fetchVisitorRequests(visitor.ip)
      .then((res) => {
        if (isMounted) {
          setData(res);
          setIsLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          setError(err instanceof Error ? err.message : 'Failed to load visitor requests.');
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [visitor]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  if (!visitor) return null;

  const handleCopy = (url: string) => {
    navigator.clipboard.writeText(url);
    setCopiedUrl(url);
    setTimeout(() => setCopiedUrl(null), 2000);
  };

  const locationLabel = visitor.location
    ? [visitor.location.city, visitor.location.state, visitor.location.country]
        .filter(Boolean)
        .join(', ')
    : 'Unknown Location';

  const filteredRequests = (data?.requests || []).filter((req) =>
    req.url.toLowerCase().includes(filterQuery.toLowerCase())
  );

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-content animate-fade-in"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="modal-header">
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <h2 className="modal-title">{visitor.ip}</h2>
              <span className="ip-badge">IP DETAILS</span>
            </div>
            <p className="modal-subtitle">
              Visitor profile and full unshorten request history
            </p>
          </div>
          <button className="modal-close-btn" onClick={onClose} title="Close (Esc)">
            <X size={20} />
          </button>
        </div>

        {/* Visitor Metadata Summary */}
        <div className="visitor-meta-grid">
          <div className="meta-box">
            <span className="meta-label">Location</span>
            <div className="meta-value">
              <MapPin size={15} color="var(--accent-cyan)" />
              <span>{locationLabel}</span>
            </div>
            {visitor.location?.lat !== null && visitor.location?.lng !== null && visitor.location?.lat !== undefined && (
              <span className="meta-sub">
                {visitor.location.lat.toFixed(4)}, {visitor.location.lng.toFixed(4)}
              </span>
            )}
          </div>

          <div className="meta-box">
            <span className="meta-label">Platforms</span>
            <div className="meta-value">
              <Smartphone size={15} color="var(--accent-primary)" />
              <span>{visitor.platforms.join(', ') || 'unknown'}</span>
            </div>
            <span className="meta-sub">Active client apps</span>
          </div>

          <div className="meta-box">
            <span className="meta-label">Total Links Requested</span>
            <div className="meta-value">
              <Radio size={15} color="var(--accent-emerald)" />
              <span style={{ fontWeight: 700, color: 'var(--accent-emerald)' }}>
                {data ? data.total_requests.toLocaleString() : visitor.total_requests.toLocaleString()}
              </span>
            </div>
            <span className="meta-sub">Requests logged in DB</span>
          </div>

          <div className="meta-box">
            <span className="meta-label">Activity Timeline</span>
            <div className="meta-value">
              <Clock size={15} color="var(--accent-amber)" />
              <span>{new Date(visitor.last_seen).toLocaleDateString()}</span>
            </div>
            <span className="meta-sub">
              Last seen: {new Date(visitor.last_seen).toLocaleTimeString()}
            </span>
          </div>
        </div>

        {/* Links Section */}
        <div className="modal-links-section">
          <div className="links-section-header">
            <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              Requested URLs ({filteredRequests.length})
            </h3>
            <div className="search-input-wrapper">
              <Search size={14} color="var(--text-muted)" />
              <input
                type="text"
                className="search-input"
                placeholder="Filter requested links..."
                value={filterQuery}
                onChange={(e) => setFilterQuery(e.target.value)}
              />
            </div>
          </div>

          {isLoading ? (
            <div style={{ padding: '3rem 1rem', textAlign: 'center' }}>
              <div className="loading-spinner" style={{ margin: '0 auto 1rem' }} />
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                Fetching URLs requested by {visitor.ip}...
              </p>
            </div>
          ) : error ? (
            <div className="error-banner" style={{ margin: '1rem 0' }}>
              <span>{error}</span>
            </div>
          ) : filteredRequests.length === 0 ? (
            <div style={{ padding: '3rem 1rem', textAlign: 'center', color: 'var(--text-muted)' }}>
              <Globe size={32} style={{ opacity: 0.3, marginBottom: '0.5rem' }} />
              <p>No URLs found {filterQuery ? 'matching your search' : 'for this visitor'}.</p>
            </div>
          ) : (
            <div className="urls-list-container">
              {filteredRequests.map((req, idx) => {
                const isCopied = copiedUrl === req.url;
                const formattedTime = new Date(req.timestamp).toLocaleString([], {
                  month: 'short',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                  second: '2-digit',
                });

                return (
                  <div key={`${req.timestamp}-${idx}`} className="url-item-card">
                    <div className="url-item-top">
                      <span className="url-timestamp">{formattedTime}</span>
                      <span className="url-platform-tag">{req.platform}</span>
                    </div>

                    <div className="url-item-main">
                      <a
                        href={req.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="url-link-text"
                        title={req.url}
                      >
                        {req.url}
                        <ExternalLink size={13} style={{ flexShrink: 0, opacity: 0.7 }} />
                      </a>

                      <button
                        onClick={() => handleCopy(req.url)}
                        className="url-copy-btn"
                        title="Copy URL"
                      >
                        {isCopied ? <Check size={14} color="var(--accent-emerald)" /> : <Copy size={14} />}
                        <span>{isCopied ? 'Copied' : 'Copy'}</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
