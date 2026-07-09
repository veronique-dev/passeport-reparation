# Feature branch — IA vision (derrière confirmation utilisateur)

## Objectif

Proposer une **suggestion IA** de catégorie / panne à partir de la photo, **sans remplacer** la confirmation utilisateur (source de vérité MVP).

## Parcours cible

```
Photo → Suggestion IA (optionnelle) → Confirmation user → Estimation → Réparateurs
```

## Principes

1. L’utilisateur peut toujours corriger / choisir « Autre »
2. Pas d’estimation sans `category` confirmée
3. Provider derrière interface `VisionClient` (OpenAI / Claude / mock)
4. Mode `VISION_PROVIDER=mock|openai|off` via env

## Tâches prévues

- [ ] Réintroduire `VisionClient` + implémentation OpenAI (vision)
- [ ] Endpoint `POST /api/diagnoses/suggest` `{ mediaId }` → suggestion
- [ ] UI : afficher suggestion pré-remplie, éditable
- [ ] Tests unitaires + E2E suggestion
- [ ] Doc architecture / .env.example

## Hors scope de cette branche

- Suppression de la confirmation manuelle
- DIY / multi-villes / auth
