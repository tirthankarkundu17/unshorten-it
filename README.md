# unshorten-it

**unshorten-it** is a simple and fast Python FastAPI application that takes a shortened URL as input and follows its redirect chain. It returns a JSON object containing the original URL, the final destination URL, all intermediate redirects, and the total response time.

## Features

- **Backend**: Fast unshortener by following redirect chains (e.g., bit.ly, t.co, youtu.be, etc.). Built with [FastAPI](https://fastapi.tiangolo.com/) and [HTTPX](https://www.python-httpx.org/).
- **Frontend**: A beautiful, modern, and highly responsive React interface designed with Vanilla CSS (Glassmorphism, dark mode, rich micro-animations).
- Includes robust error handling and input validation across the stack.
- Organized project structure adhering to modern best practices.
- Managed by [`uv`](https://github.com/astral-sh/uv) and `npm`.

## Requirements

- Python >= 3.12 (Backend)
- Node.js >= 18 (Frontend)
- [uv](https://github.com/astral-sh/uv) (for dependency management)

## Setup and Running

A `Makefile` is provided in the root directory for convenience.

1. **Install dependencies**:
   ```sh
   make install
   ```
   *This uses `uv sync` inside the `backend/` directory.*

2. **Run the development server**:
   ```sh
   make run
   # or
   make dev
   ```
   *The server will start on `http://localhost:8000` with live-reloading enabled.*

3. **Using Docker**:
   To build and run the application via Docker:
   ```sh
   make docker-build
   make docker-run
   ```
   *The server will be exposed at `http://localhost:8000` within the container.*

## API Documentation

Once the server is running, you can access the interactive API documentation (Swagger UI) directly at `http://localhost:8000/docs` or the ReDoc UI at `http://localhost:8000/redoc`.

### 1. Unshorten URL Endpoint

- **Path:** `/api/v1/unshorten`
- **Method:** `POST`
- **Content-Type:** `application/json`

**Request Body Example:**
```json
{
  "url": "https://bit.ly/3xyz123"
}
```

**Response Body Example:**
```json
{
  "original_url": "https://bit.ly/3xyz123",
  "final_url": "https://www.example.com/very/long/destination/path",
  "redirect_chain": [
    "https://bit.ly/3xyz123",
    "https://example.com/intermediate"
  ],
  "response_time_ms": 154.32
}
```

### 2. Health Check

- **Path:** `/health`
- **Method:** `GET`

Returns the health status of the application.

## Project Structure

```text
unshorten-it/
├── Makefile                # Aliases for setup and running the app
├── README.md               # Project documentation
├── backend/                # FastAPI application
│   ├── app/                # Application routes and services
│   ├── pyproject.toml      # Dependency definitions
│   └── Dockerfile          # Container configuration
└── frontend/               # React UI
    ├── src/
    │   ├── App.tsx         # Main UI view and fetch logic
    │   ├── App.css         # Component styling
    │   ├── index.css       # Global design tokens and animations
    │   └── main.tsx        # React entrypoint
    ├── package.json        
    └── vite.config.ts      # Vite bundler config
```