.PHONY: install run dev clean docker-build docker-run

# Run the FastAPI server in development mode with live reloading
run-backend:
	cd backend && uv run fastapi dev app/main.py --port 8000

# Install frontend dependencies
install-frontend:
	cd frontend && npm install

# Run the frontend application
run-frontend:
	cd frontend && npm run dev

# Alias for run (backend)
dev: run


# Install or sync dependencies using uv
install:
	cd backend && uv sync

# Clean Python cache directories (Windows compatible)
clean:
	FOR /d /r . %d in (__pycache__) DO @IF EXIST "%d" rd /s /q "%d"

# Build the Docker image
docker-build-backend:
	cd backend && docker build -t unshorten-it-backend .

# Run the Docker container
docker-run-backend:
	docker run -p 8000:8000 --rm unshorten-it-backend

# Build the Frontend Docker image
docker-build-frontend:
	cd frontend && docker build -t unshorten-it-frontend .

# Run the Frontend Docker container
docker-run-frontend:
	docker run -p 8080:80 --rm unshorten-it-frontend
