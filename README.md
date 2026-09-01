# Scalable URL Shortener Backend

A production-style URL Shortener backend built with **Java 17, Spring Boot 3, MySQL, and Redis**.
Simulates core functionality of services like Bitly or TinyURL.

## Tech Stack

- Java 17 + Spring Boot 3
- Spring Data JPA (MySQL)
- Spring Cache + Redis (optional — degrades gracefully if Redis is unavailable)
- Bean Validation (JSR-380)
- JUnit 5 + Mockito + MockMvc
- Docker + Docker Compose

## Architecture

```
Controller → Service → Repository → MySQL
                ↑
          Redis cache (URL lookups)
          Redis rate limiter (per-IP fixed window)
```

Layered: controller handles HTTP, service owns business logic, repository owns persistence.
No business logic leaks into the controller layer.

## Features

| Feature | Detail |
|---|---|
| Shorten URLs | POST /shorten — validates URL format, generates 7-char Base62 code |
| Redirect | GET /{shortCode} — 302 redirect, atomically increments click count |
| Analytics | GET /stats/{shortCode} — click count, creation date |
| Redis caching | URL lookups cached with 10-min TTL; falls back to MySQL on Redis outage |
| Atomic click tracking | Single `UPDATE click_count + 1` — no read-modify-write race condition |
| Rate limiting | 60 requests/minute per IP via Redis INCR; returns 429 + Retry-After header; fails open if Redis is down |
| Global error handling | Consistent JSON error envelope (status, message, timestamp) for all error types |
| Input validation | @NotBlank + @URL on request DTO; validation errors return structured 400 |
| Graceful degradation | Redis outage → caching and rate limiting both fail open; service stays up |

## API

### POST /shorten
```
Request:  { "url": "https://example.com/some/long/path" }
Response: { "shortUrl": "http://localhost:8080/aB3dF2x" }   (201 Created)
```

### GET /{shortCode}
Redirects to the original URL (302 Found). Increments click counter atomically.

### GET /stats/{shortCode}
```json
{
  "originalUrl": "https://example.com/some/long/path",
  "shortCode": "aB3dF2x",
  "clickCount": 5,
  "createdAt": "2026-01-01T00:00:00"
}
```

## Running with Docker (recommended)

```bash
docker-compose up --build
```

That's it — MySQL, Redis, and the app start together. The app waits for both
databases to be healthy before starting. No local Java or database installation needed.

## Running locally

Prerequisites: Java 17, MySQL, Redis (optional)

```bash
# Set credentials (never commit these)
export DB_PASSWORD=yourpassword

# Run
./mvnw spring-boot:run
```

Or copy `.env.example` (if provided) to `.env` and fill in values.

## Tests

```bash
./mvnw test
```

Tests use H2 in-memory — no MySQL or Redis required. Coverage includes:

- `UrlServiceTest` — unit tests with Mockito for all service methods
- `ShortCodeGeneratorTest` — collision handling, retry logic, Base62 format
- `UrlControllerTest` — MockMvc tests for all endpoints including validation and 404 paths

## Design decisions

**Atomic click increment** — `resolveAndTrackClick` issues a single
`UPDATE urls SET click_count = click_count + 1 WHERE short_code = ?` rather than
read → increment → save. Under concurrent redirects, the read-modify-write pattern
loses counts; a single UPDATE is atomic at the database level.

**Redis rate limiting** — the fixed-window counter (`INCR rl:{ip}:{minute}`) is
itself atomic, so concurrent requests from the same IP cannot bypass the limit through
a race. The filter fails open (allow) on Redis errors so a cache outage does not
also take down rate limiting.

**302 vs 301** — redirects use 302 (Found) rather than 301 (Moved Permanently).
Browsers cache 301s permanently, which would cause click counts to stop updating
once a browser has cached the redirect. 302 ensures every click is tracked.

**Non-root Docker user** — the runtime image creates a dedicated `appuser` so the
process does not run as root inside the container, following the principle of least privilege.

## Author

Parth — B.E. Information Science and Engineering, CMR Institute of Technology
