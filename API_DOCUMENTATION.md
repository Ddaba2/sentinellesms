# Sentinelle Mali — Documentation technique du backend

Backend Spring Boot de **Sentinelle Mali** (cahier des charges v2.0). API partagée entre l'application mobile
Android (Flutter) et le back-office web d'administration.

Hors périmètre de ce dépôt / de cette itération : **Bambara** et **fichiers audio** (module M11 — autre équipe).

## 1. Stack

| Composant | Choix |
|---|---|
| Framework | Spring Boot 4.1.0 (Java 17) |
| Base de données | PostgreSQL |
| Auth | JWT + refresh token opaque rotatif |
| Docs | springdoc-openapi (Swagger UI) |
| Passwords | BCrypt |

## 2. Démarrage

```bash
./mvnw.cmd spring-boot:run
```

Swagger : `http://localhost:8081/swagger-ui/index.html`

Au premier démarrage : rôles, compte super-admin, contenus FR (alertes / guide / conseils), motifs Mobile Money.

Variables d'environnement : voir `application.properties` (`DB_*`, `JWT_*`, `ADMIN_*`, rate limit, CORS).

## 3. Rôles (BO1)

| Rôle | Accès |
|---|---|
| `ROLE_USER` | Compte mobile |
| `ROLE_ANALYST` | Lecture BO (stats, listes) |
| `ROLE_MODERATOR` | + validation / rejet / fusion signalements, liens, numéros |
| `ROLE_ADMIN` | + règles, contenus, suspension users, audit |
| `ROLE_SUPER_ADMIN` | + attribution des rôles |

Le compte seed (`ADMIN_*`) reçoit `ROLE_SUPER_ADMIN`.

## 4. Endpoints publics (mobile)

| Méthode | Chemin | Module |
|---|---|---|
| POST | `/api/auth/*` | Auth |
| POST | `/api/analyze` | M1/M3/M5/M6 — score 0–100 + signaux |
| POST | `/api/links/check` | M2 — verdict SAFE / SUSPECT / DANGEROUS |
| GET | `/api/phones/lookup?number=` | M4 — réputation + historique |
| POST | `/api/reports` | M7 — signalement anonymisé (MESSAGE / LINK / PHONE) |
| GET | `/api/patterns/sync` | M12 — règles actives |
| GET | `/api/model/latest` | M12 — version moteur |
| GET | `/api/content/public?language=fr&type=` | M9/M10/M6 — contenus FR |

Types de contenu : `ALERT`, `EXPLANATION`, `VICTIM_GUIDE`, `PRIVACY_TIP`.

### Score de risque (M5)

| Score | Bande |
|---|---|
| 0–30 | FAIBLE |
| 31–60 | MODERE |
| 61–80 | ELEVE (alerte) |
| 81–100 | CRITIQUE (alerte) |

## 5. Endpoints back-office

### Signalements (BO3)

| Méthode | Chemin |
|---|---|
| GET | `/api/reports?status=&type=` |
| GET | `/api/reports/pending` |
| GET | `/api/reports/trending` |
| POST | `/api/reports/promote` |
| POST | `/api/reports/{id}/validate` |
| POST | `/api/reports/{id}/reject` |
| POST | `/api/reports/{id}/merge` |

Validation d'un signalement PHONE/LINK → met à jour la réputation communautaire (confiance 0–100).

### Liens & numéros

| Méthode | Chemin |
|---|---|
| GET/POST/PUT/DELETE | `/api/links` … |
| GET/DELETE | `/api/phones` … |

### Règles & modèle (BO4)

| Méthode | Chemin |
|---|---|
| CRUD | `/api/patterns` |
| GET/POST | `/api/model/versions`, `/api/model/latest` |

Champs pattern : `keywords`, `riskScore` (0–100), `signalCodes` (urgence, pin, otp, lien…).

### Contenu FR (BO6, sans Bambara/audio)

| Méthode | Chemin |
|---|---|
| CRUD | `/api/content` |
| GET | `/api/content/public` |

### Utilisateurs (BO5)

| Méthode | Chemin |
|---|---|
| GET | `/api/users?q=` |
| GET | `/api/users/{id}` |
| PUT | `/api/users/{id}/status` — suspension |
| PUT | `/api/users/{id}/roles` — super-admin |

### Stats & audit (BO2 / BO1)

| Méthode | Chemin |
|---|---|
| GET | `/api/statistics/overview` |
| GET | `/api/audit?username=` |

## 6. Confidentialité

- Aucun SMS brut stocké : métadonnées, hash de motif, numéro/URL normalisés uniquement.
- Signalements volontaires, anonymisables.
- Analyse locale privilégiée côté mobile ; `/api/analyze` est une assistance optionnelle / outil BO.
- Contenu Bambara + audio : hors scope (autre équipe).

## 7. Correspondance cahier des charges

| Module | Couverture API |
|---|---|
| M1–M3, M5–M6 | `/api/analyze` + sync patterns |
| M2 | `/api/links/check` |
| M4 | `/api/phones/lookup` |
| M7 | `/api/reports` |
| M8 | local mobile (N/A) |
| M9–M10 | `/api/content` (FR) |
| M11 | exclu (Bambara/audio) |
| M12 | `/api/patterns/sync`, `/api/model/latest` |
| BO1–BO6 | rôles, audit, stats, modération, règles, users, contenu |
