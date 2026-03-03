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
ENV REQUEST_TIMEOUT=15.0
ARG ALLOW_ORIGINS=""    
ENV ALLOW_ORIGINS=$ALLOW_ORIGINS
ARG APP_VERSION="unknown"
ENV APP_VERSION=$APP_VERSION

# Expose the application port
EXPOSE 8000

# Command to run the application using the FastAPI CLI
CMD ["fastapi", "run", "--workers", "1", "app/main.py", "--port", "8000"]
