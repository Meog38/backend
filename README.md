# Brainvest API

API REST em Spring Boot para o fluxo de aprendizado financeiro do Brainvest. A API valida respostas, calcula progresso/recompensas e salva perfis convidados em PostgreSQL.

## Stack

- Java 21, Spring Boot 3.5 e Maven
- PostgreSQL 17 com Flyway
- Docker para PostgreSQL local e, na EC2, PostgreSQL + API + Caddy
- GitHub Actions faz deploy na EC2 por SSH usando a PEM como Secret criptografada

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

## Deploy na EC2 com GitHub Actions

O workflow `.github/workflows/deploy-ec2.yml` roda os testes, gera o JAR, conecta na EC2 por SSH, copia o JAR e `deploy/deploy-ec2.sh`, e entao sobe/atualiza:

- PostgreSQL em Docker, sem porta publica
- API Java em Docker
- Caddy em Docker para HTTPS automatico

Este caminho usa a PEM da EC2 somente como **GitHub Actions Secret**. Nao precisa configurar AWS CLI, AWS account ID, region, ECR, OIDC role ou access keys para este deploy inicial.

### 1. Confirmar EC2 e acesso SSH

O IP informado foi `3.221.155.116`. Confirme no console da AWS se ele ainda e o IP publico da instancia.

Teste a PEM no seu computador:

```powershell
ssh -i "$HOME\Downloads\sua-chave.pem" ubuntu@3.221.155.116
```

O banner do SSH dessa instancia indica Ubuntu (`OpenSSH_9.6p1 Ubuntu-3ubuntu13.19`), entao use `EC2_USER=ubuntu` no GitHub. Em Amazon Linux 2023, o usuario costuma ser `ec2-user`.

Nao coloque a PEM no repositorio, em `.env`, no README, no workflow ou no chat.

### 2. Preparar a EC2

Para Ubuntu 24.04, rode o conteudo de `deploy/ec2-user-data-ubuntu-24.04.sh` como user data ao criar a instancia, ou entre na EC2 e rode com `sudo`. Ele instala Docker e OpenSSL.

Para Amazon Linux 2023, use `deploy/ec2-user-data-amazon-linux-2023.sh`.

O usuario usado pelo GitHub Actions precisa conseguir rodar `sudo` sem senha.

No Security Group da instancia, libere:

- SSH TCP `22`: necessario para o GitHub Actions conectar por SSH. O ideal e restringir ao maximo; abrir para `0.0.0.0/0` funciona para MVP, mas e menos seguro.
- HTTP TCP `80`: necessario para o Caddy emitir/renovar certificado.
- HTTPS TCP `443`: necessario para Lovable e testadores acessarem a API.

Nao libere PostgreSQL `5432` ou `5433` para a internet.

Para persistencia real, use um EBS criptografado montado em `/var/lib/brainvest` antes do primeiro deploy. O script guarda os dados do PostgreSQL e a senha gerada nesse caminho.

### 3. Criar DNS para HTTPS

O Lovable roda em HTTPS, entao o navegador bloqueia chamadas para uma API apenas em HTTP. Voce precisa de um dominio ou subdominio, por exemplo:

```text
api.seu-dominio.com
```

Crie um registro DNS `A` apontando esse host para o IP da EC2, de preferencia um Elastic IP. O valor final da API publicada sera:

```text
https://api.seu-dominio.com
```

Nao use `http://3.221.155.116` no Lovable para a versao publicada.

### 4. Configurar GitHub Variables e Secret

Abra o repositorio backend no GitHub:

```text
https://github.com/Meog38/backend
```

Va em:

```text
Settings -> Secrets and variables -> Actions
```

Na aba **Variables**, crie:

| Nome | Valor |
| --- | --- |
| `EC2_HOST` | `3.221.155.116` ou o Elastic IP confirmado |
| `EC2_USER` | `ubuntu` para esta instancia Ubuntu; `ec2-user` apenas se trocar para Amazon Linux |
| `EC2_KNOWN_HOSTS` | Saida completa de `ssh-keyscan -H 3.221.155.116` |
| `API_DOMAIN` | Dominio da API, exemplo `api.seu-dominio.com`, sem `https://` |
| `FRONTEND_ORIGIN` | Origem exata do Lovable, exemplo `https://pixel-perfect-snap-5512.lovable.app`, sem caminho extra |

Na aba **Secrets**, crie:

| Nome | Valor |
| --- | --- |
| `EC2_SSH_PRIVATE_KEY` | Conteudo completo da PEM, incluindo as linhas `BEGIN` e `END` |

`EC2_KNOWN_HOSTS` e a chave publica do servidor, nao e a PEM. Para gerar:

```powershell
& "C:\Program Files\Git\usr\bin\ssh-keyscan.exe" -H -t ed25519 3.221.155.116
```

Se o `ssh-keyscan` padrao do Windows responder `choose_kex: unsupported KEX method sntrup761x25519-sha512@openssh.com`, use o comando acima com o `ssh-keyscan.exe` do Git Bash. Copie para `EC2_KNOWN_HOSTS` somente a linha que comeca com `|1|` e termina com `ssh-ed25519 ...`; nao precisa copiar as linhas de comentario que comecam com `#`.

Se a instancia ou IP mudar, atualize `EC2_HOST` e gere novamente `EC2_KNOWN_HOSTS`.

### 5. Fazer deploy

Depois de configurar as variaveis e o Secret, faca commit e push para `main`. O workflow roda automaticamente:

```text
Actions -> Deploy API to EC2
```

Ele falha antes do SSH se alguma variavel/secret estiver faltando. No final, verifica:

```text
https://<API_DOMAIN>/actuator/health
```

### 6. Conectar o Lovable

Quando o health check publico responder `UP`, abra as configuracoes do projeto Lovable e defina:

```text
VITE_API_BASE_URL=https://<API_DOMAIN>
```

Depois rode um novo build/deploy no Lovable. Para desenvolvimento local do frontend, mantenha `.env.local` com:

```text
VITE_API_BASE_URL=http://localhost:8082
```

## Limitacoes de seguranca

O `learnerId` atual e um identificador convidado, nao autenticacao. Qualquer pessoa com acesso a API pode criar perfis convidados. Para uma entrega final mais robusta, adicione autenticacao/autorizacao, rate limiting, backups automatizados do banco, secrets gerenciados e, idealmente, troque SSH publico por SSM/OIDC.
