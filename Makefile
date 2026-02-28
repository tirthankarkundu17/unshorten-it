.PHONY: install run dev clean docker-build docker-run

# Run the FastAPI server in development mode with live reloading
run:
	cd backend && uv run fastapi dev app/main.py --port 8000

# Alias for run
dev: run

# Install or sync dependencies using uv
install:
	cd backend && uv sync

# Clean Python cache directories (Windows compatible)
clean:
	FOR /d /r . %d in (__pycache__) DO @IF EXIST "%d" rd /s /q "%d"

# Build the Docker image
docker-build:
	cd backend && docker build -t unshorten-it-backend .

# Run the Docker container
docker-run:
	docker run -p 8000:8000 --rm unshorten-it-backend
