# User stories — MVP Passeport de réparation

**Rôle :** Product Owner  
**Version :** MVP 1.0  
**Persona principale :** Propriétaire d’un appareil électroménager en panne (anonyme, sans compte)  
**Objectif produit :** En moins d’une minute, décider si ça vaut le coup de réparer et contacter un réparateur local.

## Parcours cible

```
Photo → Catégorie + panne → Verdict € → Contacter un réparateur
```

## Épics

| Épic | Nom | Stories |
|------|-----|---------|
| EPIC-1 | Capture & confirmation | US-01 → US-04 |
| EPIC-2 | Estimation & verdict | US-05 → US-07 |
| EPIC-3 | Annuaire réparateurs | US-08 → US-09 |
| EPIC-4 | Parcours résultat | US-10 → US-11 |

## Priorisation MoSCoW

Toutes les stories ci-dessous sont **Must** pour le MVP livré. Les items **Won’t** sont listés en fin de document.

---

## EPIC-1 — Capture & confirmation

### US-01 — Importer une photo de l’appareil

**En tant que** propriétaire d’un appareil cassé,  
**je veux** importer une photo de mon appareil,  
**afin de** contextualiser mon demande de passeport de réparation.

**Priorité :** Must  
**Labels :** frontend, media

**Critères d’acceptation**
1. Given je suis sur l’accueil, When j’importe un fichier JPEG/PNG/WEBP, Then un aperçu de la photo s’affiche.
2. Given une photo est sélectionnée, When je clique sur « Changer », Then je peux en choisir une autre.
3. Given un type de fichier non supporté côté API, When l’upload échoue, Then un message d’erreur explicite s’affiche.
4. Given l’API média est indisponible, When je lance l’estimation, Then un message indique que l’API est indisponible.

---

### US-02 — Confirmer la catégorie d’appareil

**En tant que** propriétaire,  
**je veux** indiquer moi-même la catégorie de mon appareil,  
**afin d’éviter** une mauvaise classification automatique.

**Priorité :** Must  
**Labels :** frontend, diagnosis

**Critères d’acceptation**
1. Given une photo est importée, When l’écran de confirmation s’affiche, Then je vois les choix : Lave-linge, Lave-vaisselle, Four, Autre.
2. Given je sélectionne une catégorie supportée, When le choix est validé, Then la liste des symptômes correspondants est chargée.
3. Given aucune catégorie n’est sélectionnée, When j’essaie d’obtenir mon passeport, Then le bouton reste désactivé.

---

### US-03 — Sélectionner le type de panne

**En tant que** propriétaire,  
**je veux** choisir le symptôme le plus proche de ma panne,  
**afin d’obtenir** une estimation de coût plus réaliste.

**Priorité :** Must  
**Labels :** frontend, diagnosis, pricing

**Critères d’acceptation**
1. Given une catégorie supportée est choisie, When l’API `/api/diagnoses/issues` répond, Then je vois une liste de pannes (ex. pompe de vidange, joint de porte).
2. Given la liste est chargée, When aucune panne n’est sélectionnée, Then le bouton « Obtenir mon passeport » reste désactivé.
3. Given je sélectionne une panne, When je lance l’estimation, Then le `issueCode` correspondant est envoyé à l’API.

---

### US-04 — Déclarer un appareil hors périmètre

**En tant que** propriétaire,  
**je veux** pouvoir indiquer que mon appareil n’est pas dans le périmètre MVP,  
**afin de** ne pas recevoir une estimation trompeuse.

**Priorité :** Must  
**Labels :** frontend, diagnosis

**Critères d’acceptation**
1. Given je choisis « Autre », When l’écran se met à jour, Then un message explique que la catégorie est hors périmètre.
2. Given « Autre » est sélectionné, When j’obtiens mon passeport, Then le résultat indique « Hors périmètre » sans fourchette de prix ni liste de réparateurs.
3. Given un fer à repasser (ou tout hors scope), When je choisis « Autre », Then je ne vois jamais un verdict de type four/lave-linge.

---

## EPIC-2 — Estimation & verdict

### US-05 — Obtenir une estimation de coût

**En tant que** propriétaire,  
**je veux** voir une fourchette de réparation et un ordre de grandeur de remplacement,  
**afin de** décider rapidement si la réparation est rentable.

**Priorité :** Must  
**Labels :** diagnosis, pricing

**Critères d’acceptation**
1. Given catégorie + panne supportées, When `POST /api/diagnoses` réussit, Then la réponse contient `repairLow`, `repairHigh`, `replacementApprox` en EUR.
2. Given deux pannes différentes d’une même catégorie, When je compare les estimations, Then les fourchettes peuvent différer (grille par panne).
3. Given une panne simple (ex. joint de porte four), When j’obtiens l’estimation, Then la fourchette est plus basse qu’une panne carte électronique.

---

### US-06 — Voir le verdict réparer / à arbitrer / remplacer

**En tant que** propriétaire,  
**je veux** un verdict simple,  
**afin de** savoir quoi faire sans lire un devis technique.

**Priorité :** Must  
**Labels :** diagnosis, frontend

