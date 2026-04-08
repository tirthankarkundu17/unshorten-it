import { useState, useEffect } from 'react';
import { Search, Link as LinkIcon, ExternalLink, Clock, AlertCircle, Smartphone, ShieldAlert, History, Trash2, ArrowLeft, CornerDownRight, QrCode, X, RefreshCw } from 'lucide-react';
import { Scanner } from '@yudiel/react-qr-scanner';
import { useRegisterSW } from 'virtual:pwa-register/react';
import './App.css';

interface PagePreview {
  title?: string;
  description?: string;
  image_url?: string;
}

interface SecurityCheck {
  is_safe: boolean;
  threat_type: string | null;
}

interface UnshortenResponse {
  original_url: string;
  final_url: string;
  cleaned_url: string;
  redirect_chain: string[];
  response_time_ms: number;
  cached: boolean;
  preview?: PagePreview;
  security?: SecurityCheck;
}

interface HistoryItem extends UnshortenResponse {
  timestamp: number;
}

interface ErrorResponse {
  error: {
    message: string;
  };
}

function App() {
  const {
    needRefresh: [needRefresh, setNeedRefresh],
    updateServiceWorker,
  } = useRegisterSW({
    onRegistered(r) {
      console.log('SW Registered: ' + r);
    },
    onRegisterError(error) {
      console.log('SW registration error', error);
    },
  });

  const [url, setUrl] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<UnshortenResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>(() => {
    try {
      const saved = localStorage.getItem('unshorten_history');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [showHistory, setShowHistory] = useState(false);
  const [showScanner, setShowScanner] = useState(false);
  const [scannerError, setScannerError] = useState<string | null>(null);
  useEffect(() => {
    localStorage.setItem('unshorten_history', JSON.stringify(history));
  }, [history]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!url.trim()) return;

    // Build absolute URL if user omits schema
    let urlToSubmit = url.trim();
    if (!/^https?:\/\//i.test(urlToSubmit)) {
      urlToSubmit = `https://${urlToSubmit}`;
    }

    setIsLoading(true);
    setError(null);
    setResult(null);

    try {
      // Prioritize runtime injected env var, fallback to Vite env var, then default.
      let apiBaseUrl = (window as any)._env_?.API_BASE_URL || import.meta.env.API_BASE_URL;

      // Clean up any stray string quotes that Docker might have injected
      if (typeof apiBaseUrl === 'string') {
        apiBaseUrl = apiBaseUrl.replace(/^["']|["']$/g, '');
      }

      const response = await fetch(`${apiBaseUrl}/api/v1/unshorten`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-App-Platform': 'web'
        },
        body: JSON.stringify({ url: urlToSubmit }),
      });

      if (!response.ok) {
        const errorData = (await response.json()) as ErrorResponse;
        throw new Error(errorData.error?.message || 'Failed to unshorten URL');
      }

      const data = (await response.json()) as UnshortenResponse;
      setResult(data);

      setHistory(prev => {
        const newHistory = [{ ...data, timestamp: Date.now() }, ...prev.filter(h => h.original_url !== data.original_url)];
        return newHistory.slice(0, 20); // Keep last 20
      });
    } catch (err: any) {
      setError(err.message || 'An unexpected error occurred.');
    } finally {
      setIsLoading(false);
    }
  };

  const loadFromHistory = (item: HistoryItem) => {
    setUrl(item.original_url);
    setResult({ ...item });
    setError(null);
    setShowHistory(false);
  };

  return (
    <div className="app-container">
      {needRefresh && (
        <div className="glass-panel update-toast animate-slide-up">
          <div className="update-toast-content">
            <RefreshCw size={20} className="update-icon" />
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>Update Available!</span>
              <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Click to refresh and get the latest features.</span>
            </div>
          </div>
          <div className="update-toast-actions">
            <button className="clear-btn" onClick={() => setNeedRefresh(false)}>
              Ignore
            </button>
            <button className="btn-primary" onClick={() => updateServiceWorker(true)} style={{ padding: '0.6rem 1.25rem', fontSize: '0.9rem', minWidth: 'auto', borderRadius: '8px' }}>
              Update App
            </button>
          </div>
        </div>
      )}

      <div className="top-bar">
        {history.length > 0 && (
          <button
            className="glass-btn history-toggle-btn"
            onClick={() => setShowHistory(!showHistory)}
          >
            <History size={18} />
            <span>History</span>
          </button>
        )}
      </div>
      <header className="hero animate-slide-up">
        <h1 className="title text-gradient">Unshorten It</h1>
        <p className="subtitle">Melt away the mystery. Discover exactly where any shortened link is taking you before you click.</p>
      </header>

      <main className="main-content">
        {showHistory ? (
          <section className="glass-panel animate-slide-up" style={{ textAlign: 'left', padding: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <History size={26} color="var(--accent-color)" />
                <h2 className="result-title" style={{ margin: 0 }}>Recent History</h2>
              </div>
              <button
                className="clear-btn"
                onClick={() => { setHistory([]); setShowHistory(false); }}
              >
                <Trash2 size={16} />
                <span>Clear All</span>
              </button>
            </div>
            {history.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-secondary)' }}>
                <History size={48} style={{ opacity: 0.2, marginBottom: '1rem' }} />
                <p style={{ fontSize: '1.1rem' }}>No history available yet.</p>
              </div>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {history.map((item, idx) => (
                  <li key={idx} className="history-card" onClick={() => loadFromHistory(item)}>
                    <div className="history-header">
                      <LinkIcon size={18} className="history-icon" />
                      <span>{item.original_url}</span>
                    </div>
                    <div className="history-subheader">
                      <CornerDownRight size={18} className="history-icon" />
                      <span>{item.final_url}</span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
            <div style={{ marginTop: '2.5rem', textAlign: 'center' }}>
              <button className="btn-primary back-btn" onClick={() => setShowHistory(false)}>
                <ArrowLeft size={18} />
                <span>Back to Search</span>
              </button>
            </div>
          </section>
        ) : (
          <>
            <form className="glass-panel search-form animate-slide-up" onSubmit={handleSubmit} style={{ animationDelay: '0.1s' }}>
              <div className="input-wrapper">
                <LinkIcon className="input-icon" size={20} />
                <input
                  type="text"
                  className="glass-input"
                  placeholder="Paste a shortened link here (e.g. bit.ly/example)..."
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  disabled={isLoading}
                />
              </div>
              <div className="action-buttons">
                <button
                  type="button"
                  className="scanner-btn-secondary"
                  onClick={() => setShowScanner(true)}
                  title="Scan QR Code"
                  disabled={isLoading}
                >
                  <QrCode size={20} />
                  <span className="scanner-btn-text">Scan QR</span>
                </button>
                <button type="submit" className="btn-primary" disabled={isLoading || !url.trim()} style={{ flex: 1 }}>
                  {isLoading ? <span className="spinner"></span> : (
                    <>
                      <Search size={20} />
                      <span>Unshorten</span>
                    </>
                  )}
                </button>
              </div>
            </form>

            {showScanner && (
              <div className="glass-panel scanner-panel animate-slide-up" style={{ position: 'relative', marginTop: '1rem', padding: '1.5rem', marginBottom: '2rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                  <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <QrCode size={20} color="var(--accent-color)" /> Scan QR Code
                  </h3>
                  <button type="button" className="clear-btn" onClick={() => { setShowScanner(false); setScannerError(null); }} style={{ padding: '0.5rem' }}>
                    <X size={20} />
                  </button>
                </div>
                {scannerError ? (
                  <div className="error-text" style={{ textAlign: 'center', color: 'var(--error-color)', marginBottom: '1rem', padding: '1rem', background: 'rgba(239, 68, 68, 0.1)', borderRadius: '8px' }}>
                    {scannerError}
                  </div>
                ) : null}
                <div style={{ borderRadius: '12px', overflow: 'hidden', background: '#000', display: 'flex', justifyContent: 'center' }}>
                  <Scanner
                    onScan={(result) => {
                      if (Array.isArray(result) && result.length > 0) {
                        setUrl(result[0].rawValue);
                        setShowScanner(false);
                      }
                    }}
                    onError={(error: any) => {
                      setScannerError(error?.message || 'Failed to start camera.');
                    }}
                  />
                </div>
              </div>
            )}
            {error && (
              <div className="glass-panel error-panel animate-slide-up">
                <AlertCircle size={24} color="var(--error-color)" />
                <p className="error-text">{error}</p>
              </div>
            )}

            {result && (
              <section className="glass-panel result-panel animate-slide-up">
                <h2 className="result-title">Destination Reached</h2>

                {result.security && !result.security.is_safe && (
                  <div className="glass-panel error-panel animate-slide-up" style={{ marginBottom: '1.5rem' }}>
                    <ShieldAlert size={24} color="var(--error-color)" />
                    <div className="error-text">
                      <strong>Security Warning:</strong> This URL has been flagged as {result.security.threat_type?.replace(/_/g, ' ') || 'a threat'}.
                      Proceeding is highly discouraged.
                    </div>
                  </div>
                )}

                <div className="result-card final-destination animate-slide-up">
                  <span className="label text-gradient">Final URL</span>
                  <a href={result.final_url} target="_blank" rel="noopener noreferrer" className="url final-url">
                    {result.final_url}
                    <ExternalLink size={16} />
                  </a>
                </div>

                {result.cleaned_url !== result.final_url && (
                  <div className="result-card cleaned-destination animate-slide-up">
                    <span className="label text-gradient">Cleaned URL (Trackers Removed)</span>
                    <a href={result.cleaned_url} target="_blank" rel="noopener noreferrer" className="url cleaned-url">
                      {result.cleaned_url}
                      <ExternalLink size={16} />
                    </a>
                  </div>
                )}

                {result.preview && (result.preview.title || result.preview.description || result.preview.image_url) && (
                  <div className="result-card page-preview animate-slide-up">
                    <div className="preview-image-container">
                      <img
                        src={result.preview.image_url || '/no-image.png'}
                        alt="Page Preview"
                        className="preview-image"
                      />
                    </div>
                    <div className="preview-content">
                      {result.preview.title && <h3 className="preview-title">{result.preview.title}</h3>}
                      {result.preview.description && <p className="preview-description">{result.preview.description}</p>}
                    </div>
                  </div>
                )}

                <div className="result-stats">
                  <div className="stat">
                    <Clock size={16} className="stat-icon" />
                    <span>Traced in <strong>{result.response_time_ms}ms</strong></span>
                    {result.cached && (
                      <span style={{ marginLeft: '8px', fontSize: '0.75rem', padding: '2px 6px', background: 'rgba(0,0,0,0.1)', borderRadius: '12px' }}>
                        Cached
                      </span>
                    )}
                  </div>
                  <div className="stat">
                    <LinkIcon size={16} className="stat-icon" />
                    <span><strong>{result.redirect_chain.length - 1}</strong> Hops</span>
                  </div>
                </div>

                {result.redirect_chain.length > 1 && (
                  <div className="redirect-chain">
                    <h3 className="chain-title">Redirect Journey</h3>
                    <ol className="chain-list">
                      {result.redirect_chain.map((link, index) => (
                        <li key={index} className="chain-item">
                          <div className="hop-node"></div>
                          <span className="hop-url">{link}</span>
                        </li>
                      ))}
                    </ol>
                  </div>
                )}
              </section>
            )}
          </>
        )}
      </main>

      <footer className="footer animate-slide-up" style={{ animationDelay: '0.2s' }}>
        <div className="app-promotion">
          <p className="promotion-text">Get the Unshorten It experience on your mobile device.</p>
          <a
            href="https://play.google.com/store/apps/details?id=in.bitmaskers.unshortenit"
            target="_blank"
            rel="noopener noreferrer"
            className="play-store-btn"
          >
            <Smartphone size={20} />
            <div className="btn-text">
              <span className="btn-label">GET IT ON</span>
              <span className="btn-title">Google Play</span>
            </div>
          </a>
        </div>
        <p>Built with FastApi & React. Transparent & Fast.</p>
        <p style={{ marginTop: '0.5rem' }}>
          Made with love by <a href="http://github.com/tirthankarkundu17/" target="_blank" rel="noopener noreferrer" className="author-link">Tirthankar Kundu</a>
          {' '}&bull;{' '}
          <a href="https://bitmaskers.in" target="_blank" rel="noopener noreferrer" className="author-link">bitmaskers.in</a>
        </p>
      </footer>
    </div>
  );
}

export default App;
