"""
AI Worker - FastAPI Application

Health check endpoints for monitoring.
The actual worker loop runs in worker.py as a background process.
"""
import logging
from fastapi import FastAPI
from fastapi.responses import JSONResponse

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Worker",
    description="Async AI processing worker for Planmate",
    version="1.0.0"
)


@app.get("/healthz")
async def healthz():
    """Liveness probe - is the service running?"""
    return JSONResponse({"status": "healthy"})


@app.get("/readyz")
async def readyz():
    """Readiness probe - is the service ready to accept work?"""
    # TODO: Check Redis connectivity
    return JSONResponse({"status": "ready"})


@app.get("/")
async def root():
    return {
        "service": "ai-worker",
        "version": "1.0.0",
        "description": "AI processing worker consuming jobs from Redis"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
