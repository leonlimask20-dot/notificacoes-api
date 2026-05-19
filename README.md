# Notifications API

![CI](https://github.com/leonlimask20-dot/notificacoes-api/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=spring&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Tests](https://img.shields.io/badge/tests-JUnit5%20+%20Mockito-2E7D32)

Multi-channel notifications API with MongoDB for persistence and Redis for
preference caching and rate limiting.

---

## Quick links

| | |
|---|---|
| Swagger UI | `http://localhost:8086/swagger-ui.html` |
| Run with Docker | `docker-compose up --build` |
| Run tests | `mvn test` |

---

## Key skills demonstrated

- **MongoDB** — document storage with flexible fields per notification type
- **Redis** — preference caching with TTL and rate limiting with an atomic counter
- Spring Data MongoDB — MongoRepository with method-name derived queries
- Spring Data Redis — RedisTemplate with JSON serialization
- Per-user rate limiting — a maximum number of sends per minute with atomic INCR + EXPIRE
- Preference caching — Redis lookup (cache hit) with a fallback to defaults (cache miss)
- Unit tests with JUnit 5 and Mockito — MongoDB and Redis fully mocked
- Embedded MongoDB (Flapdoodle) for integration tests with no infrastructure
- Docker Compose with MongoDB, Redis and the API
- CI pipeline with GitHub Actions

---

## Why MongoDB for notifications?

Each notification type can have different fields:

```json
// EMAIL
{ "tipo": "EMAIL", "metadados": { "assunto": "Purchase confirmed", "templateId": "t001" } }

// SMS
{ "tipo": "SMS", "metadados": { "ddd": "92", "operadora": "claro" } }

// PUSH
{ "tipo": "PUSH", "metadados": { "deviceToken": "abc123", "badge": 1 } }
```

In SQL this would require nullable columns or separate tables. In MongoDB, each
document has exactly the fields it needs.

---

## Why Redis for preferences and rate limiting?

**Preferences:** read on every notification send. Redis keeps them in memory
with a TTL — ~10x faster than MongoDB for frequent reads.

**Rate limiting:** uses atomic INCR + EXPIRE. Thread-safe by nature, with no
need for locks.

---

## Tech stack

| Technology | Use |
|---|---|
| MongoDB 7 | Notification persistence |
| Redis 7 | Preference caching + rate limiting |
| Spring Data MongoDB | MongoRepository |
| Spring Data Redis | RedisTemplate |
| Flapdoodle | Embedded MongoDB for tests |
| JUnit 5 + Mockito | Unit tests |
| Swagger UI | Interactive documentation |
| Docker Compose | Local orchestration |

---

## Architecture

```
src/
├── config/
│   └── ConfiguracaoRedis.java      ← JSON serialization in Redis
├── controller/
│   └── ControladorNotificacao.java ← REST endpoints
├── dto/
│   └── NotificacaoDTO.java         ← request, response, preferences
├── enums/
│   ├── TipoNotificacao.java        ← EMAIL, SMS, PUSH, IN_APP
│   └── StatusNotificacao.java      ← PENDENTE, ENVIADA, FALHOU, LIDA
├── excecao/
│   ├── TratadorDeExcecoes.java
│   ├── RateLimitExcedidoException.java
│   └── RecursoNaoEncontradoException.java
├── modelo/
│   ├── Notificacao.java            ← @Document MongoDB
│   └── PreferenciasUsuario.java    ← stored in Redis
├── repositorio/
│   └── NotificacaoRepositorio.java ← MongoRepository
└── servico/
    ├── ServicoNotificacao.java     ← business logic
    └── ServicoRedis.java           ← caching + rate limiting
```

---

## Endpoints

| Method | Route | Description |
|--------|------|-------------|
| POST | `/api/notificacoes` | Send notification |
| GET | `/api/notificacoes/usuario/{id}` | List notifications |
| GET | `/api/notificacoes/usuario/{id}/status/{status}` | Filter by status |
| PATCH | `/api/notificacoes/{id}/lida` | Mark as read |
| GET | `/api/notificacoes/usuario/{id}/resumo` | Summary by status |
| PUT | `/api/notificacoes/usuario/{id}/preferencias` | Save preferences to Redis |

---

## How to run

```bash
docker-compose up --build
```

Open the Swagger UI at `http://localhost:8086/swagger-ui.html`

---

## Tests

```bash
mvn test
```

The tests run without MongoDB or Redis installed:
- MongoDB: replaced by Flapdoodle (embedded in memory)
- Redis: mocked via Mockito

---

## 🤖 Agent Architecture

This project was built and code-reviewed using a **multi-agent
context-optimization workflow**: specialized AI agents each audit a single
slice of the codebase — MongoDB persistence, Redis caching, REST layer, tests —
within a strict context budget. The approach cuts review time and token cost
while keeping full traceability of every finding.

Methodology, agent templates and the full playbook: **[leonlim3.gumroad.com](https://leonlim3.gumroad.com)**

---

## Author

**LNL**
GitHub: [@leonlimask20-dot](https://github.com/leonlimask20-dot)
Email: leonlimask@gmail.com
