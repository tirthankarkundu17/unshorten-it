import { useState } from 'react';
import { Search, Link as LinkIcon, ExternalLink, Clock, AlertCircle, Smartphone } from 'lucide-react';
import './App.css';

interface UnshortenResponse {
  original_url: string;
  final_url: string;
  redirect_chain: string[];
  response_time_ms: number;
  cached: boolean;
}

interface ErrorResponse {
  error: {
    message: string;
  };
}

function App() {
  const [url, setUrl] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<UnshortenResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

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
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: urlToSubmit }),
      });

      if (!response.ok) {
        const errorData = (await response.json()) as ErrorResponse;
        throw new Error(errorData.error?.message || 'Failed to unshorten URL');
      }

      const data = (await response.json()) as UnshortenResponse;
      setResult(data);
    } catch (err: any) {
      setError(err.message || 'An unexpected error occurred.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app-container">
      <header className="hero animate-slide-up">
        <h1 className="title text-gradient">Unshorten It</h1>
        <p className="subtitle">Melt away the mystery. Discover exactly where any shortened link is taking you before you click.</p>
      </header>

      <main className="main-content">
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
          <button type="submit" className="btn-primary" disabled={isLoading || !url.trim()}>
            {isLoading ? <span className="spinner"></span> : (
              <>
                <Search size={20} />
                <span>Unshorten</span>
              </>
            )}
          </button>
        </form>

        {error && (
          <div className="glass-panel error-panel animate-slide-up">
            <AlertCircle size={24} color="var(--error-color)" />
            <p className="error-text">{error}</p>
          </div>
        )}

        {result && (
          <section className="glass-panel result-panel animate-slide-up">
            <h2 className="result-title">Destination Reached</h2>

            <div className="result-card final-destination">
              <span className="label text-gradient">Final URL</span>
              <a href={result.final_url} target="_blank" rel="noopener noreferrer" className="url final-url">
                {result.final_url}
                <ExternalLink size={16} />
              </a>
            </div>

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
