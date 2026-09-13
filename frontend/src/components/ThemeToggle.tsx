import React from 'react';
import { Sun, Moon, Monitor } from 'lucide-react';
import type { ThemeMode } from '../types/theme';
import './ThemeToggle.css';

interface ThemeToggleProps {
  theme: ThemeMode;
  onThemeChange: (theme: ThemeMode) => void;
}

export const ThemeToggle: React.FC<ThemeToggleProps> = ({ theme, onThemeChange }) => {
  return (
    <div className="theme-toggle-container">
      <div className="theme-toggle" role="radiogroup" aria-label="Theme selection">
        <button
          type="button"
          className={`theme-toggle-btn ${theme === 'light' ? 'active' : ''}`}
          onClick={() => onThemeChange('light')}
          role="radio"
          aria-checked={theme === 'light'}
          title="Light theme"
          aria-label="Light theme"
        >
          <Sun size={15} />
          <span className="theme-label">Light</span>
        </button>

        <button
          type="button"
          className={`theme-toggle-btn ${theme === 'system' ? 'active' : ''}`}
          onClick={() => onThemeChange('system')}
          role="radio"
          aria-checked={theme === 'system'}
          title="System theme (matches device settings)"
          aria-label="System theme"
        >
          <Monitor size={15} />
          <span className="theme-label">System</span>
        </button>

        <button
          type="button"
          className={`theme-toggle-btn ${theme === 'dark' ? 'active' : ''}`}
          onClick={() => onThemeChange('dark')}
          role="radio"
          aria-checked={theme === 'dark'}
          title="Dark theme"
          aria-label="Dark theme"
        >
          <Moon size={15} />
          <span className="theme-label">Dark</span>
        </button>
      </div>
    </div>
  );
};
