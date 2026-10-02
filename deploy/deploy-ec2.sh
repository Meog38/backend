#!/usr/bin/env bash
set -euo pipefail

: "${API_DOMAIN:?API_DOMAIN is required}"
: "${FRONTEND_ORIGIN:?FRONTEND_ORIGIN is required}"

JAR_SOURCE=/tmp/brainvest-release/brainvest-api-0.1.0-SNAPSHOT.jar
JAR_TARGET=/opt/brainvest/app.jar

[[ -s "$JAR_SOURCE" ]] || { echo "Release JAR is missing: $JAR_SOURCE" >&2; exit 1; }
command -v docker >/dev/null || { echo "Docker is not installed. Run the EC2 bootstrap script first." >&2; exit 1; }
command -v openssl >/dev/null || { echo "OpenSSL is not installed." >&2; exit 1; }

systemctl enable --now docker
install -d -m 700 /opt/brainvest /var/lib/brainvest/postgres

if [[ ! -s /var/lib/brainvest/db-password ]]; then
  umask 077
  openssl rand -hex 32 > /var/lib/brainvest/db-password
fi
DB_PASSWORD=$(cat /var/lib/brainvest/db-password)

docker network inspect brainvest >/dev/null 2>&1 || docker network create brainvest

if ! docker container inspect brainvest-postgres >/dev/null 2>&1; then
  docker run -d \
    --name brainvest-postgres \
    --network brainvest \
    --network-alias postgres \
    --restart unless-stopped \
    -e POSTGRES_DB=brainvest \
    -e POSTGRES_USER=brainvest \
    -e "POSTGRES_PASSWORD=$DB_PASSWORD" \
    -v /var/lib/brainvest/postgres:/var/lib/postgresql/data \
    --health-cmd="pg_isready -U brainvest -d brainvest" \
    --health-interval=5s \
    --health-timeout=3s \
    --health-retries=20 \
    postgres:17-alpine
else
  docker start brainvest-postgres >/dev/null 2>&1 || true
fi

for attempt in $(seq 1 30); do
  if docker exec brainvest-postgres pg_isready -U brainvest -d brainvest >/dev/null 2>&1; then
    break
  fi
  if [[ "$attempt" -eq 30 ]]; then
    echo "PostgreSQL did not become healthy." >&2
    exit 1
  fi
  sleep 2
done

cp "$JAR_SOURCE" "$JAR_TARGET.new"
chmod 644 "$JAR_TARGET.new"
mv "$JAR_TARGET.new" "$JAR_TARGET"

docker rm -f brainvest-api >/dev/null 2>&1 || true
docker run -d \
  --name brainvest-api \
  --network brainvest \
  --restart unless-stopped \
  -e SERVER_PORT=8082 \
  -e DB_URL=jdbc:postgresql://postgres:5432/brainvest \
  -e DB_USERNAME=brainvest \
  -e "DB_PASSWORD=$DB_PASSWORD" \
  -e "FRONTEND_ORIGINS=$FRONTEND_ORIGIN" \
  -v "$JAR_TARGET:/app/app.jar:ro" \
  eclipse-temurin:21-jre \
  java -XX:MaxRAMPercentage=75 -jar /app/app.jar

docker rm -f brainvest-caddy >/dev/null 2>&1 || true
docker run -d \
  --name brainvest-caddy \
  --network brainvest \
  --restart unless-stopped \
  -p 80:80 \
  -p 443:443 \
  -v brainvest-caddy-data:/data \
  -v brainvest-caddy-config:/config \
  caddy:2-alpine \
  caddy reverse-proxy --from "$API_DOMAIN" --to brainvest-api:8082

for attempt in $(seq 1 30); do
  if docker run --rm --network brainvest curlimages/curl:8.12.1 \
      -fsS http://brainvest-api:8082/actuator/health >/dev/null; then
    break
  fi
  if [[ "$attempt" -eq 30 ]]; then
    docker logs brainvest-api
    exit 1
  fi
  sleep 2
done

echo "Brainvest API deployed successfully."