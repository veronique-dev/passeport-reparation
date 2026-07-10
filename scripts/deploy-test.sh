#!/usr/bin/env bash
# Déploie l’environnement de test sur un VPS via SSH + Docker Compose.
#
# Prérequis sur le serveur : Docker + plugin Compose, accès SSH.
# Usage :
#   export TEST_SSH=user@ton-vps
#   export TEST_DIR=/opt/passeport-reparation   # optionnel
#   ./scripts/deploy-test.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEST_SSH="${TEST_SSH:?Définis TEST_SSH=user@host}"
TEST_DIR="${TEST_DIR:-/opt/passeport-reparation}"
ENV_FILE="${ENV_FILE:-$ROOT/infra/test/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Fichier manquant : $ENV_FILE"
  echo "Copie infra/test/.env.example → infra/test/.env et renseigne les valeurs."
  exit 1
fi

echo "==> Sync code → ${TEST_SSH}:${TEST_DIR}"
ssh "$TEST_SSH" "mkdir -p '$TEST_DIR'"
rsync -az --delete \
  --exclude '.git' \
  --exclude 'frontend/node_modules' \
  --exclude 'frontend/dist' \
  --exclude '**/target' \
  --exclude '.env' \
  --exclude 'infra/test/.env' \
  "$ROOT/" "$TEST_SSH:$TEST_DIR/"

echo "==> Copie .env de test"
scp "$ENV_FILE" "$TEST_SSH:$TEST_DIR/infra/test/.env"

echo "==> Build & up"
ssh "$TEST_SSH" "cd '$TEST_DIR' && docker compose -f infra/test/docker-compose.yml --env-file infra/test/.env up -d --build"

echo "==> Health (frontend)"
# shellcheck disable=SC2029
PUBLIC_BASE_URL="$(grep -E '^PUBLIC_BASE_URL=' "$ENV_FILE" | cut -d= -f2-)"
curl -fsS -o /dev/null -w "HTTP %{http_code}\n" "${PUBLIC_BASE_URL}/" || {
  echo "Le front ne répond pas encore — attends 30–60s puis reteste ${PUBLIC_BASE_URL}/"
}

echo "OK. App: ${PUBLIC_BASE_URL}/  ·  Mailpit: ${PUBLIC_BASE_URL%:*}:8025 (si MAILPIT_UI_PORT=8025)"
