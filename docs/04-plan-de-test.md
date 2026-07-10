# Plan de test — MVP Passeport de réparation

**Rôle :** QA  
**Version :** MVP 1.0 + compte optionnel (v1.1) + claim (US-12) + E2E auth (US-22)  
**Source stories :** `product/user-stories-mvp.json`, `product/user-stories-v1.2.json`  
**Matrice :** `product/test-matrix.json`

## Objectif

Valider :
1. le parcours anonyme **Photo → Catégorie + panne → Verdict € → Contact réparateur**
2. le parcours compte **Inscription → Confirm email → Connexion → Mes passeports / claim**

## Niveaux de test

| Niveau | Description | Où |
|--------|-------------|-----|
| **Unitaire** | Media, diagnosis, repairer, auth, vision | `services/*/src/test` |
| **Unitaire front** | Auth, claim, API, guard, interceptor, login/home | `frontend` (`npm run test:ci`) |
| **API manuelle** | Curl / Postman via gateway | `http://localhost:8090` |
| **UI manuelle** | Parcours Angular | `http://localhost:4200` |
| **E2E auto** | REST Assured via gateway (`-Pe2e`) | `e2e-tests/` |

## Matrice User Story → Tests

| US | Titre | Auto | Manuel |
|----|-------|------|--------|
| US-01 | Importer une photo | `MediaStorageServiceTest`, `MvpAcceptanceTest.us01_*` | UI + `POST /api/media` |
| US-02 | Confirmer catégorie | `HomePageComponent` (canSubmit / issues) | UI |
| US-03 | Sélectionner panne | `PricingCatalogTest`, `DiagnosisServiceTest.us03_*`, `MvpAcceptanceTest.us03_*`, `HomePageComponent` | UI + `GET /api/diagnoses/issues` |
| US-04 | Hors périmètre | `DiagnosisServiceTest.us04_*`, `MvpAcceptanceTest.us04_*` | UI « Autre » |
| US-05 | Estimation coût | `PricingCatalogTest`, `DiagnosisServiceTest.us05_*`, `MvpAcceptanceTest.us05_*` | API + UI |
| US-06 | Verdict | `VerdictCalculatorTest`, `DiagnosisServiceTest.us06_*`, `MvpAcceptanceTest.us05_us06_us07_*` | UI |
| US-07 | Disclaimer | `DiagnosisServiceTest.us06_and_us07_*`, `MvpAcceptanceTest.us05_us06_us07_*` | UI |
| US-08 | Liste réparateurs | `RepairerServiceTest`, `MvpAcceptanceTest.us08_*` | UI + `GET /api/repairers` |
| US-09 | Contact 1 clic | `RepairerServiceTest`, `MvpAcceptanceTest.us09_*` | UI (tel / mailto / wa.me) |
| US-10 | Écran passeport | `MvpAcceptanceTest.us10_*` | UI `/resultat` |
| US-11 | Nouvelle photo | — | UI |
| Compte | Register / login / reset | `AuthServiceTest`, specs Angular auth/login | UI `/inscription`, `/connexion` |
| US-12 | Claim passeport anonyme | `DiagnosisServiceTest.us12_*`, `AuthAcceptanceTest.us12_*`, `DiagnosisClaimService` | UI après login |
| US-22 | E2E auth | `AuthAcceptanceTest.us22_*` | — |

## Cas de test manuels (checklist)

### Smoke parcours heureux (anonyme)
1. Ouvrir `http://localhost:4200`
2. Importer une photo JPEG
3. Choisir **Four** → symptôme **Joint de porte usé**
4. Obtenir le passeport
5. Vérifier : fourchette **50–120 €**, verdict **Réparer**, disclaimer visible
6. Vérifier présence de réparateurs Lyon
7. Cliquer **Appeler** / **Email** / **WhatsApp** (liens corrects)
8. **Nouvelle photo** → retour accueil

### Hors périmètre (anti-régression fer à repasser)
1. Importer n’importe quelle photo
2. Choisir **Autre**
3. Vérifier message hors périmètre
4. Résultat sans prix ni réparateurs

### Estimation différenciée
1. Four + joint de porte → fourchette basse
2. Four + carte électronique → fourchette plus haute
3. Les deux fourchettes doivent différer

### Compte optionnel + claim (US-12 / US-22)
1. Faire une estimation **anonyme** jusqu’au résultat
2. Aller sur `/inscription`, créer un compte
3. Ouvrir Mailpit `http://localhost:8025`, cliquer le lien de confirmation
4. Se connecter sur `/connexion`
5. Vérifier que le passeport anonyme apparaît dans `/mes-passeports` (claim)
6. Rouvrir un passeport → écran résultat
7. Mot de passe oublié → mail Mailpit → nouveau mdp → reconnexion OK
8. Se déconnecter → parcours anonyme toujours possible depuis `/`

### API indisponible
1. Arrêter la gateway
2. Lancer une estimation
3. Message « API indisponible…8090 »

## Jeux de données API

```bash
# Issues four
curl -s 'http://localhost:8090/api/diagnoses/issues?category=OVEN' | jq

# Diagnostic joint de porte
curl -s -X POST http://localhost:8090/api/diagnoses \
  -H 'Content-Type: application/json' \
  -d '{"mediaId":"qa-1","category":"OVEN","issueCode":"OV_DOOR_SEAL"}' | jq

# Hors périmètre
curl -s -X POST http://localhost:8090/api/diagnoses \
  -H 'Content-Type: application/json' \
  -d '{"mediaId":"qa-2","category":"UNSUPPORTED"}' | jq

# Réparateurs
curl -s 'http://localhost:8090/api/repairers?category=OVEN&city=Lyon' | jq
```

Importer : `product/postman/Passeport-Reparation-MVP.postman_collection.json`  
Image d’exemple : `product/postman/sample.png`  
`baseUrl` = `http://localhost:8090` · Mailpit UI = `http://localhost:8025`

### Go / no-go

- [ ] Unitaires auth + diagnosis + media + repairer verts
- [ ] Unitaires Angular verts (`cd frontend && npm run test:ci`)
- [ ] Suite E2E `MvpAcceptanceTest` + `AuthAcceptanceTest` verte via gateway `:8090`
- [ ] Smoke UI parcours heureux OK
- [ ] Cas « Autre » OK (pas de faux four)
- [ ] Disclaimer visible
- [ ] Contact 1 clic OK sur au moins 1 réparateur seed
- [ ] Compte : inscription → confirm Mailpit → login → Mes passeports
- [ ] Claim : passeport anonyme rattaché après connexion
- [ ] Aucune régression port (API sur **8090**)

## Commandes

```bash
# Unitaires (auth inclus)
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
mvn -pl services/auth-service,services/diagnosis-service,services/media-service,services/repairer-service -am test

# E2E via gateway (stack Docker requise — Mailpit pour confirm/reset)
docker compose up -d
mvn -pl e2e-tests -Pe2e test -De2e.base.url=http://localhost:8090 -De2e.mailpit.url=http://localhost:8025

# Unitaires Angular (Chromium via Puppeteer)
cd frontend && npm run test:ci
```

## Bugs connus / hors scope QA

- Vision IA : mock par défaut (pas d’OpenAI requis pour QA)
- Zone géographique figée à Lyon
- US-11 reste manuel (UI « Nouvelle photo »)
