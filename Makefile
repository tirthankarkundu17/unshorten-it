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
run: run-backend run-frontend
	echo "Backend and Frontend are running"


# Install or sync dependencies using uv
install:
	cd backend && uv sync

# Clean Python cache directories (Windows compatible)
clean:
	FOR /d /r . %d in (__pycache__) DO @IF EXIST "%d" rd /s /q "%d"

# Docker Image tags for multi-architecture builds
DOCKER_USER ?= your_docker_username
BACKEND_IMAGE ?= $(DOCKER_USER)/unshortenit-backend:latest
FRONTEND_IMAGE ?= $(DOCKER_USER)/unshortenit-frontend:latest

# Build the Docker image locally
docker-build-backend:
	cd backend && docker build --build-arg ALLOW_ORIGINS="$(ALLOW_ORIGINS)" --build-arg APP_VERSION="$(APP_VERSION)" -t unshorten-it-backend .

# Run the Docker container locally
docker-run-backend:
	docker run -p 8000:8000 --rm unshorten-it-backend

# Build the Frontend Docker image locally
docker-build-frontend:
	cd frontend && docker build -t unshorten-it-frontend .

# Run the Frontend Docker container locally
docker-run-frontend:
	docker run -p 8080:80 --rm unshorten-it-frontend

# Set up docker buildx builder for multi-arch builds
docker-setup-buildx:
	docker buildx create --use --name unshorten-builder || docker buildx use unshorten-builder

# Multi-arch build and push for backend
docker-build-push-backend: docker-setup-buildx
	cd backend && docker buildx build --platform linux/amd64,linux/arm64 --build-arg ALLOW_ORIGINS="$(ALLOW_ORIGINS)" --build-arg APP_VERSION="$(APP_VERSION)" -t $(BACKEND_IMAGE) --push .

# Multi-arch build and push for frontend
docker-build-push-frontend: docker-setup-buildx
	cd frontend && docker buildx build --platform linux/amd64,linux/arm64 -t $(FRONTEND_IMAGE) --push .

# Run the full stack using Docker Compose
docker-up:
	docker compose up --build -d

# Stop and tear down the Docker Compose stack
docker-down:
	docker compose down

