# Architecture — Passeport de réparation

**Rôle :** Architecte logiciel  
**Version :** MVP → v1.1 (compte) / v1.2 en cours  
**Style :** Microservices synchrones, monorepo Maven, front Angular

> Synthèse visuelle (Mermaid) : section **Architecture** du [`README.md`](../README.md).

## 1. Objectif architecture

Livrer un parcours **Photo → Suggestion IA → Confirmation → Estimation € → Réparateurs**, avec un **compte optionnel** (historique Mes passeports), une séparation claire des bounded contexts, et un déploiement local simple (Docker Compose).

## 2. Vue d’ensemble

```
┌──────────────────┐
│  Angular SPA     │  :4200 (dev) / :4201 (Docker)
│  frontend        │
└────────┬─────────┘
         │ HTTP JSON / multipart
         ▼
┌──────────────────┐
│  API Gateway     │  :8090
│  Spring Cloud    │  CORS + routage /api/**
└────────┬─────────┘
    ┌────┼─────────────┬──────────────┐
    ▼    ▼             ▼              ▼
┌────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ auth   │ │ media        │ │ diagnosis    │ │ repairer     │
│ :8084  │ │ :8083        │ │ :8081        │ │ :8082        │
│ auth_db│ │ volume disque│ │ diagnosis_db │ │ repairer_db  │
│ Mailpit│ └──────────────┘ │ (+ Vision)   │ └──────────────┘
└────────┘                  └──────────────┘
```

Le frontend **ne parle qu’à la gateway**. Les services ne s’appellent presque pas entre eux : orchestration côté client. Exception : `diagnosis-service` peut lire une photo via `media-service` pour la suggestion vision.

## 3. Modules Maven

| Module | Rôle |
|--------|------|
| `common/` | DTOs, enums (`ApplianceCategory`, `IssueCode`, `RepairVerdict`) |
| `services/gateway/` | Spring Cloud Gateway, CORS, routes `/api/**` |
| `services/auth-service/` | Compte, JWT + refresh, confirm email, reset mdp |
| `services/media-service/` | Upload / lecture photos (stockage disque) |
| `services/diagnosis-service/` | Vision suggest, catalogue pannes, estimation, `/mine`, claim |
| `services/repairer-service/` | Annuaire curaté, filtre catégorie / ville |
| `frontend/` | Angular 16 SPA |
| `e2e-tests/` | Acceptances via gateway + Mailpit |

## 4. Découpage bounded contexts

| Contexte | Service | Données | Règles clés |
|----------|---------|---------|-------------|
| Identité | auth-service | `auth_db` | Compte optionnel ; BCrypt ; JWT + refresh |
| Média | media-service | Fichiers + `mediaId` | Types image, taille max 10 Mo |
| Diagnostic | diagnosis-service | `diagnosis_db` | Confirmation utilisateur = vérité ; grille `IssueCode` ; JWT pour `/mine` |
| Annuaire | repairer-service | `repairer_db` | Seed Lyon ; contact externe |
| Expérience | frontend | Session / tokens | Orchestre le parcours ; auth non bloquante |

## 5. Flux métier

### 5.1 Parcours anonyme (cœur)

```
User → Angular → Gateway → Media / Diagnosis / Repairer
  1. POST /api/media                    → mediaId
  2. POST /api/diagnoses/suggest        → préremplissage (optionnel)
  3. GET  /api/diagnoses/issues         → catalogue pannes
  4. POST /api/diagnoses                → estimate + verdict
  5. GET  /api/repairers?city=Lyon      → contacts
```

### 5.2 Compte & historique

```
  register → email Mailpit → confirm
  login → JWT access + refresh
  POST /api/diagnoses (Bearer)     → rattache userId
  POST /api/diagnoses/{id}/claim   → lie un passeport anonyme
  GET  /api/diagnoses/mine         → historique (createdAt DESC)
```

Diagrammes Mermaid (séquence, C4, ER) : voir le README.

## 6. Contrats API (gateway)

| Méthode | Chemin | Service |
|---------|--------|---------|
| `*` | `/api/auth/**` | auth |
| `POST` / `GET` | `/api/media/**` | media |
| `*` | `/api/diagnoses/**` | diagnosis |
| `GET` | `/api/repairers/**` | repairer |

Liste détaillée : section **API** du README.

### Contrat `POST /api/diagnoses`

```json
{
  "mediaId": "uuid",
  "category": "WASHING_MACHINE | DISHWASHER | OVEN | UNSUPPORTED",
  "issueCode": "WM_DRAIN_PUMP | ... | null"
}
```

Réponse : appareil, panne, `estimate`, `verdict`, `disclaimer`, `supported`, `userConfirmed`, `createdAt`.

## 7. Modèle de données (simplifié)

**auth_db** — users, refresh tokens, tokens confirm / reset.

**diagnosis_db.diagnoses**
- `id`, `media_id`, `user_id` (nullable), `category`, `issue_code`
- `appliance_label`, `probable_issue`
- `repair_low`, `repair_high`, `replacement_approx`, `verdict`
- `supported`, `user_confirmed`, `confidence`, `created_at`

**repairer_db.repairers**
- `id`, `name`, `city`, `phone`, `email`, `whatsapp`, `lat/lng`, `active`
- `repairer_categories` (collection)

**media** : pas de BDD — fichiers `{mediaId}.{ext}` sur volume.

## 8. Règles métier (diagnosis)

1. `category = UNSUPPORTED` → pas d’estimation, pas de réparateurs.
2. Sinon, résolution `IssueCode` dans le catalogue `IssuePricing` (fallback `*_UNKNOWN`).
3. Verdict catalogue, sauf si `(repairLow + repairHigh) / 2 > 70% * replacement` → `REPLACE`.
4. Disclaimer toujours présent.
5. Vision (`/suggest`) : préremplit uniquement ; jamais source de vérité du verdict.

## 9. Décisions d’architecture

| Décision | Choix | Alternative reportée |
|----------|-------|----------------------|
| Style | Microservices + gateway | Monolithe |
| Orchestration | Frontend | BFF / saga |
| Identification appareil | Confirmation + suggestion IA | Vision = verdict |
| Pricing | Catalogue in-memory versionné | Table BDD / moteur règles |
| Auth | JWT maison (auth-service) | Keycloak / Auth0 / OAuth |
| Messaging | Aucun | Kafka / RabbitMQ |
| Discovery | URLs Compose fixes | Eureka / Consul |
| Ports locaux | Gateway **8090**, Postgres **5434** | 8080 / 5432 |

## 10. Qualités & contraintes

- **Disponibilité locale** : Compose suffit ; un service down → message UI.
- **Cohérence** : database-per-service.
- **Évolutivité** : `VisionClient` (mock / openai / off) sans changer le contrat front.
- **Sécurité** : JWT partagé auth ↔ diagnosis ; uploads limités ; compte jamais obligatoire.

## 11. Déploiement

```bash
docker compose up -d --build
# API      http://localhost:8090
# App      http://localhost:4201  (ou npm start → :4200)
# Mailpit  http://localhost:8025
```

## 12. Dette technique assumée

- Pas de contrat OpenAPI centralisé (Swagger par service possible).
- Seed réparateurs en `CommandLineRunner` (pas de back-office).
- Estimation indicative, non calibrée marché réel.
- Rate-limit / refresh auto front : roadmap v1.2 (US-16, US-17).
