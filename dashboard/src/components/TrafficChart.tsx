import React, { useState } from 'react';
import type { DailyTraffic } from '../types/analytics';
import { Activity, Users } from 'lucide-react';

interface TrafficChartProps {
  history: DailyTraffic[];
}

export const TrafficChart: React.FC<TrafficChartProps> = ({ history }) => {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);

  if (!history || history.length === 0) {
    return (
      <div
        style={{
          background: 'var(--bg-card)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-subtle)',
          padding: '2rem',
          textAlign: 'center',
          color: 'var(--text-muted)',
        }}
      >
        <Activity size={32} style={{ opacity: 0.3, marginBottom: '0.5rem' }} />
        <p>No traffic history recorded yet.</p>
      </div>
    );
  }

  const maxVal = Math.max(
    ...history.map((d) => Math.max(d.requests, d.unique_visitors)),
    1
  );

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
        gap: '1.25rem',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
        <div>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            Traffic Activity (14 Days)
          </h3>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Daily requests and unique visitor volume
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', fontSize: '0.8rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
            <span style={{ width: 10, height: 10, borderRadius: 2, background: 'linear-gradient(to top, #6366f1, #8b5cf6)' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Requests</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
            <span style={{ width: 10, height: 10, borderRadius: 2, background: '#06b6d4' }} />
            <span style={{ color: 'var(--text-secondary)' }}>Visitors</span>
          </div>
        </div>
      </div>

      {/* Chart visualization */}
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-end',
          height: 180,
          gap: '8px',
          paddingTop: '20px',
          paddingBottom: '4px',
          borderBottom: '1px solid var(--border-subtle)',
          position: 'relative',
        }}
      >
        {history.map((day, idx) => {
          const reqPercent = day.requests > 0 ? Math.max(8, (day.requests / maxVal) * 100) : 0;
          const visPercent = day.unique_visitors > 0 ? Math.max(8, (day.unique_visitors / maxVal) * 100) : 0;
          const maxGroupHeight = Math.max(reqPercent, visPercent);
          const isHovered = hoveredIdx === idx;

          return (
            <div
              key={day.date}
              onMouseEnter={() => setHoveredIdx(idx)}
              onMouseLeave={() => setHoveredIdx(null)}
              style={{
                flex: 1,
                display: 'flex',
                alignItems: 'flex-end',
                justifyContent: 'center',
                gap: '3px',
                height: '100%',
                position: 'relative',
                cursor: 'pointer',
                padding: '0 1px',
              }}
            >
              {/* Tooltip */}
              {isHovered && (
                <div
                  style={{
                    position: 'absolute',
                    bottom: `${Math.min(maxGroupHeight + 14, 110)}%`,
                    left: '50%',
                    transform: 'translateX(-50%)',
                    background: 'var(--bg-modal)',
                    border: '1px solid var(--border-highlight)',
                    borderRadius: 'var(--radius-sm)',
                    padding: '0.5rem 0.75rem',
                    boxShadow: 'var(--shadow-card)',
                    zIndex: 20,
                    whiteSpace: 'nowrap',
                    pointerEvents: 'none',
                  }}
                  className="animate-fade-in"
                >
                  <p style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: 3 }}>
                    {day.date}
                  </p>
                  <p style={{ fontSize: '0.75rem', color: '#818cf8', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <Activity size={12} /> {day.requests.toLocaleString()} requests
                  </p>
                  <p style={{ fontSize: '0.75rem', color: '#06b6d4', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <Users size={12} /> {day.unique_visitors.toLocaleString()} unique visitors
                  </p>
                </div>
              )}

              {/* Bar 1: Requests */}
              <div
                style={{
                  width: '50%',
                  maxWidth: 16,
                  height: `${reqPercent}%`,
                  background: isHovered
                    ? 'linear-gradient(180deg, #a78bfa 0%, #6366f1 100%)'
                    : 'linear-gradient(180deg, #818cf8 0%, #4f46e5 100%)',
                  borderRadius: '3px 3px 0 0',
                  boxShadow: isHovered ? '0 0 12px rgba(99, 102, 241, 0.5)' : 'none',
                  transition: 'var(--transition-smooth)',
                }}
                title={`${day.requests} requests`}
              />

              {/* Bar 2: Unique Visitors */}
              <div
                style={{
                  width: '50%',
                  maxWidth: 16,
                  height: `${visPercent}%`,
                  background: isHovered
                    ? 'linear-gradient(180deg, #67e8f9 0%, #06b6d4 100%)'
                    : 'linear-gradient(180deg, #22d3ee 0%, #0891b2 100%)',
                  borderRadius: '3px 3px 0 0',
                  boxShadow: isHovered ? '0 0 12px rgba(6, 182, 212, 0.5)' : 'none',
                  transition: 'var(--transition-smooth)',
                }}
                title={`${day.unique_visitors} unique visitors`}
              />
            </div>
          );
        })}
      </div>

      {/* Date labels */}
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', color: 'var(--text-muted)' }}>
        <span>{history[0]?.date || ''}</span>
        <span>{history[Math.floor(history.length / 2)]?.date || ''}</span>
        <span>{history[history.length - 1]?.date || ''}</span>
      </div>
    </div>
  );
};
