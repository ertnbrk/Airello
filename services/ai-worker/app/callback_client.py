"""
Callback Client - Sends results back to Java API

Uses exponential backoff retry logic for reliability.
"""
import logging
from typing import Dict, Any
import httpx
from tenacity import (
    retry,
    stop_after_attempt,
    wait_exponential,
    retry_if_exception_type,
    before_sleep_log
)
from .config import settings

logger = logging.getLogger(__name__)


class CallbackClient:
    """HTTP client for calling Java API callback endpoint"""

    def __init__(self):
        self.callback_url = settings.java_callback_url
        self.worker_token = settings.worker_token
        self.timeout = httpx.Timeout(30.0, connect=10.0)

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((httpx.HTTPError, httpx.ConnectError)),
        before_sleep=before_sleep_log(logger, logging.WARNING)
    )
    def send_callback(self, correlation_id: str, result: Dict[str, Any]) -> bool:
        """
        Send callback to Java API with retry logic.

        Args:
            correlation_id: Correlation ID of the AI request
            result: Result dictionary from LLM processing

        Returns:
            True if callback was successful, False otherwise
        """
        # Build callback payload matching Java DTO
        payload = {
            "correlationId": correlation_id,
            "status": result.get("status", "FAILED"),
            "provider": result.get("provider"),
            "tokensUsed": result.get("tokens_used")
        }

        if result["status"] == "COMPLETED":
            response_data = result.get("result", {})
            payload["epics"] = response_data.get("epics", [])
            payload["issues"] = response_data.get("issues", [])
            payload["sprints"] = response_data.get("sprints", [])
        else:
            payload["errorMessage"] = result.get("error", "Unknown error")

        try:
            logger.info(
                f"Sending callback for {correlation_id} to {self.callback_url}"
            )

            with httpx.Client(timeout=self.timeout) as client:
                response = client.post(
                    self.callback_url,
                    json=payload,
                    headers={
                        "Content-Type": "application/json",
                        "X-Worker-Token": self.worker_token
                    }
                )

            if response.status_code == 204:
                logger.info(f"✅ Callback successful for {correlation_id}")
                return True
            elif response.status_code == 401:
                logger.error(
                    f"❌ Callback authentication failed for {correlation_id}. "
                    "Check WORKER_TOKEN configuration."
                )
                return False
            else:
                logger.error(
                    f"❌ Callback failed for {correlation_id}: "
                    f"HTTP {response.status_code} - {response.text}"
                )
                response.raise_for_status()
                return False

        except httpx.HTTPError as e:
            logger.error(
                f"❌ HTTP error sending callback for {correlation_id}: {str(e)}"
            )
            raise  # Tenacity will retry
        except Exception as e:
            logger.error(
                f"❌ Unexpected error sending callback for {correlation_id}: {str(e)}",
                exc_info=True
            )
            return False
