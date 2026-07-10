# Environnement de test — livraison

**Aujourd’hui (0 €) :** test en **local** + démo publique optionnelle via **tunnel Cloudflare**.  
**Plus tard (budget VPS) :** déploiement sur serveur avec [`infra/test/`](../infra/test/).

---

## Option A — Test local (recommandé sans budget)

C’est l’environnement de test principal tant qu’il n’y a pas de serveur.

```bash
docker compose up -d --build
cd frontend && npm start
```

| Service | URL |
|---------|-----|
| App (dev) | http://localhost:4200 |
| App (Docker) | http://localhost:4201 |
| API gateway | http://localhost:8090 |
| Mailpit | http://localhost:8025 |

Checklist smoke : parcours anonyme, inscription → Mailpit → confirm, Mes passeports, reset mdp.

---

## Option B — Démo publique gratuite (tunnel)

Pour montrer l’app à un tiers **sans VPS** : ta machine reste le serveur, Cloudflare (ou ngrok) publie une URL temporaire.

### Prérequis

1. Stack Docker **complète** (front inclus), pour une seule origine publique :

```bash
docker compose up -d --build
# App : http://localhost:4201  (nginx proxy /api → gateway)
```

2. Compte Cloudflare gratuit + outil [`cloudflared`](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/)  
   (alternative : [ngrok](https://ngrok.com/) `ngrok http 4201`)

### Lancer le tunnel

```bash
# Expose le front Docker (qui proxy déjà /api)
cloudflared tunnel --url http://localhost:4201
```

Cloudflare affiche une URL du type `https://xxxx.trycloudflare.com`.

### Brancher les liens email

Les mails de confirm / reset doivent pointer vers l’URL du tunnel (pas `localhost`) :

```bash
# Exemple — adapte l’URL affichée par cloudflared
export APP_FRONTEND_URL=https://xxxx.trycloudflare.com
export MEDIA_PUBLIC_BASE_URL=https://xxxx.trycloudflare.com
export CORS_ALLOWED_ORIGINS=https://xxxx.trycloudflare.com

docker compose up -d auth-service media-service gateway
```

Puis réouvre Mailpit (`:8025`) : les nouveaux mails contiennent le bon lien.

### Limites (assumées)

- Ta machine doit rester allumée et connectée
- L’URL change à chaque relance du tunnel rapide (sauf tunnel nommé Cloudflare)
- Débit / stabilité = ta box — OK pour une démo, pas pour une QA permanente

---

## Option C — VPS (quand tu auras un budget)

**Cible :** un VPS (OVH, Hetzner, DigitalOcean…) avec Docker Compose.  
Même stack que le local, durcie pour une URL publique unique.

### Architecture

```
Internet
   │
   ▼
:80  frontend (nginx)
   ├── /          → SPA Angular
   └── /api/**    → gateway → auth | media | diagnosis | repairer
   │
:8025 Mailpit UI (emails confirm / reset — QA uniquement)
```

- Postgres **non exposé**
- Gateway **non exposé** (uniquement via nginx)
- Emails → Mailpit (pas de vrai SMTP en v1 test)

Fichiers :

| Fichier | Rôle |
|---------|------|
| [`infra/test/docker-compose.yml`](../infra/test/docker-compose.yml) | Stack test |
| [`infra/test/.env.example`](../infra/test/.env.example) | Secrets / URL publique |
| [`scripts/deploy-test.sh`](../scripts/deploy-test.sh) | Sync SSH + `compose up` |
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | Build + tests sur `main` |

### Prérequis serveur

- Ubuntu 22.04+ (ou équivalent)
- Docker Engine + Compose plugin
- Ports ouverts : **80** (app), **8025** (Mailpit QA — idéalement restreint à ton IP)
- Accès SSH

### Première livraison

#### 1. Préparer le `.env` en local

```bash
cp infra/test/.env.example infra/test/.env
```

Renseigner au minimum :

- `PUBLIC_BASE_URL` — ex. `http://203.0.113.10`
- `POSTGRES_PASSWORD` — fort
- `JWT_SECRET` — ≥ 32 caractères
- `CORS_ALLOWED_ORIGINS` — en général = `PUBLIC_BASE_URL`

#### 2. Déployer

```bash
export TEST_SSH=ubuntu@TON_IP
export TEST_DIR=/opt/passeport-reparation
chmod +x scripts/deploy-test.sh
./scripts/deploy-test.sh
```

Sans script (manuel sur le serveur) :

```bash
cd /opt/passeport-reparation
cp infra/test/.env.example infra/test/.env   # éditer
docker compose -f infra/test/docker-compose.yml --env-file infra/test/.env up -d --build
```

#### 3. Vérifier

| Check | URL / commande |
|-------|----------------|
| App | `http://<HOST>/` |
| Mailpit | `http://<HOST>:8025` |
| Inscription | compte → mail Mailpit → confirmer |

### Mises à jour

```bash
export TEST_SSH=ubuntu@TON_IP
./scripts/deploy-test.sh
```

### TLS (optionnel)

Quand un domaine pointe vers le VPS, placer Caddy/Traefik devant le port 80, puis :

```env
PUBLIC_BASE_URL=https://test.ton-domaine.fr
CORS_ALLOWED_ORIGINS=https://test.ton-domaine.fr
```

### Hors scope volontaire

- Déploiement auto GitHub Actions → VPS
- SMTP réel (SendGrid, etc.)
- Haute dispo / backups auto

### Dépannage VPS

| Symptôme | Piste |
|----------|--------|
| Page blanche / 502 | `docker compose … logs gateway frontend` |
| Pas d’email | Mailpit `:8025` ; `MAIL_ENABLED=true` |
| Lien confirm casse | `PUBLIC_BASE_URL` doit matcher l’URL ouverte |
| CORS | Origine exacte dans `CORS_ALLOWED_ORIGINS` |
