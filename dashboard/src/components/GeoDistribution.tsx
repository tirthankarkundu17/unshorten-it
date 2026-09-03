import React from 'react';
import type { LocationStat } from '../types/analytics';
import { Globe, MapPin } from 'lucide-react';

interface GeoDistributionProps {
  locations: LocationStat[];
  totalUniqueVisitors: number;
}

export const GeoDistribution: React.FC<GeoDistributionProps> = ({
  locations,
  totalUniqueVisitors,
}) => {
  const totalGeoCount = locations.reduce((sum, loc) => sum + loc.count, 0) || totalUniqueVisitors || 1;

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
              background: 'rgba(6, 182, 212, 0.15)',
              border: '1px solid rgba(6, 182, 212, 0.3)',
              color: 'var(--accent-cyan)',
              padding: '0.4rem',
              borderRadius: 'var(--radius-sm)',
            }}
          >
            <Globe size={18} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              Geographic Distribution
            </h3>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Top countries and cities by active visitor count
            </p>
          </div>
        </div>
      </div>

      {locations.length === 0 ? (
        <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <MapPin size={32} style={{ opacity: 0.3, marginBottom: '0.5rem' }} />
          <p>No location data detected yet.</p>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Locations are automatically detected when public IP traffic arrives.
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
          {locations.map((loc, idx) => {
            const percentage = Math.min(100, Math.round((loc.count / totalGeoCount) * 100));
            const locationLabel = loc.city ? `${loc.city}, ${loc.country}` : loc.country;

            return (
              <div
                key={`${loc.country}-${loc.city || idx}`}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.375rem',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.875rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <span
                      style={{
                        display: 'inline-block',
                        width: '20px',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        color: 'var(--text-muted)',
                      }}
                    >
                      #{idx + 1}
                    </span>
                    <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>
                      {locationLabel}
                    </span>
                    {loc.lat !== null && loc.lng !== null && (
                      <span
                        style={{
                          fontSize: '0.7rem',
                          fontFamily: 'var(--font-mono)',
                          color: 'var(--text-muted)',
                          background: 'rgba(255, 255, 255, 0.04)',
                          padding: '0.1rem 0.35rem',
                          borderRadius: '4px',
                        }}
                      >
                        {loc.lat.toFixed(2)}, {loc.lng.toFixed(2)}
                      </span>
                    )}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span style={{ fontWeight: 600, color: 'var(--accent-cyan)' }}>
                      {loc.count.toLocaleString()}
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
                      background: 'linear-gradient(90deg, #06b6d4, #3b82f6)',
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
