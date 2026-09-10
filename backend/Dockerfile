# Use Python 3.12 slim image as the base
FROM python:3.12-slim

# Copy uv executable from the official image
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

# Set working directory in the container
WORKDIR /app

# Enable bytecode compilation
ENV UV_COMPILE_BYTECODE=1

# Copy dependency definition files first for better caching
COPY pyproject.toml uv.lock ./

# Install dependencies in a virtual environment
RUN uv sync --frozen --no-dev --no-install-project

# Copy the rest of the application code
COPY . .

# Install the project itself
RUN uv sync --frozen --no-dev

# Add the virtual environment to the PATH so commands execute inside it
ENV PATH="/app/.venv/bin:$PATH"

# Set environment variables
# Network & Rate Limiting defaults
ENV REQUEST_TIMEOUT=10.0
ARG TRUST_PROXIES="false"
ENV TRUST_PROXIES=$TRUST_PROXIES
ARG RATE_LIMIT_ENABLED="true"
ENV RATE_LIMIT_ENABLED=$RATE_LIMIT_ENABLED
ARG RATE_LIMIT_REQUESTS="60"
ENV RATE_LIMIT_REQUESTS=$RATE_LIMIT_REQUESTS
ARG RATE_LIMIT_WINDOW="60"
ENV RATE_LIMIT_WINDOW=$RATE_LIMIT_WINDOW

# App config
ARG ALLOW_ORIGINS=""    
ENV ALLOW_ORIGINS=$ALLOW_ORIGINS
ARG APP_VERSION="unknown"
ENV APP_VERSION=$APP_VERSION

# Database & Cache settings
ARG MONGODB_URI="mongodb://localhost:27017"
ENV MONGODB_URI=$MONGODB_URI
ARG MONGODB_DATABASE="unshorten_it"
ENV MONGODB_DATABASE=$MONGODB_DATABASE
ARG REDIS_URL=""
ENV REDIS_URL=$REDIS_URL
ARG DISKCACHE_DIR="/tmp/cacheservice"
ENV DISKCACHE_DIR=$DISKCACHE_DIR

# Security Feeds
ARG URLHAUS_FEED_ENABLED="true"
ENV URLHAUS_FEED_ENABLED=$URLHAUS_FEED_ENABLED
ARG URLHAUS_BACKFILL="false"
ENV URLHAUS_BACKFILL=$URLHAUS_BACKFILL

# Default port if PORT is not set by the container runtime (e.g. Cloud Run provides PORT=8080)
ENV PORT=8000
EXPOSE 8000

# Production CMD using uvicorn:
# - 'exec' replaces the shell with uvicorn so it runs as PID 1 (graceful SIGTERM shutdown)
# - Dynamically respects $PORT provided by Cloud Run / container runtime (fallback to 8000)
# - --workers 1 prevents duplicate background tasks (URLhaus sync) per container instance
# - --proxy-headers & --forwarded-allow-ips='*' trust Cloud Run's reverse proxy for client IP resolution
# - --timeout-keep-alive 65 prevents intermittent 502 errors behind GCP load balancers
CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000} --workers 1 --proxy-headers --forwarded-allow-ips='*' --timeout-keep-alive 65"]
