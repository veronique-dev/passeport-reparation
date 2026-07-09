# Architecture — Passeport de réparation (MVP)

**Rôle :** Architecte logiciel  
**Version :** MVP 1.0  
**Style :** Microservices synchrones, monorepo Maven, front Angular

## 1. Objectif architecture

Livrer un parcours **Photo → Confirmation catégorie/panne → Estimation € → Contact réparateur** sans compte utilisateur, avec une séparation claire des responsabilités et un déploiement local simple (Docker Compose).

## 2. Vue d’ensemble

```
┌──────────────────┐
│  Angular SPA     │  :4200 (dev) / :4201 (Docker)
│  frontend        │
└────────┬─────────┘
         │ HTTP JSON / multipart
         ▼
┌──────────────────┐
│  API Gateway     │  :8090 → :8080 (container)
│  Spring Cloud    │  CORS + routage
└────────┬─────────┘
    ┌────┼────────────────┐
    ▼    ▼                ▼
┌────────────┐ ┌──────────────┐ ┌──────────────┐
│ media      │ │ diagnosis    │ │ repairer     │
│ :8083      │ │ :8081        │ │ :8082        │
│ fichiers   │ │ Postgres     │ │ Postgres     │
│ locaux     │ │ diagnosis_db │ │ repairer_db  │
└────────────┘ └──────────────┘ └──────────────┘
```

Le frontend **ne parle qu’à la gateway**. Les services ne s’appellent pas entre eux en MVP (orchestration côté client : upload → diagnose → list repairers).

## 3. Modules Maven

| Module | Rôle |
|--------|------|
| `common/` | DTOs, enums (`ApplianceCategory`, `IssueCode`, `RepairVerdict`) |
| `services/gateway/` | Spring Cloud Gateway, CORS, routes `/api/**` |
| `services/media-service/` | Upload / lecture photos (stockage disque) |
| `services/diagnosis-service/` | Catalogue pannes, estimation, verdict, persistance diagnostic |
| `services/repairer-service/` | Annuaire curaté, filtre catégorie / ville |
| `frontend/` | Angular 16 SPA |

## 4. Découpage bounded contexts

| Contexte | Service | Données | Règles clés |
|----------|---------|---------|-------------|
| Média | media-service | Fichiers + `mediaId` | Types image uniquement, taille max 10 Mo |
| Diagnostic | diagnosis-service | `diagnoses` | Catégorie utilisateur = source de vérité ; prix par `IssueCode` |
| Annuaire | repairer-service | `repairers` | Seed Lyon ; contact externe (tel/mail/WA) |
| Expérience | frontend | Session navigateur | Orchestre le parcours ; pas d’auth |

## 5. Flux métier (séquence)

```
User          Angular           Gateway         Media      Diagnosis    Repairer
 |               |                 |              |            |           |
 |--photo------->|                 |              |            |           |
 |               |--POST /media--->|--forward---->|            |           |
 |               |<--mediaId-------|<-------------|            |           |
 |--catégorie--->|                 |              |            |           |
 |--panne------->|                 |              |            |           |
 |               |--GET /issues--->|--forward----------------->|           |
 |               |<--liste pannes--|<--------------------------|           |
 |--submit------>|                 |              |            |           |
 |               |--POST /diagnoses|------------forward----------------->|           |
 |               |<--estimate+verdict--|<----------------------|           |
 |               |--GET /repairers----->|--forward------------------------>|
 |               |<--liste---------------|<--------------------------------|
 |<--écran résultat--|                 |              |            |           |
```

## 6. Contrats API (gateway)

| Méthode | Chemin | Service | US |
|---------|--------|---------|----|
| `POST` | `/api/media` | media | US-01 |
| `GET` | `/api/media/{id}` | media | US-01 / US-10 |
| `GET` | `/api/diagnoses/issues?category=` | diagnosis | US-03 |
| `POST` | `/api/diagnoses` | diagnosis | US-04 → US-07 |
| `GET` | `/api/diagnoses/{id}` | diagnosis | US-10 |
| `GET` | `/api/repairers?category=&city=` | repairer | US-08 / US-09 |

### Contrat `POST /api/diagnoses`

```json
{
  "mediaId": "uuid",
  "category": "WASHING_MACHINE | DISHWASHER | OVEN | UNSUPPORTED",
  "issueCode": "WM_DRAIN_PUMP | ... | null"
}
```

Réponse : appareil, panne, `estimate`, `verdict`, `disclaimer`, `supported`, `userConfirmed`.

## 7. Modèle de données (simplifié)

**diagnosis_db.diagnoses**
- `id`, `media_id`, `category`, `issue_code`, `appliance_label`, `probable_issue`
- `repair_low`, `repair_high`, `replacement_approx`, `verdict`
- `supported`, `user_confirmed`, `confidence`, `created_at`

**repairer_db.repairers**
- `id`, `name`, `city`, `phone`, `email`, `whatsapp`, `lat/lng`, `active`
- `repairer_categories` (collection)

**media** : pas de BDD — fichiers nommés `{mediaId}.{ext}` sur volume.

## 8. Règles métier (diagnosis)

1. `category = UNSUPPORTED` → pas d’estimation, pas de réparateurs.
2. Sinon, résolution `IssueCode` dans le catalogue `IssuePricing` (fallback `*_UNKNOWN`).
3. Verdict catalogue, sauf si `(repairLow + repairHigh) / 2 > 70% * replacement` → `REPLACE`.
4. Disclaimer toujours présent.

## 9. Décisions d’architecture

| Décision | Choix MVP | Alternative reportée |
|----------|-----------|----------------------|
| Style | Microservices + gateway | Monolithe |
| Orchestration | Frontend (choreography légère) | BFF / saga |
| Identification appareil | Confirmation utilisateur | Vision IA |
| Pricing | Catalogue in-memory versionné | Table BDD / moteur règles |
| Auth | Aucune | JWT |
| Messaging | Aucun | Kafka / RabbitMQ |
| Discovery | URLs Compose fixes | Eureka / Consul |
| Ports locaux | Gateway **8090**, Postgres **5434** | 8080/5433 (conflit kids-activities) |

## 10. Qualités & contraintes

- **Disponibilité locale** : Compose suffit ; un service down → message UI.
- **Cohérence** : chaque service a sa DB (database-per-service).
- **Évolutivité** : brancher un `VisionClient` derrière diagnosis sans changer le front (category reste overridable).
- **Sécurité MVP** : pas de données personnelles obligatoires ; uploads limités ; pas d’auth.

## 11. Déploiement

```bash
docker compose up -d --build
# API  http://localhost:8090
# App  http://localhost:4201  (ou npm start → :4200)
```

## 12. Dette technique assumée

- Pas de contrat OpenAPI publié centralisé (Swagger par service).
- Pas de tests E2E automatisés via gateway (à ajouter).
- Seed réparateurs en `CommandLineRunner` (pas de back-office).
- Estimation indicative, non calibrée marché réel.
