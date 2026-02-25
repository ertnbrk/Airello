# PlanMate

AI-powered agile project management platform with real-time collaboration.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       Load Balancer                          │
└────────────┬────────────────────────────┬───────────────────┘
             │                            │
       ┌─────▼──────┐              ┌─────▼──────┐
       │   API      │              │   API      │
       │  (Java)    │              │  (Java)    │
       └─────┬──────┘              └─────┬──────┘
             │                            │
             └────────────┬───────────────┘
                          │
        ┌─────────────────┼─────────────────────────┐
        │                 │                         │
   ┌────▼─────┐    ┌─────▼──────┐   ┌─────▼──────┐
   │PostgreSQL│    │   Redis    │   │ AI Worker  │
   │+pgvector │    │  (Session) │   │  (Python)  │
   └──────────┘    └────────────┘   └────────────┘
```

## Tech Stack

**Backend**
- Java 21 + Spring Boot 3.2
- PostgreSQL 16 + pgvector
- Redis Stack
- JWT Authentication

**AI Worker**
- Python 3.11+
- LangChain
- OpenAI/Ollama support

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local development)

### Run with Docker

```bash
# Copy environment template
cp .env.example .env

# Edit .env and set required variables:
# - JWT_SECRET (change from default)
# - WORKER_TOKEN (change from default)
# - OPENAI_API_KEY (optional, for AI features)

# Start all services
docker compose up -d

# Check health
curl http://localhost:8080/actuator/health
```

**Services:**
- API: http://localhost:8080
- PostgreSQL: localhost:5433
- Redis: localhost:6379
- RedisInsight: http://localhost:8001

### Local Development

```bash
# Start infrastructure
docker compose up -d postgres redis

# Run API
cd services/api
./gradlew bootRun

# Run AI worker (separate terminal)
cd services/ai-worker
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python -m app.worker
```

## Environment Variables

Required variables in `.env`:

```bash
# Security (MUST CHANGE)
JWT_SECRET=your-secret-key-here
WORKER_TOKEN=your-worker-token-here

# Database
POSTGRES_DB=planmate
POSTGRES_USER=planmate
POSTGRES_PASSWORD=your-db-password

# Features
AI_ENABLED=true
REDIS_ENABLED=true

# AI Configuration
LLM_TYPE=mock  # or "openai"
OPENAI_API_KEY=sk-...  # if using OpenAI
```

## Project Structure

```
/
├── services/
│   ├── api/              # Spring Boot backend
│   │   ├── src/
│   │   ├── build.gradle
│   │   └── Dockerfile
│   └── ai-worker/        # Python AI service
│       ├── app/
│       ├── requirements.txt
│       └── Dockerfile
├── infra/                # Terraform configs
├── docker-compose.yml
├── .env.example
└── README.md
```

## API Overview

**Authentication**
```
POST   /auth/demo           # Anonymous demo session
POST   /auth/register       # User registration
POST   /auth/login          # Login
```

**Projects & Boards**
```
GET    /v1/projects                      # List projects
POST   /v1/projects                      # Create project
GET    /v1/projects/{id}/board           # Get board
POST   /v1/projects/{id}/board/columns   # Add column
PUT    /v1/projects/{id}/board/issues/{id}/move  # Move issue
```

**Chat & AI**
```
GET    /v1/projects/{id}/chat/threads/{id}/messages
POST   /v1/projects/{id}/chat/threads/{id}/messages
POST   /v1/projects/{id}/diagrams/generate
```

## Features

- **Dynamic Boards** - Custom columns, drag & drop
- **AI Assistant** - OpenAI/Ollama integration with semantic caching
- **Real-Time Sync** - WebSocket collaboration
- **Chat Commands** - Natural language project management
- **Demo Mode** - Try without signup (24h sessions)
- **Transactional Outbox** - Guaranteed event delivery
- **Horizontal Scaling** - Redis sessions, distributed locks

## Development

```bash
# Run tests
cd services/api
./gradlew test

# Build
./gradlew build

# Format code
./gradlew spotlessApply
```

## Deployment

### Docker Production

```bash
docker build -t planmate-api ./services/api
docker build -t planmate-worker ./services/ai-worker

docker run -d \
  -e SPRING_PROFILE=prod \
  -e DB_URL=jdbc:postgresql://... \
  -e JWT_SECRET=$JWT_SECRET \
  planmate-api
```

### Kubernetes

Helm charts available in `services/api/helm/`.

```bash
helm install planmate ./services/api/helm/planmate \
  --set image.tag=latest \
  --set postgresql.enabled=true
```

## Monitoring

- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus
- RedisInsight: http://localhost:8001

## License

MIT License - see [LICENSE](LICENSE)

## Support

- Issues: [GitHub Issues](https://github.com/yourorg/planmate/issues)
- Docs: See `services/api/docs/` for detailed documentation
