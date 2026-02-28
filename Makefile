.PHONY: install run dev clean

# Run the FastAPI server in development mode with live reloading
run:
	cd backend && uv run uvicorn app.main:app --reload

# Alias for run
dev: run

# Install or sync dependencies using uv
install:
	cd backend && uv sync

# Clean Python cache directories (Windows compatible)
clean:
	FOR /d /r . %d in (__pycache__) DO @IF EXIST "%d" rd /s /q "%d"
