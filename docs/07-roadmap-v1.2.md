# Roadmap v1.2 — Passeport de réparation

**Statut :** cadrage produit (pas encore d’implémentation)  
**Base :** MVP livré sur `main` (parcours + vision suggestive + compte optionnel)  
**Objectif v1.2 :** renforcer la valeur perçue et la fiabilité, sans rouvrir le hors-scope (marketplace, OAuth, DIY, devis).

## Principes

1. **Impact utilisateur d’abord** — données et continuité de parcours avant polish infra.
2. **Compte reste optionnel** — ne jamais bloquer le parcours 30 secondes.
3. **Confirmation utilisateur = source de vérité** — la vision reste une suggestion.
4. **Une story = un incrément livrable** (PR petite, testable).

## Ordre d’exécution recommandé

| Vague | Stories | Pourquoi en premier |
|-------|---------|---------------------|
| **A — Continuité compte** | US-12, US-13 | Le compte devient utile dès le 1er diagnostic anonyme |
| **B — Confiance métier** | US-14, US-15 | L’utilisateur juge prix + réparateurs avant la techno |
| **C — Session & sécu** | US-16, US-17 | Évite les “déconnexions surprises” et abus email |
| **D — Vision calibrée** | US-18, US-19 | IA utile sans tromper |
| **E — Qualité & clarté** | US-20, US-21, US-22 | Confiance produit + dette docs/tests |

Hors vague v1.2 (garder en v2+) : OAuth, DIY, multi-villes GPS, marketplace, devis.

---

## EPIC-5 — Continuité compte (Vague A)

### US-12 — Rattacher mon dernier passeport anonyme après inscription / connexion

**En tant que** propriétaire qui vient de créer un compte (ou de se connecter),  
**je veux** rattacher le dernier diagnostic fait en anonyme,  
**afin de** le retrouver dans Mes passeports sans refaire le parcours.

**Priorité :** Must (v1.2)  
**Labels :** auth, diagnosis, frontend

**Critères d’acceptation**
1. Given un diagnostic anonyme en session (`diagnosisId`), When je m’inscris ou me connecte, Then ce diagnostic est rattaché à mon `userId`.
2. Given le rattachement réussi, When j’ouvre Mes passeports, Then le passeport apparaît.
3. Given le diagnostic appartient déjà à un autre compte, When je tente le claim, Then l’API refuse (403/409) sans fuite d’info.
4. Given aucun diagnostic anonyme en session, When je me connecte, Then le parcours compte fonctionne normalement.

**Notes techniques (indicatif)**
- Stocker `diagnosisId` côté front (session) après résultat anonyme.
- `POST /api/diagnoses/{id}/claim` (Bearer) — set `userId` si `userId` était null.

---

### US-13 — Enrichir Mes passeports (date, verdict, rouvrir)

**En tant que** utilisateur connecté,  
**je veux** voir une liste claire de mes passeports (date, appareil, verdict),  
**afin de** retrouver rapidement un diagnostic et le rouvrir.

**Priorité :** Must (v1.2)  
**Labels :** frontend, diagnosis

**Critères d’acceptation**
1. Given j’ai plusieurs passeports, When j’ouvre `/mes-passeports`, Then ils sont triés du plus récent au plus ancien avec date visible.
2. Given un item, When je clique, Then j’arrive sur l’écran résultat (estimation + réparateurs si supporté).
3. Given la liste est vide, When la page charge, Then un CTA mène à une nouvelle estimation.

---

## EPIC-6 — Confiance métier (Vague B)

### US-14 — Fiabiliser la grille de prix (sources & fourchettes)

**En tant que** propriétaire,  
**je veux** des fourchettes de réparation / remplacement plus crédibles,  
**afin de** prendre une décision sans me sentir “mocké”.

**Priorité :** Must (v1.2)  
**Labels :** diagnosis, product

**Critères d’acceptation**
1. Given chaque `IssueCode` supporté, When je consulte l’estimation, Then low/high/replacement sont documentés (commentaire ou fiche produit).
2. Given la règle des 70 %, When le mid-repair dépasse 70 % du remplacement, Then le verdict devient REPLACE (régression OK).
3. Given le disclaimer, When le résultat s’affiche, Then il reste visible et non ambigu.

**Notes**
- Pas besoin d’une API marché : une revue manuelle + versionnage du catalogue suffit.
- Option : champ `pricingVersion` dans la réponse diagnostic.

---

### US-15 — Enrichir l’annuaire (plus de réparateurs / diversité)

**En tant que** propriétaire à Lyon,  
**je veux** davantage de réparateurs pertinents par catégorie,  
**afin d’avoir un vrai choix de contact.**

**Priorité :** Should (v1.2)  
**Labels :** repairer

**Critères d’acceptation**
1. Given lave-linge / lave-vaisselle / four, When je liste les réparateurs Lyon, Then au moins 3 fiches actives par catégorie (ou justification produit si moins).
2. Given une fiche, When je contacte, Then tel / email / WhatsApp restent en un clic.
3. Given une catégorie sans réparateur, When la liste est vide, Then le message vide reste explicite.

---

## EPIC-7 — Session & sécurité (Vague C)

### US-16 — Renouveler ma session sans me reconnecter

**En tant que** utilisateur connecté,  
**je veux** que mon access token soit renouvelé via refresh,  
**afin de** continuer Mes passeports / un diagnostic sans erreur 401 surprise.

**Priorité :** Must (v1.2)  
**Labels :** auth, frontend

**Critères d’acceptation**
1. Given mon access token a expiré et mon refresh est valide, When j’appelle une API protégée, Then le front refresh puis réessaie une fois.
2. Given le refresh est révoqué / expiré, When le renouvellement échoue, Then je suis renvoyé vers `/connexion` avec session nettoyée.
3. Given je me déconnecte, When le refresh est révoqué, Then il ne peut plus être réutilisé.

