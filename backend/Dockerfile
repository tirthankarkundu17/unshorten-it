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
ARG URLHAUS_BACKFILL="false"
ENV URLHAUS_BACKFILL=$URLHAUS_BACKFILL

# Expose the application port
EXPOSE 8000

# Command to run the application using the FastAPI CLI
CMD ["fastapi", "run", "--workers", "3", "app/main.py", "--port", "8000"]
