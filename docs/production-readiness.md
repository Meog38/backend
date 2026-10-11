# Brainvest production checklist

## Deploy

- Deploy the backend after every backend commit. The database migrations run automatically through Flyway.
- Deploy the frontend after every frontend commit. The hosted frontend should use `VITE_API_BASE_URL=https://brainvest-api.onrender.com`.
- Keep `FRONTEND_ORIGINS` on Render aligned with the published frontend origin.

## Password recovery

- Password reset tokens expire after 30 minutes and are single-use.
- Configure `PASSWORD_RESET_BASE_URL` with the frontend recovery URL when a public recovery page/link is available.
- Configure SMTP with `MAIL_ENABLED=true`, `MAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, and `SMTP_PASSWORD`.
- Keep `PASSWORD_RESET_LOG_TOKENS=false` in production. Use `true` only as a temporary operational fallback while there is no e-mail provider.

## Monitoring

- Render health check: `GET /actuator/health`.
- Watch logs for `http_request` entries with `status>=500` or high `duration_ms`.
- Configure Render notifications for failed deploys and service failures.
- Track cold-start latency separately if the service remains on a free/sleeping plan.

## Backups

- Confirm automated backups/PITR in the Postgres provider before public launch.
- Do a restore drill into a temporary database before relying on backups.
- Document retention period, restore owner, and where database credentials are stored.
