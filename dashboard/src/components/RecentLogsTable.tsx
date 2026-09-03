import React, { useState } from 'react';
import type { RecentLog } from '../types/analytics';
import { Radio, Copy, Check, MapPin } from 'lucide-react';

interface RecentLogsTableProps {
  logs: RecentLog[];
}

export const RecentLogsTable: React.FC<RecentLogsTableProps> = ({ logs }) => {
  const [copiedUrl, setCopiedUrl] = useState<string | null>(null);

  const handleCopy = (url: string) => {
    navigator.clipboard.writeText(url);
    setCopiedUrl(url);
    setTimeout(() => setCopiedUrl(null), 2000);
  };

  const formatTimestamp = (ts: string) => {
    try {
      const date = new Date(ts);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      return ts;
    }
  };

  return (
    <div
      style={{
        background: 'var(--bg-card)',
        backdropFilter: 'blur(16px)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-md)',
        padding: '1.5rem',
        boxShadow: 'var(--shadow-card)',
        display: 'flex',
        flexDirection: 'column',
        gap: '1rem',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
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
            <Radio size={18} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              Live Request Stream
            </h3>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Recent unshorten requests processed by backend
            </p>
          </div>
        </div>
        <span style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.75rem', color: 'var(--accent-emerald)' }}>
          <span className="live-indicator" /> Live
        </span>
      </div>

      {logs.length === 0 ? (
        <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <Radio size={32} style={{ opacity: 0.3, marginBottom: '0.5rem' }} />
          <p>No recent requests logged yet.</p>
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
                <th style={{ padding: '0.75rem 0.5rem', fontWeight: 600 }}>Time</th>
                <th style={{ padding: '0.75rem 0.5rem', fontWeight: 600 }}>Platform</th>
                <th style={{ padding: '0.75rem 0.5rem', fontWeight: 600 }}>Client IP</th>
                <th style={{ padding: '0.75rem 0.5rem', fontWeight: 600 }}>Location</th>
                <th style={{ padding: '0.75rem 0.5rem', fontWeight: 600 }}>Target URL</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log, idx) => {
                const isCopied = copiedUrl === log.url;

                return (
                  <tr
                    key={`${log.timestamp}-${idx}`}
                    style={{
                      borderBottom: '1px solid rgba(255, 255, 255, 0.04)',
                      transition: 'background 0.15s ease',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'rgba(255, 255, 255, 0.02)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'transparent';
                    }}
                  >
                    <td style={{ padding: '0.75rem 0.5rem', fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                      {formatTimestamp(log.timestamp)}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', whiteSpace: 'nowrap' }}>
                      <span
                        style={{
                          fontSize: '0.75rem',
                          fontWeight: 500,
                          padding: '0.2rem 0.5rem',
                          borderRadius: 'var(--radius-full)',
                          textTransform: 'uppercase',
                          letterSpacing: '0.04em',
                          background:
                            log.platform === 'web'
                              ? 'rgba(99, 102, 241, 0.15)'
                              : log.platform === 'android'
                              ? 'rgba(16, 185, 129, 0.15)'
                              : 'rgba(245, 158, 11, 0.15)',
                          color:
                            log.platform === 'web'
                              ? '#a5b4fc'
                              : log.platform === 'android'
                              ? '#6ee7b7'
                              : '#fcd34d',
                          border: `1px solid ${
                            log.platform === 'web'
                              ? 'rgba(99, 102, 241, 0.3)'
                              : log.platform === 'android'
                              ? 'rgba(16, 185, 129, 0.3)'
                              : 'rgba(245, 158, 11, 0.3)'
                          }`,
                        }}
                      >
                        {log.platform}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                      {log.ip}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                      {log.location ? (
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                          <MapPin size={12} color="var(--accent-cyan)" />
                          {log.location}
                        </span>
                      ) : (
                        <span style={{ color: 'var(--text-muted)' }}>Local / Unknown</span>
                      )}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', maxWidth: '320px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span
                          style={{
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            color: 'var(--text-primary)',
                            fontFamily: 'var(--font-mono)',
                            fontSize: '0.8rem',
                          }}
                          title={log.url}
                        >
                          {log.url}
                        </span>
                        <button
                          onClick={() => handleCopy(log.url)}
                          title="Copy URL"
                          style={{
                            background: 'transparent',
                            border: 'none',
                            color: isCopied ? 'var(--accent-emerald)' : 'var(--text-muted)',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            padding: '2px',
                            flexShrink: 0,
                          }}
                        >
                          {isCopied ? <Check size={14} /> : <Copy size={14} />}
                        </button>
                      </div>
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
