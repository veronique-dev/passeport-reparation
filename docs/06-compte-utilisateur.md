# Création de compte utilisateur — cadrage

**Branche :** `feature/creation-compte-utilisateur`  
**Statut :** décisions produit figées — implémentation à démarrer après validation

## 1. Décisions produit

| # | Décision | Choix |
|---|----------|--------|
| 1 | Compte pour diagnostiquer | **Optionnel** — parcours anonyme conservé |
| 2 | Contenu du compte | **Profil + historique** des diagnostics (revoir / modifier) |
| 3 | Email | **Confirmation d’inscription + reset mot de passe dès v1** |
| 4 | Architecture | Nouveau service **`auth-service`** + DB **`auth_db`** |

## 2. Stack retenue (Option A)

| Élément | Choix |
|---------|--------|
| Service | `services/auth-service` (Spring Boot 3.3, Java 21) |
| Sécurité | Spring Security + BCrypt |
| Tokens | JWT access (court, ~15 min) + refresh token opaque en DB (~7–30 j) |
| Données | Postgres `auth_db` |
| Email | SMTP configurable (`spring.mail` / Mailhog en local) |
| Front | Angular : Inscription, Connexion, Mot de passe oublié, Mon compte, Mes passeports |

## 3. Périmètre v1

### Auth / profil
- Inscription email + mot de passe (+ prénom optionnel)
- Email de confirmation (lien à usage unique, expiration)
- Connexion → access JWT + refresh
- Déconnexion (révocation refresh)
- Mot de passe oublié / reset via email
- `GET /api/auth/me` — profil
- `PATCH /api/auth/me` — modifier profil (prénom, etc.)

### Historique diagnostics
- Si connecté au moment du `POST /api/diagnoses` → rattacher `userId`
- `GET /api/diagnoses/mine` — liste des passeports de l’utilisateur
- `GET /api/diagnoses/{id}` — détail (owner ou anonyme via id connu)
- Modification limitée v1 : **re-créer** un diagnostic corrigé (catégorie/panne) plutôt qu’éditer librement les prix ; option : `PATCH` catégorie/issue → recalcul estimation

### Hors v1 (reporté)
- OAuth Google / Apple
- Compte obligatoire
- Marketplace, rôles admin
- Suppression RGPD automatisée complète (prévoir endpoint plus tard)

## 4. API (via gateway)

| Méthode | Chemin | Service | Auth |
|---------|--------|---------|------|
| `POST` | `/api/auth/register` | auth | public |
| `POST` | `/api/auth/login` | auth | public |
| `POST` | `/api/auth/refresh` | auth | public (refresh) |
| `POST` | `/api/auth/logout` | auth | refresh / Bearer |
| `GET` | `/api/auth/confirm?token=` | auth | public |
| `POST` | `/api/auth/forgot-password` | auth | public |
| `POST` | `/api/auth/reset-password` | auth | public |
| `GET` | `/api/auth/me` | auth | Bearer |
| `PATCH` | `/api/auth/me` | auth | Bearer |
| `GET` | `/api/diagnoses/mine` | diagnosis | Bearer |
| `POST` | `/api/diagnoses` | diagnosis | public **ou** Bearer (si présent → `userId`) |

## 5. Modèle de données

**auth_db**
- `users` — id, email (unique), password_hash, first_name, email_verified, created_at, updated_at
- `email_tokens` — id, user_id, type (`CONFIRM` \| `RESET`), token_hash, expires_at, used_at
- `refresh_tokens` — id, user_id, token_hash, expires_at, revoked_at

**diagnosis_db.diagnoses** (évolution)
- `user_id` (UUID nullable) — null = anonyme

## 6. Sécurité (principes)

- Parcours anonyme inchangé sans header `Authorization`
- Gateway route `/api/auth/**` → auth-service
- diagnosis-service valide le JWT (clé partagée / JWKS simple) **uniquement** pour `/mine` et pour rattacher `userId` à la création
- Mots de passe : BCrypt, politique min. 8 caractères
- Tokens email : one-shot, TTL court (confirm ~24 h, reset ~1 h)
- Rate-limit basique sur login / forgot (à prévoir)
- Local : Mailhog (ou log du lien dans les logs si SMTP off)

## 7. UX front

- Header : **Connexion** / **Créer un compte** (si anonyme) · **Mon compte** (si connecté)
- Après diagnostic connecté : « Enregistré dans Mes passeports »
- Après diagnostic anonyme : CTA soft « Crée un compte pour retrouver ce passeport » (pas bloquant)
- Pages : `/connexion`, `/inscription`, `/mot-de-passe-oublie`, `/compte`, `/mes-passeports`

## 8. Infra

- `init-databases.sql` → `CREATE DATABASE auth_db;`
- `docker-compose` : service `auth-service` + Mailhog (optionnel)
- Variables : `JWT_SECRET`, `MAIL_*`, `APP_FRONTEND_URL` (liens email)

## 9. Stories cibles (à détailler)

| ID | Story |
|----|--------|
| US-A01 | Créer un compte (email + mdp) |
| US-A02 | Confirmer mon email |
| US-A03 | Me connecter / me déconnecter |
| US-A04 | Réinitialiser mon mot de passe |
| US-A05 | Voir / modifier mon profil |
| US-A06 | Voir l’historique de mes passeports |
| US-A07 | Diagnostiquer sans compte (régression) |
| US-A08 | Diagnostiquer connecté → passeport rattaché |

## 10. Plan d’implémentation suggéré

1. Scaffold `auth-service` + DB + routes gateway  
2. Register / login / JWT / me  
3. Email confirm + forgot/reset (Mailhog)  
4. `user_id` sur diagnoses + `/mine`  
5. UI Angular auth + Mes passeports  
6. Tests unitaires + Postman + doc archi  

---

## Statut

- [x] Décisions produit figées
- [x] `auth-service` + JWT + refresh + BCrypt
- [x] Confirm email + forgot/reset (Mailhog / logs)
- [x] `user_id` sur diagnoses + `GET /api/diagnoses/mine`
- [x] UI Angular auth + Mes passeports
- [ ] E2E auth (optionnel)
- [ ] Rate-limit login / forgot