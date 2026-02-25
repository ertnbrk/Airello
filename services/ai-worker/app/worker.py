"""
Worker Main Loop - Consumes jobs from Redis and processes them

Usage:
    python -m app.worker
"""
import logging
import json
import signal
import sys
from typing import Optional
import redis
from .config import settings
from .llm_router import LLMRouter
from .callback_client import CallbackClient

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

logger = logging.getLogger(__name__)


class Worker:
    """AI Worker - Consumes jobs from Redis and processes them"""

    def __init__(self):
        self.redis_client: Optional[redis.Redis] = None
        self.llm_router = LLMRouter()
        self.callback_client = CallbackClient()
        self.running = False
        self.queue_key = settings.ai_queue_key

    def start(self):
        """Start the worker loop"""
        logger.info("🚀 Starting AI Worker...")
        logger.info(f"   Redis URL: {settings.redis_url}")
        logger.info(f"   Queue Key: {self.queue_key}")
        logger.info(f"   LLM Provider: {settings.llm_type}")
        logger.info(f"   Callback URL: {settings.java_callback_url}")

        # Connect to Redis
        try:
            self.redis_client = redis.from_url(
                settings.redis_url,
                decode_responses=True,
                socket_connect_timeout=5,
                socket_keepalive=True
            )
            self.redis_client.ping()
            logger.info("✅ Connected to Redis")
        except Exception as e:
            logger.error(f"❌ Failed to connect to Redis: {e}")
            sys.exit(1)

        # Set up signal handlers
        signal.signal(signal.SIGINT, self._handle_shutdown)
        signal.signal(signal.SIGTERM, self._handle_shutdown)

        self.running = True
        logger.info("✅ Worker is ready and listening for jobs...")

        # Main loop
        while self.running:
            try:
                self._process_next_job()
            except KeyboardInterrupt:
                logger.info("Received keyboard interrupt, shutting down...")
                break
            except Exception as e:
                logger.error(f"Unexpected error in worker loop: {e}", exc_info=True)
                # Continue running despite errors

        logger.info("Worker shutdown complete")

    def _process_next_job(self):
        """Fetch and process the next job from Redis"""
        try:
            # BRPOP blocks until a job is available (or timeout)
            result = self.redis_client.brpop(
                self.queue_key,
                timeout=settings.worker_poll_timeout
            )

            if result is None:
                # Timeout - no jobs available
                return

            _, job_json = result
            job = json.loads(job_json)

            correlation_id = job.get("correlationId", "unknown")
            request_type = job.get("requestType", "unknown")

            logger.info(
                f"📥 Received job: {correlation_id} (type: {request_type})"
            )

            # Process job with LLM
            result = self.llm_router.process(job)

            # Send callback to Java API
            success = self.callback_client.send_callback(correlation_id, result)

            if success:
                logger.info(f"✅ Job {correlation_id} completed successfully")
            else:
                logger.error(f"❌ Failed to send callback for job {correlation_id}")

        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in job: {e}")
        except redis.RedisError as e:
            logger.error(f"Redis error: {e}")
            # Sleep briefly before retrying
            import time
            time.sleep(5)
        except Exception as e:
            logger.error(f"Error processing job: {e}", exc_info=True)

    def _handle_shutdown(self, signum, frame):
        """Handle graceful shutdown"""
        logger.info(f"Received signal {signum}, initiating graceful shutdown...")
        self.running = False


def main():
    """Main entry point"""
    worker = Worker()
    worker.start()


if __name__ == "__main__":
    main()
