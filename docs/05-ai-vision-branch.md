# Feature branch — IA vision (derrière confirmation utilisateur)

## Objectif

Proposer une **suggestion IA** de catégorie / panne à partir de la photo, **sans remplacer** la confirmation utilisateur (source de vérité MVP).

## Parcours

```
Photo → Upload → POST /api/diagnoses/suggest → Préremplissage UI → Confirmation user → POST /api/diagnoses
```

## Providers

| `VISION_PROVIDER` | Comportement |
|-------------------|--------------|
| `mock` (défaut) | Suggestion déterministe sans clé API |
| `openai` | Appel OpenAI Vision (`OPENAI_API_KEY` requis) |
| `off` | Pas de suggestion utile — choix manuel |

## API

`POST /api/diagnoses/suggest`

```json
{ "mediaId": "..." }
```

Réponse : `category`, `suggestedIssueCode`, `confidence`, `rationale`, `suggestionOnly: true`.

## Config

```bash
VISION_PROVIDER=mock   # ou openai
OPENAI_API_KEY=sk-...
MEDIA_SERVICE_URL=http://media-service:8083
```

## Statut

- [x] `VisionClient` + mock / openai / off
- [x] Endpoint suggest
- [x] UI suggestion pré-remplie éditable
- [x] Tests unitaires vision
- [ ] E2E suggest (optionnel)
- [ ] Clé OpenAI en environnement réel
