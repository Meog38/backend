# Do Zero ao CPA - Backend

API REST em Spring Boot para o aplicativo educacional gamificado **Do Zero ao CPA**.

- Repositorio hub: https://github.com/Meog38/do-zero-ao-cpa
- Frontend: https://github.com/Meog38/do-zero-ao-cpa-frontend
- App em producao: https://do-zero-ao-cpa.vercel.app/
- API em producao: https://brainvest-api.onrender.com

## Sobre

O backend valida respostas, calcula recompensas, controla autenticacao, protege o acesso ao progresso do aluno e persiste dados em PostgreSQL.

O projeto tem finalidade educacional. Ele nao presta recomendacao individual de investimentos.

## Stack

- Java 21
- Spring Boot 3.5.6
- API REST com JSON
- Spring JDBC
- PostgreSQL
- Flyway
- Spring Mail
- Spring Actuator
- Docker para deploy
- Render para hospedagem da API
- Neon para PostgreSQL gerenciado
- Brevo SMTP para emails transacionais

## Rodar localmente

Requisitos: Java 21, Maven 3.9+ e Docker Desktop.

```powershell
docker compose up -d postgres
mvn spring-boot:run
```

API local:

```text
http://localhost:8082
```

Health check:

```text
http://localhost:8082/actuator/health
```

Swagger/OpenAPI:

```text
http://localhost:8082/swagger-ui.html
```

O PostgreSQL local fica em `localhost:5433` para evitar conflito com uma instalacao local na porta `5432`.

## Endpoints principais

| Metodo | Rota | Descricao |
| ------ | ---- | --------- |
| `GET` | `/api/v1/curriculum` | Retorna 5 niveis e 100 questoes, sem expor gabarito. |
| `POST` | `/api/v1/auth/register` | Cria usuario, learner e sessao. |
| `POST` | `/api/v1/auth/login` | Autentica usuario e devolve token. |
| `GET` | `/api/v1/auth/me` | Consulta usuario logado e progresso. |
| `POST` | `/api/v1/auth/logout` | Encerra a sessao atual. |
| `PATCH` | `/api/v1/auth/password` | Troca senha de usuario autenticado. |
| `POST` | `/api/v1/auth/password/forgot` | Solicita email de recuperacao de senha. |
| `POST` | `/api/v1/auth/password/reset` | Redefine senha com token valido. |
| `GET` | `/api/v1/learners/{learnerId}/progress` | Consulta progresso do learner autenticado. |
| `PATCH` | `/api/v1/learners/{learnerId}/profile` | Atualiza nome e objetivo. |
| `POST` | `/api/v1/learners/{learnerId}/answers` | Valida resposta e calcula XP, vidas, gemas e feedback. |
| `POST` | `/api/v1/learners/{learnerId}/missions/{levelId}/restart` | Reinicia missao ativa quando as vidas acabam. |
| `DELETE` | `/api/v1/learners/{learnerId}` | Remove learner e progresso associado. |

## Banco de dados

As migrations ficam em `src/main/resources/db/migration`.

Tabelas principais:

- `app_users`: usuarios e hashes de senha.
- `auth_sessions`: sessoes autenticadas por token hash.
- `learners`: perfil, objetivo, XP, gemas, vidas e missao ativa.
- `mission_progress`: progresso por nivel.
- `rewarded_questions`: controle de XP ja concedido por questao.
- `answer_requests`: idempotencia de respostas.
- `password_reset_tokens`: tokens hash para recuperacao de senha.

## Variaveis de ambiente

| Nome | Uso |
| ---- | --- |
| `DB_URL` | JDBC URL do PostgreSQL, em producao apontando para Neon. |
| `DB_USERNAME` | Usuario do banco. |
| `DB_PASSWORD` | Senha do banco. |
| `FRONTEND_ORIGINS` | Origens permitidas no CORS, como `https://do-zero-ao-cpa.vercel.app`. |
| `MAIL_ENABLED` | Habilita envio de email transacional. |
| `MAIL_FROM` | Remetente dos emails. |
| `SMTP_HOST` | Host SMTP, por exemplo Brevo. |
| `SMTP_PORT` | Porta SMTP. |
| `SMTP_USERNAME` | Usuario SMTP. |
| `SMTP_PASSWORD` | Chave SMTP. |
| `PASSWORD_RESET_BASE_URL` | URL base usada no link de reset de senha. |
| `PASSWORD_RESET_LOG_TOKENS` | Deve ficar `false` em producao. |

Nunca versione credenciais reais.

## Deploy

O backend esta preparado para deploy no Render como Docker Web Service.

Health check recomendado:

```text
/actuator/health
```

O banco de producao fica no Neon PostgreSQL. O envio de email de recuperacao usa SMTP configurado por variaveis de ambiente.

## Seguranca

- Senhas armazenadas com BCrypt.
- Sessoes salvas como hash SHA-256 do token.
- Endpoints de progresso exigem autenticacao.
- O backend valida se o learner pertence ao usuario autenticado.
- Rate limiting em cadastro, login, reset de senha e envio de respostas.
- CORS configurado para origens conhecidas.

## Testes

```powershell
mvn test
```

Os testes atuais cobrem catalogo de curriculo, API de curriculo, autorizacao dos endpoints de learner e rate limiting. Eles ajudam a proteger partes criticas do prototipo, mas nao substituem QA completo ou testes E2E.
