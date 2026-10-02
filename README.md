# Brainvest API

API REST em Spring Boot para o fluxo de aprendizado financeiro do Brainvest. A API valida respostas, calcula progresso/recompensas e salva perfis convidados em PostgreSQL.

## Stack

- Java 21, Spring Boot 3.5 e Maven
- PostgreSQL com Flyway
- Docker para deploy no Render
- Neon Postgres para banco gratuito/gerenciado

## Rodar localmente

Requisitos: Java 21, Maven 3.9+ e Docker Desktop.

```powershell
docker compose up -d postgres
mvn spring-boot:run
```

API local: `http://localhost:8082`

Health check: `http://localhost:8082/actuator/health`

Swagger/OpenAPI: `http://localhost:8082/swagger-ui.html`

O PostgreSQL local fica em `localhost:5433` para nao bater com outro PostgreSQL local na porta `5432`. A senha em `compose.yaml` e apenas para desenvolvimento.

## Endpoints principais

- `GET /api/v1/curriculum`: retorna os cinco niveis e 25 perguntas, sem expor as respostas corretas.
- `POST /api/v1/learners`: cria um perfil convidado.
- `GET /api/v1/learners/{learnerId}/progress`: consulta o progresso salvo.
- `PATCH /api/v1/learners/{learnerId}/profile`: atualiza nome e objetivo.
- `POST /api/v1/learners/{learnerId}/answers`: envia resposta; o backend calcula acerto, XP e recompensas. O `requestId` em UUID torna retentativas idempotentes.
- `POST /api/v1/learners/{learnerId}/missions/{levelId}/restart`: reinicia a missao ativa apos as vidas chegarem a zero.
- `DELETE /api/v1/learners/{learnerId}`: remove o perfil convidado e o progresso relacionado.

## Deploy gratuito com Render + Neon

Este repositorio esta pronto para deploy no Render como **Docker Web Service**. O Render fornece uma URL HTTPS publica, por exemplo:

```text
https://brainvest-api.onrender.com
```

O banco fica no Neon Postgres. Nao precisa de AWS, EC2, PEM, SSH, porta 22 ou dominio proprio.

### Variaveis de ambiente no Render

Configure estas variaveis no servico Render:

| Nome | Valor |
| --- | --- |
| `DB_URL` | JDBC URL do Neon, exemplo `jdbc:postgresql://host/neondb?sslmode=require` |
| `DB_USERNAME` | Usuario do Neon |
| `DB_PASSWORD` | Senha do Neon |
| `FRONTEND_ORIGINS` | Origem publica do Lovable, exemplo `https://pixel-perfect-snap-5512.lovable.app` |

O Render injeta `PORT` automaticamente. A aplicacao ja usa `PORT` quando ele existir e cai para `8082` no desenvolvimento local.

### Health check

Use este path no Render:

```text
/actuator/health
```

Quando o deploy estiver pronto, teste:

```text
https://<seu-servico>.onrender.com/actuator/health
```

Resposta esperada:

```json
{"status":"UP"}
```

### Conectar Lovable

Depois do deploy no Render, configure o frontend para usar:

```text
VITE_API_BASE_URL=https://<seu-servico>.onrender.com
```

Se o Lovable tiver gravado a URL antiga direto no codigo, troque para a URL nova do Render.

## Limitacoes de seguranca

O `learnerId` atual e um identificador convidado, nao autenticacao. Qualquer pessoa com acesso a API pode criar perfis convidados. Para uma entrega final mais robusta, adicione autenticacao/autorizacao, rate limiting, backups automatizados e secrets gerenciados.
