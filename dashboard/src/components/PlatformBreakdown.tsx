import React from 'react';
import type { PlatformStat } from '../types/analytics';
import { Smartphone, Globe, Terminal, Layers } from 'lucide-react';

interface PlatformBreakdownProps {
  platforms: PlatformStat[];
  totalRequests: number;
}

export const PlatformBreakdown: React.FC<PlatformBreakdownProps> = ({
  platforms,
  totalRequests,
}) => {
  const safeTotal = totalRequests || platforms.reduce((acc, p) => acc + p.count, 0) || 1;

  const getPlatformIcon = (platform: string) => {
    switch (platform.toLowerCase()) {
      case 'web':
        return Globe;
      case 'android':
        return Smartphone;
      case 'api':
        return Terminal;
      default:
        return Layers;
    }
  };

  const getPlatformColor = (platform: string) => {
    switch (platform.toLowerCase()) {
      case 'web':
        return {
          gradient: 'linear-gradient(90deg, #6366f1, #8b5cf6)',
          accent: '#818cf8',
        };
      case 'android':
        return {
          gradient: 'linear-gradient(90deg, #10b981, #06b6d4)',
          accent: '#34d399',
        };
      case 'api':
        return {
          gradient: 'linear-gradient(90deg, #f59e0b, #f97316)',
          accent: '#fbbf24',
        };
      default:
        return {
          gradient: 'linear-gradient(90deg, #a855f7, #ec4899)',
          accent: '#c084fc',
        };
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
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
        <div
          style={{
            background: 'rgba(16, 185, 129, 0.15)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            color: 'var(--accent-emerald)',
            padding: '0.4rem',
            borderRadius: 'var(--radius-sm)',
          }}
        >
          <Smartphone size={18} />
        </div>
        <div>
          <h3 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            Platform Breakdown
          </h3>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Client applications sending requests
          </p>
        </div>
      </div>

      {platforms.length === 0 ? (
        <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <Layers size={32} style={{ opacity: 0.3, marginBottom: '0.5rem' }} />
          <p>No platform statistics recorded yet.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
          {platforms.map((item) => {
            const Icon = getPlatformIcon(item.platform);
            const { gradient, accent } = getPlatformColor(item.platform);
            const percentage = Math.min(100, Math.round((item.count / safeTotal) * 100));

            return (
              <div key={item.platform} style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.875rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Icon size={16} color={accent} />
                    <span style={{ fontWeight: 500, color: 'var(--text-primary)', textTransform: 'capitalize' }}>
                      {item.platform}
                    </span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span style={{ fontWeight: 600, color: accent }}>
                      {item.count.toLocaleString()}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', width: '38px', textAlign: 'right' }}>
                      {percentage}%
                    </span>
                  </div>
                </div>

                {/* Progress Bar */}
                <div
                  style={{
                    height: '6px',
                    width: '100%',
                    background: 'rgba(255, 255, 255, 0.05)',
                    borderRadius: 'var(--radius-full)',
                    overflow: 'hidden',
                  }}
                >
                  <div
                    style={{
                      height: '100%',
                      width: `${percentage}%`,
                      background: gradient,
                      borderRadius: 'var(--radius-full)',
                      transition: 'width 0.5s ease-out',
                    }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
