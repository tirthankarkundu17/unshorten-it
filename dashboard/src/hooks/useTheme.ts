import { useState, useEffect, useCallback } from 'react';

export type ThemePreference = 'light' | 'dark' | 'system';
export type ActiveTheme = 'light' | 'dark';

const THEME_STORAGE_KEY = 'unshorten_admin_theme';

export function useTheme() {
  const [themePreference, setThemePreferenceState] = useState<ThemePreference>(() => {
    const saved = localStorage.getItem(THEME_STORAGE_KEY);
    if (saved === 'light' || saved === 'dark' || saved === 'system') {
      return saved;
    }
    return 'system';
  });

  const [activeTheme, setActiveTheme] = useState<ActiveTheme>('dark');

  const getSystemTheme = (): ActiveTheme => {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
  };

  const applyTheme = useCallback((pref: ThemePreference) => {
    const effective = pref === 'system' ? getSystemTheme() : pref;
    setActiveTheme(effective);
    document.documentElement.setAttribute('data-theme', effective);

    // Update browser theme-color meta tag
    const metaTheme = document.querySelector('meta[name="theme-color"]');
    if (metaTheme) {
      metaTheme.setAttribute('content', effective === 'dark' ? '#07090e' : '#f8fafc');
    }
  }, []);

  const setTheme = (pref: ThemePreference) => {
    setThemePreferenceState(pref);
    localStorage.setItem(THEME_STORAGE_KEY, pref);
    applyTheme(pref);
  };

  useEffect(() => {
    applyTheme(themePreference);

    if (themePreference === 'system' && window.matchMedia) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const listener = () => {
        applyTheme('system');
      };
      mediaQuery.addEventListener('change', listener);
      return () => mediaQuery.removeEventListener('change', listener);
    }
  }, [themePreference, applyTheme]);

  return {
    themePreference,
    activeTheme,
    setTheme,
  };
}
