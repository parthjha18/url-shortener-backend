import { useState, useEffect } from 'react'
import { shortenUrl, getStats } from './api'
import styles from './App.module.css'

const HISTORY_KEY = 'url-shortener-history'

export default function App() {
  const [tab, setTab] = useState('shorten')

  // ── Shorten state ──────────────────────────────────────────────────
  const [url, setUrl] = useState('')
  const [result, setResult] = useState(null)   // { shortUrl }
  const [shortenErr, setShortenErr] = useState('')
  const [shortenBusy, setShortenBusy] = useState(false)
  const [copied, setCopied] = useState(false)
  const [history, setHistory] = useState(() => {
    try { return JSON.parse(localStorage.getItem(HISTORY_KEY)) ?? [] }
    catch { return [] }
  })

  // ── Stats state ────────────────────────────────────────────────────
  const [code, setCode] = useState('')
  const [stats, setStats] = useState(null)
  const [statsErr, setStatsErr] = useState('')
  const [statsBusy, setStatsBusy] = useState(false)

  // persist history
  useEffect(() => {
    try { localStorage.setItem(HISTORY_KEY, JSON.stringify(history)) }
    catch { /* storage full — ignore */ }
  }, [history])

  // ── Handlers ───────────────────────────────────────────────────────
  async function handleShorten(e) {
    e.preventDefault()
    setShortenErr('')
    setResult(null)
    setShortenBusy(true)
    try {
      const data = await shortenUrl(url)
      setResult(data)
      setHistory(prev => {
        const next = [{ original: url, short: data.shortUrl, ts: Date.now() }, ...prev]
        return next.slice(0, 6)
      })
    } catch (err) {
      setShortenErr(err.message)
    } finally {
      setShortenBusy(false)
    }
  }

  function handleCopy() {
    navigator.clipboard.writeText(result.shortUrl).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  async function handleStats(e) {
    e.preventDefault()
    setStatsErr('')
    setStats(null)
    setStatsBusy(true)
    try {
      const data = await getStats(code)
      setStats(data)
    } catch (err) {
      setStatsErr(err.message)
    } finally {
      setStatsBusy(false)
    }
  }

  function useHistoryItem(item) {
    setTab('stats')
    setCode(item.short)
    setStats(null)
    setStatsErr('')
  }

  // ── Render ─────────────────────────────────────────────────────────
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.logo}>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
          </svg>
          <span>Shortly</span>
        </div>
        <p className={styles.tagline}>Shorten links. Track clicks.</p>
      </header>

      <main className={styles.main}>
        <div className={styles.tabs}>
          <button
            className={`${styles.tab} ${tab === 'shorten' ? styles.tabActive : ''}`}
            onClick={() => setTab('shorten')}
          >
            Shorten
          </button>
          <button
            className={`${styles.tab} ${tab === 'stats' ? styles.tabActive : ''}`}
            onClick={() => setTab('stats')}
          >
            Stats
          </button>
        </div>

        {/* ── Shorten tab ──────────────────────────────────────────── */}
        {tab === 'shorten' && (
          <div className={styles.card}>
            <form onSubmit={handleShorten} className={styles.form}>
              <input
                className={styles.input}
                type="url"
                placeholder="https://your-very-long-url.com/..."
                value={url}
                onChange={e => setUrl(e.target.value)}
                required
              />
              <button className={styles.btn} type="submit" disabled={shortenBusy}>
                {shortenBusy ? 'Shortening…' : 'Shorten'}
              </button>
            </form>

            {shortenErr && <p className={styles.error}>{shortenErr}</p>}

            {result && (
              <div className={styles.result}>
                <a
                  href={result.shortUrl}
                  target="_blank"
                  rel="noreferrer"
                  className={styles.shortUrl}
                >
                  {result.shortUrl}
                </a>
                <button
                  className={`${styles.copyBtn} ${copied ? styles.copyBtnDone : ''}`}
                  onClick={handleCopy}
                >
                  {copied ? 'Copied!' : 'Copy'}
                </button>
              </div>
            )}

            {history.length > 0 && (
              <div className={styles.history}>
                <h3 className={styles.historyHeading}>Recent</h3>
                {history.map((item, i) => (
                  <div key={i} className={styles.historyRow}>
                    <span className={styles.historyOriginal}>{item.original}</span>
                    <button
                      className={styles.historyShort}
                      onClick={() => useHistoryItem(item)}
                      title="View stats"
                    >
                      {item.short.replace(/^https?:\/\//, '')}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* ── Stats tab ────────────────────────────────────────────── */}
        {tab === 'stats' && (
          <div className={styles.card}>
            <form onSubmit={handleStats} className={styles.form}>
              <input
                className={styles.input}
                placeholder="Short code or full short URL"
                value={code}
                onChange={e => setCode(e.target.value)}
                required
              />
              <button className={styles.btn} type="submit" disabled={statsBusy}>
                {statsBusy ? 'Loading…' : 'Get Stats'}
              </button>
            </form>

            {statsErr && <p className={styles.error}>{statsErr}</p>}

            {stats && (
              <div className={styles.statsGrid}>
                <div className={`${styles.statCell} ${styles.statCellWide}`}>
                  <span className={styles.statLabel}>Original URL</span>
                  <a
                    href={stats.originalUrl}
                    target="_blank"
                    rel="noreferrer"
                    className={styles.statValue}
                  >
                    {stats.originalUrl}
                  </a>
                </div>
                <div className={styles.statCell}>
                  <span className={styles.statLabel}>Short Code</span>
                  <span className={styles.statValue}>{stats.shortCode}</span>
                </div>
                <div className={`${styles.statCell} ${styles.statHighlight}`}>
                  <span className={styles.statLabel}>Total Clicks</span>
                  <span className={styles.statBigNumber}>{stats.clickCount.toLocaleString()}</span>
                </div>
                <div className={styles.statCell}>
                  <span className={styles.statLabel}>Created</span>
                  <span className={styles.statValue}>
                    {new Date(stats.createdAt).toLocaleDateString('en-IN', { dateStyle: 'medium' })}
                  </span>
                </div>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
