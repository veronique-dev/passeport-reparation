# Plan de test — MVP Passeport de réparation

**Rôle :** QA  
**Version :** MVP 1.0  
**Source stories :** `product/user-stories-mvp.json`  
**Matrice :** `product/test-matrix.json`

## Objectif

Valider le parcours **Photo → Catégorie + panne → Verdict € → Contact réparateur** et la non-régression des règles d’estimation.

## Niveaux de test

| Niveau | Description | Où |
|--------|-------------|-----|
| **Unitaire** | Catalogue prix, verdict, service diagnostic (Mockito) | `services/diagnosis-service/.../test` |
| **API manuelle** | Curl / Postman via gateway | `http://localhost:8090` |
| **UI manuelle** | Parcours Angular | `http://localhost:4200` |
| **E2E auto** | À venir (REST Assured via gateway) | hors scope immédiat |

## Matrice User Story → Tests

| US | Titre | Auto | Manuel |
|----|-------|------|--------|
| US-01 | Importer une photo | — | UI + `POST /api/media` |
| US-02 | Confirmer catégorie | — | UI |
| US-03 | Sélectionner panne | `PricingCatalogTest`, `DiagnosisServiceTest.us03_*` | UI + `GET /api/diagnoses/issues` |
| US-04 | Hors périmètre | `DiagnosisServiceTest.us04_*` | UI « Autre » |
| US-05 | Estimation coût | `PricingCatalogTest`, `DiagnosisServiceTest.us05_*` | API + UI |
| US-06 | Verdict | `VerdictCalculatorTest`, `DiagnosisServiceTest.us06_*` | UI |
| US-07 | Disclaimer | `DiagnosisServiceTest.us06_and_us07_*` | UI |
| US-08 | Liste réparateurs | — | UI + `GET /api/repairers` |
| US-09 | Contact 1 clic | — | UI (tel / mailto / wa.me) |
| US-10 | Écran passeport | — | UI `/resultat` |
| US-11 | Nouvelle photo | — | UI |

## Cas de test manuels (checklist)

### Smoke parcours heureux
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

## Critères de sortie QA (MVP)

- [ ] Tous les tests unitaires diagnosis-service verts
- [ ] Smoke UI parcours heureux OK
- [ ] Cas « Autre » OK (pas de faux four)
- [ ] Disclaimer visible
- [ ] Contact 1 clic OK sur au moins 1 réparateur seed
- [ ] Aucune régression port (API sur **8090**, pas 8080 kids-activities)

## Commandes

```bash
# Unitaires diagnosis
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
mvn -pl services/diagnosis-service -am test

# Stack
docker compose up -d
```

## Bugs connus / hors scope QA

- Pas d’IA vision : la photo n’identifie pas l’objet (comportement attendu MVP)
- Zone géographique figée à Lyon
- Pas de suite E2E automatisée encore
