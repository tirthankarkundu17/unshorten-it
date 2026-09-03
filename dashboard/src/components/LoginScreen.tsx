import React, { useState } from 'react';
import { loginAdmin } from '../api/adminApi';
import {
  ShieldCheck,
  Lock,
  User,
  Eye,
  EyeOff,
  ArrowRight,
  AlertTriangle,
} from 'lucide-react';
import type { ThemePreference } from '../hooks/useTheme';
import { ThemeSelector } from './ThemeSelector';

interface LoginScreenProps {
  onLoginSuccess: (username: string) => void;
  currentTheme?: ThemePreference;
  onThemeChange?: (theme: ThemePreference) => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({
  onLoginSuccess,
  currentTheme,
  onThemeChange,
}) => {
  const [username, setUsername] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      setError('Please enter both username and password.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      await loginAdmin({
        username: username.trim(),
        password,
      });
      onLoginSuccess(username.trim());
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Invalid credentials. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-wrapper">
      {currentTheme && onThemeChange && (
        <div style={{ position: 'absolute', top: '1.5rem', right: '1.5rem', zIndex: 10 }}>
          <ThemeSelector currentTheme={currentTheme} onThemeChange={onThemeChange} />
        </div>
      )}
      <div className="login-card animate-fade-in">
        {/* Brand & Icon */}
        <div className="login-header">
          <div className="login-icon-badge">
            <ShieldCheck size={32} color="#6366f1" />
          </div>
          <h1 className="login-title">Admin Authentication</h1>
          <p className="login-subtitle">
            Unshorten It Telemetry & Analytics Console
          </p>
        </div>

        {/* Error Feedback */}
        {error && (
          <div className="login-error-alert animate-fade-in">
            <AlertTriangle size={18} style={{ flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        )}

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="login-form">
          <div className="login-field">
            <label className="login-label">Username</label>
            <div className="login-input-box">
              <User size={18} color="var(--text-muted)" style={{ flexShrink: 0 }} />
              <input
                type="text"
                className="login-input"
                placeholder="Enter admin username..."
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={isLoading}
                autoFocus
                autoComplete="username"
              />
            </div>
          </div>

          <div className="login-field">
            <label className="login-label">Password</label>
            <div className="login-input-box">
              <Lock size={18} color="var(--text-muted)" style={{ flexShrink: 0 }} />
              <input
                type={showPassword ? 'text' : 'password'}
                className="login-input"
                placeholder="Enter admin password..."
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={isLoading}
                autoComplete="current-password"
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword(!showPassword)}
                title={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className="login-submit-btn"
            disabled={isLoading || !username.trim() || !password}
          >
            {isLoading ? (
              <span className="btn-spinner" />
            ) : (
              <>
                <span>Sign In to Dashboard</span>
                <ArrowRight size={17} />
              </>
            )}
          </button>
        </form>

        <div className="login-footer">
          <span>Protected by HMAC-SHA256 authenticated session token</span>
        </div>
      </div>
    </div>
  );
};
