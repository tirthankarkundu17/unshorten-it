import React from 'react';
import { Sun, Moon, Monitor } from 'lucide-react';
import type { ThemePreference } from '../hooks/useTheme';

interface ThemeSelectorProps {
  currentTheme: ThemePreference;
  onThemeChange: (theme: ThemePreference) => void;
}

export const ThemeSelector: React.FC<ThemeSelectorProps> = ({
  currentTheme,
  onThemeChange,
}) => {
  const options: { value: ThemePreference; label: string; icon: React.ReactNode }[] = [
    {
      value: 'light',
      label: 'Light mode',
      icon: <Sun size={14} />,
    },
    {
      value: 'dark',
      label: 'Dark mode',
      icon: <Moon size={14} />,
    },
    {
      value: 'system',
      label: 'System mode',
      icon: <Monitor size={14} />,
    },
  ];

  return (
    <div
      className="theme-selector-container"
      role="radiogroup"
      aria-label="Color theme switcher"
      title={`Theme: ${currentTheme}`}
    >
      {options.map((opt) => {
        const isActive = currentTheme === opt.value;
        return (
          <button
            key={opt.value}
            type="button"
            className={`theme-selector-btn ${isActive ? 'active' : ''}`}
            onClick={() => onThemeChange(opt.value)}
            title={opt.label}
            aria-checked={isActive}
            role="radio"
          >
            {opt.icon}
          </button>
        );
      })}
    </div>
  );
};
