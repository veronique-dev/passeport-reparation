# Passeport de réparation

MVP d’aide à la décision quand un appareil électroménager tombe en panne : une photo suffit pour savoir si ça vaut le coup de réparer, et où contacter un réparateur local.

## Problème

Un objet casse. On ne sait ni si la réparation est rentable, ni où aller. Par défaut, on jette et on rachète.

## Concept

En ~30 secondes, à partir d’une photo :

1. **Diagnostic IA** — identification de l’objet et de la panne probable
2. **Estimation réparer vs remplacer** — fourchette de prix pour déclencher la décision
3. **Annuaire local** — réparateurs à proximité, contact en un clic

## Fonctionnalités MVP

### 1. Prise de photo + diagnostic IA

- Capture ou import d’une photo de l’appareil / de la panne
- Identification de l’objet et de la panne probable via IA
- **Périmètre v1 limité à 3 familles** pour maximiser la précision :
  - Lave-linge
  - Lave-vaisselle
  - Four
- Hors périmètre → message explicite : catégorie non supportée pour l’instant

### 2. Estimation réparer vs remplacer

- Fourchette de coût de réparation (basse / haute)
- Ordre de grandeur du remplacement
- Verdict simple : **réparer** / **à arbitrer** / **remplacer**
- Disclaimer : estimation indicative, pas un devis

### 3. Annuaire de réparateurs locaux

- Liste curatée manuellement (zone géographique test : Lyon)
- Filtrage par famille d’appareil
- Contact en un clic (téléphone / email / WhatsApp)
- Pas de marketplace, pas de réservation intégrée, pas de paiement

## Parcours utilisateur

```
Photo → Catégorie + panne → Verdict € → Contacter un réparateur
```

## Hors scope (v1)

- Compte utilisateur obligatoire
- Marketplace / matching temps réel
- Devis en ligne et paiement
- Suivi de réparation
- Tutoriels DIY
- Couverture de toutes les catégories d’appareils

## Stack

| Couche | Techno |
|--------|--------|
| Frontend | Angular 16 |
| Backend | Java 21, Spring Boot 3.3, Maven |
| Base de données | PostgreSQL 16 (DB par service) |
| Conteneurs | Docker / Docker Compose |
| Architecture | Microservices |

### Microservices

| Service | Port | Responsabilité |
|---------|------|----------------|
| **gateway** | 8090 | Entrée unique, routage, CORS |
| **diagnosis-service** | 8081 | Vision (mock) + estimation + verdict |
| **repairer-service** | 8082 | Annuaire curaté, filtre catégorie / zone |
| **media-service** | 8083 | Upload / stockage local des photos |
| **frontend** | 4200 (dev) / 4201 (Docker) | SPA Angular |

```
passeport-reparation/
├── common/                 # DTOs partagés
├── services/
│   ├── gateway/
│   ├── diagnosis-service/
│   ├── repairer-service/
│   └── media-service/
├── frontend/               # Angular
├── infra/postgres/
└── docker-compose.yml
```

**Hors scope infra v1** : Eureka, Kafka, auth — Compose + URLs fixes.

## Démarrage rapide

### Prérequis

- Java 21, Maven 3.9+
- Node 18+, Angular CLI 16
- Docker (optionnel mais recommandé pour Postgres)

### 1. Base de données

```bash
docker compose up -d postgres
# Postgres exposé sur le port 5434 (5433 est souvent pris par d’autres projets)
```

### 2. Backend

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home")
mvn -pl services/media-service,services/diagnosis-service,services/repairer-service,services/gateway -am spring-boot:run
```

Ou lancer chaque service dans un terminal :

```bash
mvn -pl services/media-service -am spring-boot:run
mvn -pl services/diagnosis-service -am spring-boot:run
mvn -pl services/repairer-service -am spring-boot:run
mvn -pl services/gateway -am spring-boot:run
```

API via gateway : `http://localhost:8090`

### 3. Frontend

```bash
cd frontend && npm install && npm start
```

App : `http://localhost:4200`

### Tout en Docker

```bash
docker compose up --build
```

- Frontend : http://localhost:4201  
- API : http://localhost:8090  

## API (via gateway)

| Méthode | Chemin | Description |
|---------|--------|-------------|
| `POST` | `/api/media` | Upload photo (`multipart/form-data`, champ `file`) |
| `GET` | `/api/media/{id}` | Télécharger la photo |
| `GET` | `/api/diagnoses/issues?category=OVEN` | Liste des pannes / prix pour une catégorie |
| `POST` | `/api/diagnoses` | `{ "mediaId", "category", "issueCode?" }` → estimation |
| `GET` | `/api/diagnoses/{id}` | Relire un diagnostic |
| `GET` | `/api/repairers?category=WASHING_MACHINE&city=Lyon` | Annuaire |

Le diagnostic MVP s’appuie sur la **confirmation utilisateur** (catégorie + panne) et une grille de prix, pas sur une IA vision.

## Roadmap indicative

| Version | Contenu |
|---------|---------|
| **MVP** | Photo → confirmation catégorie/panne → estimation → annuaire |
| **v1.1** | Tutoriels DIY si panne simple |
| **v1.2** | Élargissement des catégories / zones |
| **v2** | Compte utilisateur, suivi, partenariats renforcés |

## Licence

À définir.
