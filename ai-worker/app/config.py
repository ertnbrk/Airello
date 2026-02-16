"""Configuration management using Pydantic Settings"""
from pydantic_settings import BaseSettings
from typing import Literal


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    # Redis Configuration
    redis_url: str = "redis://localhost:6379"
    ai_queue_key: str = "ai:jobs"

    # Java API Callback
    java_callback_url: str = "http://localhost:8080/v1/ai/callback"
    worker_token: str = "changeme-worker-secret-token"

    # LLM Provider Configuration
    llm_type: Literal["openai", "ollama", "mock"] = "mock"

    # OpenAI Configuration
    openai_api_key: str | None = None
    openai_model: str = "gpt-4o-mini"
    openai_base_url: str = "https://api.openai.com/v1"

    # Ollama Configuration
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama2"

    # Worker Configuration
    worker_poll_timeout: int = 10  # seconds to wait on BRPOP
    worker_retry_max: int = 3
    worker_retry_delay: int = 5  # seconds

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
