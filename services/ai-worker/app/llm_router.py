"""
LLM Router - Routes requests to different LLM providers

Supports:
- OpenAI (GPT-4, GPT-3.5-turbo)
- Ollama (local LLMs)
- Mock (for testing)
"""
import logging
import json
from typing import Dict, Any
from langchain_openai import ChatOpenAI
from langchain_community.llms import Ollama
from langchain.prompts import ChatPromptTemplate
from langchain.output_parsers import PydanticOutputParser
from pydantic import BaseModel, Field
from .config import settings

logger = logging.getLogger(__name__)


# Output Schema for Structured JSON
class AiOutputSchema(BaseModel):
    """Structured output schema for AI responses"""
    summary: str = Field(description="High-level project summary")
    epics: list[Dict[str, Any]] = Field(default_factory=list, description="List of epics")
    issues: list[Dict[str, Any]] = Field(default_factory=list, description="List of issues")
    sprints: list[Dict[str, Any]] = Field(default_factory=list, description="List of sprints")


class LLMRouter:
    """Routes LLM requests to appropriate provider"""

    def __init__(self):
        self.provider = settings.llm_type
        logger.info(f"🤖 LLM Router initialized with provider: {self.provider}")

    def process(self, job: Dict[str, Any]) -> Dict[str, Any]:
        """
        Process a job using the configured LLM provider.

        Args:
            job: Job dictionary with correlationId, prompt, context, etc.

        Returns:
            Dictionary with:
                - status: "COMPLETED" | "FAILED"
                - result: Structured output (if successful)
                - error: Error message (if failed)
                - provider: LLM provider used
                - tokens_used: Token count (if available)
        """
        correlation_id = job.get("correlationId")
        prompt = job.get("prompt", "")
        context = job.get("context", {})
        constraints = job.get("constraints", {})

        logger.info(
            f"Processing job {correlation_id} with {self.provider} "
            f"(prompt length: {len(prompt)} chars)"
        )

        try:
            if self.provider == "openai":
                result = self._process_openai(prompt, context, constraints)
            elif self.provider == "ollama":
                result = self._process_ollama(prompt, context, constraints)
            else:
                result = self._process_mock(prompt, context)

            logger.info(f"✅ Job {correlation_id} completed successfully")
            return {
                "status": "COMPLETED",
                "result": result,
                "provider": self.provider,
                "tokens_used": result.get("tokens_used")
            }

        except Exception as e:
            logger.error(f"❌ Job {correlation_id} failed: {str(e)}", exc_info=True)
            return {
                "status": "FAILED",
                "error": str(e),
                "provider": self.provider
            }

    def _process_openai(
        self, prompt: str, context: Dict[str, Any], constraints: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Process using OpenAI API"""
        if not settings.openai_api_key:
            raise ValueError("OpenAI API key not configured")

        max_tokens = constraints.get("maxTokens", 2000)
        temperature = constraints.get("temperature", 0.7)

        # Initialize OpenAI chat model
        llm = ChatOpenAI(
            model=settings.openai_model,
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url,
            max_tokens=max_tokens,
            temperature=temperature,
        )

        # Build prompt with context
        system_prompt = self._build_system_prompt(context)
        full_prompt = f"{system_prompt}\n\n{prompt}"

        # Invoke LLM
        response = llm.invoke(full_prompt)

        # Parse structured output
        result = self._parse_response(response.content)

        # Estimate tokens (rough estimate: 1 token ≈ 4 characters)
        tokens_used = (len(full_prompt) + len(response.content)) // 4

        result["tokens_used"] = tokens_used
        return result

    def _process_ollama(
        self, prompt: str, context: Dict[str, Any], constraints: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Process using Ollama (local LLM)"""
        temperature = constraints.get("temperature", 0.7)

        # Initialize Ollama
        llm = Ollama(
            base_url=settings.ollama_base_url,
            model=settings.ollama_model,
            temperature=temperature,
        )

        # Build prompt
        system_prompt = self._build_system_prompt(context)
        full_prompt = f"{system_prompt}\n\n{prompt}"

        # Invoke LLM
        response = llm.invoke(full_prompt)

        # Parse structured output
        result = self._parse_response(response)

        # Ollama doesn't provide token counts
        result["tokens_used"] = None
        return result

    def _process_mock(self, prompt: str, context: Dict[str, Any]) -> Dict[str, Any]:
        """Mock LLM for testing (no API calls)"""
        project_name = context.get("projectName", "Sample Project")

        return {
            "summary": f"AI-generated plan for {project_name}",
            "epics": [
                {
                    "title": "User Authentication",
                    "description": "Implement user registration, login, and JWT auth",
                    "priority": "HIGH"
                },
                {
                    "title": "Core Features",
                    "description": "Build main application features",
                    "priority": "MEDIUM"
                }
            ],
            "issues": [
                {
                    "epicTitle": "User Authentication",
                    "type": "STORY",
                    "title": "User Registration",
                    "description": "As a user, I want to register an account",
                    "priority": "HIGH",
                    "storyPoints": 5,
                    "labels": ["backend", "auth"],
                    "dependsOn": []
                },
                {
                    "epicTitle": "User Authentication",
                    "type": "STORY",
                    "title": "User Login",
                    "description": "As a user, I want to log in with email/password",
                    "priority": "HIGH",
                    "storyPoints": 3,
                    "labels": ["backend", "auth"],
                    "dependsOn": ["User Registration"]
                }
            ],
            "sprints": [
                {
                    "name": "Sprint 1: Foundation",
                    "goal": "Set up authentication and core infrastructure",
                    "issueKeys": ["User Registration", "User Login"]
                }
            ],
            "tokens_used": 0
        }

    def _build_system_prompt(self, context: Dict[str, Any]) -> str:
        """Build system prompt with context"""
        project_name = context.get("projectName", "the project")
        project_description = context.get("projectDescription", "")

        return f"""You are an expert project planner and software architect.

Project: {project_name}
Description: {project_description}

Your task is to generate a comprehensive project plan in JSON format.

Output MUST be valid JSON with this structure:
{{
  "summary": "High-level project summary",
  "epics": [
    {{"title": "...", "description": "...", "priority": "HIGH|MEDIUM|LOW"}}
  ],
  "issues": [
    {{
      "epicTitle": "...",
      "type": "STORY|TASK|BUG",
      "title": "...",
      "description": "...",
      "priority": "HIGH|MEDIUM|LOW",
      "storyPoints": 1-13,
      "labels": ["..."],
      "dependsOn": ["..."]
    }}
  ],
  "sprints": [
    {{"name": "Sprint 1", "goal": "...", "issueKeys": ["..."]}}
  ]
}}

Rules:
- Create 2-4 epics
- Create 8-15 issues total
- Issues must reference epicTitle
- Set realistic story points (1, 2, 3, 5, 8, 13)
- Use relevant labels
- Define dependencies between issues
- Organize issues into 2-3 sprints
"""

    def _parse_response(self, response_text: str) -> Dict[str, Any]:
        """Parse LLM response into structured format"""
        # Try to extract JSON from response
        try:
            # If response is already valid JSON
            return json.loads(response_text)
        except json.JSONDecodeError:
            # Try to find JSON block in markdown code fence
            if "```json" in response_text:
                start = response_text.find("```json") + 7
                end = response_text.find("```", start)
                json_text = response_text[start:end].strip()
                return json.loads(json_text)
            elif "```" in response_text:
                start = response_text.find("```") + 3
                end = response_text.find("```", start)
                json_text = response_text[start:end].strip()
                return json.loads(json_text)
            else:
                # Fallback: create minimal valid structure
                logger.warning("Could not parse JSON from LLM response, using fallback")
                return {
                    "summary": response_text[:500],
                    "epics": [],
                    "issues": [],
                    "sprints": []
                }