**Critères d’acceptation**
1. Given une estimation supportée, When j’arrive sur le résultat, Then le verdict affiché est l’un de : Réparer, À arbitrer, Remplacer.
2. Given le milieu de fourchette dépasse 70 % du coût de remplacement, When le verdict est calculé, Then le système force « Remplacer ».
3. Given une panne catalogue avec verdict par défaut, When le ratio est ≤ 70 %, Then le verdict catalogue est conservé.

---

### US-07 — Afficher le disclaimer d’estimation

**En tant que** propriétaire,  
**je veux** être informé que l’estimation n’est pas un devis,  
**afin de** ne pas confondre l’outil avec un engagement commercial.

**Priorité :** Must  
**Labels :** diagnosis, frontend, legal

**Critères d’acceptation**
1. Given un diagnostic est affiché, When je consulte le passeport, Then un disclaimer indique que l’estimation est indicative.
2. Given le disclaimer, When je lis le texte, Then il précise qu’un réparateur confirmera sur place.

---

## EPIC-3 — Annuaire réparateurs

### US-08 — Voir les réparateurs de la zone test

**En tant que** propriétaire,  
**je veux** voir une liste de réparateurs locaux pour ma catégorie,  
**afin de** savoir qui contacter.

**Priorité :** Must  
**Labels :** repairer, frontend

**Critères d’acceptation**
1. Given un diagnostic supporté (ex. lave-linge), When le résultat s’affiche, Then je vois des réparateurs de la zone test Lyon filtrés par catégorie.
2. Given aucun réparateur ne correspond, When la liste est vide, Then un message « aucun réparateur trouvé » s’affiche.
3. Given un diagnostic hors périmètre, When le résultat s’affiche, Then la section réparateurs n’apparaît pas.

---

### US-09 — Contacter un réparateur en un clic

**En tant que** propriétaire,  
**je veux** appeler, écrire ou ouvrir WhatsApp depuis la fiche,  
**afin de** passer à l’action sans créer de compte.

**Priorité :** Must  
**Labels :** repairer, frontend

**Critères d’acceptation**
1. Given un réparateur a un téléphone, When je clique « Appeler », Then le lien `tel:` s’ouvre.
2. Given un email est présent, When je clique « Email », Then le lien `mailto:` s’ouvre.
3. Given un WhatsApp est présent, When je clique « WhatsApp », Then le lien `wa.me` s’ouvre dans un nouvel onglet.
4. Given le MVP, When je consulte l’annuaire, Then il n’y a ni réservation, ni paiement, ni matching marketplace.

---

## EPIC-4 — Parcours résultat

### US-10 — Consulter mon passeport de réparation

**En tant que** propriétaire,  
**je veux** un écran résultat clair (photo, appareil, panne, coûts, verdict),  
**afin de** comprendre la décision en un coup d’œil.

**Priorité :** Must  
**Labels :** frontend, ux

**Critères d’acceptation**
1. Given un diagnostic réussi, When je suis redirigé vers `/resultat`, Then je vois la photo, le libellé appareil, le symptôme, les coûts et le verdict.
2. Given je rafraîchis ou j’ouvre `/resultat` sans diagnostic en session, When la page charge, Then je suis renvoyé vers l’accueil.
3. Given le diagnostic vient d’une confirmation utilisateur, When le résultat s’affiche, Then un libellé indique que c’est confirmé par l’utilisateur.

---

### US-11 — Relancer une nouvelle estimation

**En tant que** propriétaire,  
**je veux** recommencer avec une nouvelle photo,  
**afin de** traiter un autre appareil ou corriger mon choix.

**Priorité :** Must  
**Labels :** frontend

**Critères d’acceptation**
1. Given je suis sur le résultat, When je clique « Nouvelle photo », Then la session résultat est effacée et je reviens à l’accueil.
2. Given je reviens à l’accueil, When le formulaire s’affiche, Then aucune catégorie/panne précédente n’est pré-sélectionnée de façon bloquante.

---

## Hors scope MVP (Won’t / plus tard)

| Item | Raison |
|------|--------|
| Reconnaissance IA automatique de l’objet | Remplacée par confirmation utilisateur en MVP |
| Compte utilisateur / authentification | Non nécessaire au parcours décisionnel |
| Devis en ligne, paiement, réservation | Marketplace hors scope |
| Tutoriels DIY | Roadmap v1.1 |
| Toutes les catégories d’appareils | Périmètre limité à 3 familles + Autre |
| Géolocalisation GPS réelle multi-villes | Zone test Lyon seedée |
| Suivi de réparation | Roadmap v2 |

## Définition of Done (story)

- Critères d’acceptation validés manuellement sur le parcours UI
- API concernée joignable via gateway (`http://localhost:8090`)
- Pas de régression sur le parcours Photo → Confirmation → Résultat → Contact
- Documentation produit à jour (`product/user-stories-mvp.json`)

## Fichiers associés

- JSON structuré : [`product/user-stories-mvp.json`](../product/user-stories-mvp.json)
- CSV (Jira / Excel) : [`product/user-stories-mvp.csv`](../product/user-stories-mvp.csv)
