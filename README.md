# Notificações API

![CI](https://github.com/leonlimask20-dot/notificacoes-api/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=spring&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Testes](https://img.shields.io/badge/testes-JUnit5%20+%20Mockito-2E7D32)

API de notificações multicanal com MongoDB para persistência e Redis para cache de preferências e rate limiting.

---

## Links rápidos

| | |
|---|---|
| Swagger UI | `http://localhost:8086/swagger-ui.html` |
| Rodar com Docker | `docker-compose up --build` |
| Rodar testes | `mvn test` |

---

## Principais competências demonstradas

- **MongoDB** — armazenamento de documentos com campos flexíveis por tipo de notificação
- **Redis** — cache de preferências com TTL e rate limiting com contador atômico
- Spring Data MongoDB — MongoRepository com queries derivadas por nome de método
- Spring Data Redis — RedisTemplate com serialização JSON
- Rate limiting por usuário — máximo de envios por minuto com INCR + EXPIRE atômicos
- Cache de preferências — busca no Redis (cache hit) com fallback para padrão (cache miss)
- Testes unitários com JUnit 5 e Mockito — MongoDB e Redis totalmente mockados
- MongoDB embarcado (Flapdoodle) para testes de integração sem infraestrutura
- Docker Compose com MongoDB, Redis e API
- Pipeline CI com GitHub Actions

---

## Por que MongoDB para notificações?

Cada tipo de notificação pode ter campos diferentes:

```json
// EMAIL
{ "tipo": "EMAIL", "metadados": { "assunto": "Compra confirmada", "templateId": "t001" } }

// SMS
{ "tipo": "SMS", "metadados": { "ddd": "92", "operadora": "claro" } }

// PUSH
{ "tipo": "PUSH", "metadados": { "deviceToken": "abc123", "badge": 1 } }
```

Em SQL isso exigiria colunas nulas ou tabelas separadas. No MongoDB, cada documento tem exatamente os campos que precisa.

---

## Por que Redis para preferências e rate limiting?

**Preferências:** lidas a cada envio de notificação. Redis mantém em memória com TTL — ~10x mais rápido que MongoDB para leituras frequentes.

**Rate limiting:** usa INCR + EXPIRE atômicos. Thread-safe por natureza, sem necessidade de locks.

---

## Tecnologias

| Tecnologia | Uso |
|---|---|
| MongoDB 7 | Persistência de notificações |
| Redis 7 | Cache de preferências + rate limiting |
| Spring Data MongoDB | MongoRepository |
| Spring Data Redis | RedisTemplate |
| Flapdoodle | MongoDB embarcado para testes |
| JUnit 5 + Mockito | Testes unitários |
| Swagger UI | Documentação interativa |
| Docker Compose | Orquestração local |

---

## Arquitetura

```
src/
├── config/
│   └── ConfiguracaoRedis.java      ← serialização JSON no Redis
├── controller/
│   └── ControladorNotificacao.java ← endpoints REST
├── dto/
│   └── NotificacaoDTO.java         ← requisição, resposta, preferências
├── enums/
│   ├── TipoNotificacao.java        ← EMAIL, SMS, PUSH, IN_APP
│   └── StatusNotificacao.java      ← PENDENTE, ENVIADA, FALHOU, LIDA
├── excecao/
│   ├── TratadorDeExcecoes.java
│   ├── RateLimitExcedidoException.java
│   └── RecursoNaoEncontradoException.java
├── modelo/
│   ├── Notificacao.java            ← @Document MongoDB
│   └── PreferenciasUsuario.java    ← armazenado no Redis
├── repositorio/
│   └── NotificacaoRepositorio.java ← MongoRepository
└── servico/
    ├── ServicoNotificacao.java     ← lógica de negócio
    └── ServicoRedis.java           ← cache + rate limiting
```

---

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/notificacoes` | Enviar notificação |
| GET | `/api/notificacoes/usuario/{id}` | Listar notificações |
| GET | `/api/notificacoes/usuario/{id}/status/{status}` | Filtrar por status |
| PATCH | `/api/notificacoes/{id}/lida` | Marcar como lida |
| GET | `/api/notificacoes/usuario/{id}/resumo` | Resumo por status |
| PUT | `/api/notificacoes/usuario/{id}/preferencias` | Salvar preferências no Redis |

---

## Como executar

```bash
docker-compose up --build
```

Acesse o Swagger em `http://localhost:8086/swagger-ui.html`

---

## Testes

```bash
mvn test
```

Os testes rodam sem MongoDB nem Redis instalados:
- MongoDB: substituído pelo Flapdoodle (embarcado em memória)
- Redis: mockado via Mockito

---

## Autor

**LNL**
GitHub: [@leonlimask20-dot](https://github.com/leonlimask20-dot)
Email: leonlimask@gmail.com