---

### US-17 — Limiter les abus sur login et mot de passe oublié

**En tant que** opérateur du service,  
**je veux** un rate-limit basique sur login / forgot-password,  
**afin de** limiter le brute-force et le spam d’emails.

**Priorité :** Should (v1.2)  
**Labels :** auth, security

**Critères d’acceptation**
1. Given trop de tentatives login échouées depuis la même IP (seuil défini), When je réessaie, Then 429 + message clair.
2. Given trop de `forgot-password` pour le même email / IP, When je réessaie, Then 429 (sans révéler si l’email existe).
3. Given un usage normal, When je me connecte, Then aucun blocage.

---

## EPIC-8 — Vision calibrée (Vague D)

### US-18 — Ne préremplir que si la confiance est suffisante

**En tant que** propriétaire,  
**je veux** que la suggestion IA ne s’impose pas si elle est incertaine,  
**afin de** ne pas être biaisé par une mauvaise catégorie.

**Priorité :** Should (v1.2)  
**Labels :** diagnosis, frontend, vision

**Critères d’acceptation**
1. Given `confidence < seuil` (ex. 0.55), When la suggestion revient, Then aucune catégorie n’est forcée ; message “choisis manuellement”.
2. Given `confidence ≥ seuil`, When la suggestion revient, Then préremplissage actuel (éditable).
3. Given provider `off` ou erreur suggest, When l’upload a réussi, Then le parcours manuel reste possible.

---

### US-19 — Jeu de photos de référence pour valider la vision

**En tant que** PO / QA,  
**je veux** un petit corpus de photos + résultats attendus,  
**afin de** vérifier mock/openai sans se fier au feeling.

**Priorité :** Could (v1.2)  
**Labels :** qa, vision

**Critères d’acceptation**
1. Given 10–20 images de test (repo ou dossier `product/vision-fixtures/`), When je lance une checklist manuelle, Then catégorie attendue / issue attendue sont documentées.
2. Given le provider mock, When je rejoue les fixtures, Then le comportement reste déterministe.
3. Given openai (si clé), When je rejoue un sous-ensemble, Then les écarts sont notés (pas de blocage CI obligatoire en v1.2).

---

## EPIC-9 — Clarté & qualité (Vague E)

### US-20 — Expliquer le verdict en une phrase

**En tant que** propriétaire,  
**je veux** comprendre pourquoi le verdict est réparer / à arbitrer / remplacer,  
**afin de** faire confiance à la recommandation.

**Priorité :** Should (v1.2)  
**Labels :** frontend, diagnosis

**Critères d’acceptation**
1. Given un diagnostic supporté, When le résultat s’affiche, Then une phrase explique le verdict (ex. “réparation estimée nettement sous le remplacement”).
2. Given UNSUPPORTED, When le résultat s’affiche, Then pas de fausse explication de verdict €.

---

### US-21 — Aligner la doc produit sur le compte optionnel

**En tant que** lecteur du repo / recruteur / coéquipier,  
**je veux** que persona et user stories reflètent le compte optionnel,  
**afin d’éviter l’écart docs ↔ produit.**

**Priorité :** Must (v1.2)  
**Labels :** docs

**Critères d’acceptation**
1. Given `docs/03-user-stories.md` et `product/user-stories-mvp.json`, When je lis la persona, Then le compte optionnel / historique est mentionné (anonyme possible).
2. Given le hors-scope, When je lis “compte utilisateur”, Then il est clarifié : *compte obligatoire* / OAuth restent hors scope, compte optionnel est livré.
3. Given le README roadmap, When je consulte v1.2, Then elle pointe vers ce document.

---

### US-22 — E2E auth minimal (register → confirm → login → mine)

**En tant que** équipe,  
**je veux** un scénario E2E auth via la gateway,  
**afin de** ne pas régresser le compte à chaque merge.

**Priorité :** Should (v1.2)  
**Labels :** e2e, auth

**Critères d’acceptation**
1. Given la stack Docker up, When le test E2E auth tourne, Then register + confirm (token récupérable en mode test/log) + login + `/diagnoses/mine` passent.
2. Given un diagnostic créé avec Bearer, When `/mine` est appelé, Then le diagnostic apparaît.
3. Given le parcours anonyme E2E existant, When les tests tournent, Then pas de régression.

---

## Définition of Done (story v1.2)

- Critères d’acceptation validés (manuel et/ou auto)
- Pas de régression sur Photo → Confirmation → Résultat → Contact (anonyme)
- Compte toujours optionnel
- Doc / README mis à jour si la story change le contrat API ou le parcours
- PR petite, mergeable seule

## Suivi d’avancement

| ID | Story | Vague | Statut |
|----|-------|-------|--------|
| US-12 | Claim passeport anonyme | A | Fait |
| US-13 | Liste Mes passeports enrichie | A | À faire |
| US-14 | Grille de prix fiabilisée | B | À faire |
| US-15 | Annuaire enrichi | B | À faire |
| US-16 | Refresh session front | C | À faire |
| US-17 | Rate-limit auth | C | À faire |
| US-18 | Seuil de confiance vision | D | À faire |
| US-19 | Fixtures vision | D | À faire |
| US-20 | Phrase d’explication verdict | E | À faire |
| US-21 | Alignement docs | E | À faire |
| US-22 | E2E auth | E | Fait |

## Fichiers associés

- Stories JSON : [`product/user-stories-v1.2.json`](../product/user-stories-v1.2.json)
- Cadrage compte : [`docs/06-compte-utilisateur.md`](06-compte-utilisateur.md)
- MVP stories : [`docs/03-user-stories.md`](03-user-stories.md)
