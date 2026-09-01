const BASE = import.meta.env.VITE_API_BASE ?? ''

async function request(path, options = {}) {
  const res = await fetch(BASE + path, options)
  const body = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(body.error ?? body.message ?? `Request failed (${res.status})`)
  }
  return body
}

export function shortenUrl(url) {
  return request('/shorten', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ url }),
  })
}

export function getStats(shortCode) {
  // Accept either the bare code ("aB3dF2x") or the full short URL
  const code = shortCode.includes('/')
    ? shortCode.split('/').pop()
    : shortCode
  return request(`/stats/${code}`)
}
