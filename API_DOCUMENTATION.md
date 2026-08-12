# SentinelleSMS — Documentation technique du backend

Backend Spring Boot du projet **SentinelleSMS** (candidature JNC 2026 — Détection embarquée des fraudes Mobile
Money par IA). Ce document couvre uniquement le backend : réception des signalements anonymisés, agrégation, et
distribution des mises à jour de détection vers l'application mobile (cf. cahier des charges §4.2).

L'application mobile Flutter (lecture des SMS, classification embarquée, alertes) n'est pas dans ce dépôt.

## 1. Stack technique

| Composant | Choix |
|---|---|
| Framework | Spring Boot 4.1.0 (Java 17) |
| Base de données | PostgreSQL |
| Authentification | JWT (access token) + refresh token opaque rotatif |
| Documentation API | springdoc-openapi (Swagger UI) |
| Mots de passe | BCrypt |

## 2. Lancer le projet

### Prérequis
- Java 17
- Une base PostgreSQL accessible (créée au préalable, ex. `CREATE DATABASE sentinellesms;`)

### Configuration (`application.properties`, surchargeable par variables d'environnement)

| Variable d'env | Défaut (dev) | Rôle |
|---|---|---|
| `SERVER_PORT` | `8081` | Port HTTP de l'API |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sentinellesms` | URL JDBC |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `root` | Identifiants PostgreSQL |
| `DDL_AUTO` | `update` | Stratégie Hibernate (création/màj auto des tables) |
| `JWT_SECRET` | valeur de dev | Clé de signature des access tokens — **à changer en prod** |
| `JWT_ACCESS_EXP_MS` | `900000` (15 min) | Durée de vie de l'access token |
| `JWT_REFRESH_EXP_MS` | `2592000000` (30 j) | Durée de vie du refresh token |
| `CORS_ALLOWED_ORIGINS` | `*` | Origines autorisées (liste séparée par virgules) |
| `RATE_LIMIT_CAPACITY` | `20` | Requêtes autorisées par IP et par fenêtre sur les endpoints publics |
| `RATE_LIMIT_WINDOW_SECONDS` | `60` | Durée de la fenêtre de rate limiting |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin` / `admin@sentinellesms.local` / `ChangeMe123!` | Compte admin créé automatiquement au premier démarrage |

### Démarrage

```
./mvnw.cmd spring-boot:run
```

Au premier démarrage, `DataInitializer` crée automatiquement les rôles `ROLE_USER`/`ROLE_ADMIN` et le compte admin
par défaut si aucun compte n'existe déjà avec ce nom/email. **Changer le mot de passe admin par défaut avant toute
mise en production.**

### Documentation interactive (Swagger)

Une fois l'application démarrée :
- `http://localhost:8081/swagger-ui/index.html` — interface interactive
- `http://localhost:8081/v3/api-docs` — spécification OpenAPI brute (JSON)

## 3. Authentification

Auth stateless par JWT. Le token d'accès est court (15 min par défaut) et se renouvelle via le refresh token
(opaque, stocké en base, à usage unique — chaque `/refresh` révoque l'ancien et en émet un nouveau).

```
Authorization: Bearer <accessToken>
```

Deux rôles : `ROLE_USER` (créé à l'inscription) et `ROLE_ADMIN` (gestion des motifs, versions du modèle,
statistiques, modération des signalements).

## 4. Endpoints

### `/api/auth` — public

| Méthode | Chemin | Description |
|---|---|---|
| POST | `/register` | Crée un compte (`ROLE_USER`), retourne les tokens |
| POST | `/login` | Authentifie par username/email + mot de passe |
| POST | `/refresh` | Échange un refresh token valide contre une nouvelle paire de tokens |
| POST | `/logout` | Révoque un refresh token |

### `/api/users`

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| GET | `/me` | Authentifié | Profil de l'utilisateur courant |
| GET | `/` | Admin | Liste des comptes |

### `/api/reports` — signalement communautaire (F4)

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| POST | `/` | Public | Crée un signalement. Aucune donnée brute de SMS n'est transmise : uniquement `language`, `category`, `riskLevel`, `reasonCodes`, `patternHash`, `modelVersion`. Anonyme par défaut ; rattaché au compte si un token valide est fourni et `anonymous=false`. |
| GET | `/` | Admin | Liste tous les signalements |
| GET | `/trending` | Admin | Signalements non traités regroupés par `patternHash`/`category`/`riskLevel`/`language`, triés par nombre d'occurrences décroissant — sert à repérer les fraudes émergentes non encore couvertes par un motif |
| POST | `/promote` | Admin | Transforme un cluster de signalements (par `patternHash`) en un nouveau `FraudPattern` actif ; marque les signalements correspondants comme traités (`reviewed=true`) |

### `/api/patterns` — motifs de fraude connus (F2/F5)

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| GET | `/sync?language=fr` | Public | Motifs actifs à synchroniser sur l'appareil (filtrable par langue) |
| GET | `/` | Admin | Tous les motifs (actifs et inactifs) |
| POST | `/` | Admin | Crée un motif |
| PUT | `/{id}` | Admin | Met à jour un motif |
| DELETE | `/{id}` | Admin | Supprime un motif |

### `/api/model` — versions du modèle embarqué (F5)

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| GET | `/latest` | Public | Dernière version active du modèle + motifs actifs (payload de synchronisation complet pour l'app) |
| GET | `/versions` | Admin | Historique des versions |
| POST | `/versions` | Admin | Publie une nouvelle version (désactive automatiquement la précédente) |

### `/api/statistics` — tableau de bord

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| GET | `/overview` | Admin | Totaux de signalements, répartition par risque/catégorie/langue |

## 5. Boucle signalement → motif

Le cahier des charges décrit un cycle : les signalements communautaires (F4) doivent progressivement enrichir les
motifs de détection distribués aux appareils (F5). Comme aucune donnée brute de SMS ne remonte jamais au serveur
(contrainte de confidentialité du CDC §5), cette transformation ne peut pas être totalement automatique — un motif
nécessite un `label`/`keywords`/`description` lisibles, qui n'existent que sur l'appareil de la personne qui
signale.

Le workflow retenu :
1. Chaque signalement porte un `patternHash` (empreinte du motif détecté localement, ou vide si l'app n'a rien
   détecté).
2. `GET /api/reports/trending` fait remonter aux administrateurs les `patternHash` les plus fréquemment signalés et
   non encore traités.
3. Un administrateur examine le cluster et appelle `POST /api/reports/promote` avec un `label`/`keywords`
   descriptifs : cela crée le `FraudPattern` correspondant (repris automatiquement : `category`, `riskLevel`,
   `language` du cluster) et marque tous les signalements de ce cluster comme traités.

Ce contrôle humain sert aussi de garde-fou contre l'empoisonnement de données (signalements en masse forgés pour
biaiser les futurs motifs), sujet d'autant plus pertinent que ce projet est présenté dans un cadre cybersécurité.

## 6. Anti-abus (rate limiting)

Les endpoints accessibles sans authentification sont, par nature, ouverts à n'importe qui. Un filtre limite les
requêtes par IP sur :

- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`
- `POST /api/reports`
- `GET /api/patterns/sync`
- `GET /api/model/latest`

Par défaut : 20 requêtes par IP et par endpoint sur une fenêtre glissante de 60 secondes (`RATE_LIMIT_CAPACITY` /
`RATE_LIMIT_WINDOW_SECONDS`). Au-delà, l'API répond `429 Too Many Requests` avec un en-tête `Retry-After`.

Limite connue : le compteur est en mémoire locale au processus (adapté à une instance unique, comme pour une démo
ou un MVP). Un déploiement multi-instances nécessiterait un backend partagé (Redis) pour que la limite soit
cohérente entre instances.

## 7. Modèle de données (résumé)

| Entité | Rôle |
|---|---|
| `User` / `Role` | Comptes et rôles (`ROLE_USER`, `ROLE_ADMIN`) |
| `RefreshToken` | Refresh tokens opaques, rotatifs, révocables |
| `Report` | Signalement communautaire anonymisé (jamais de contenu brut de SMS) ; `reviewed` marque s'il a été transformé en motif |
| `FraudPattern` | Motif de fraude connu (mots-clés, catégorie, niveau de risque, langue) distribué à l'app |
| `ModelVersion` | Version du modèle embarqué (TFLite/ONNX) ; une seule version `active` à la fois |

## 8. Limites connues / hors périmètre de ce backend

- `ModelVersion.downloadUrl` référence un fichier modèle (TFLite/ONNX) hébergé **ailleurs** (CDN, stockage
  externe...) — ce backend ne stocke/sert pas lui-même le binaire du modèle.
- Aucun test automatisé au-delà du test de contexte Spring par défaut.
- Le jeu de données annoté et l'entraînement du modèle IA sont un livrable séparé, hors backend.
- L'application mobile Flutter (F1, F2 embarqué, F3, F6, F7) reste entièrement à développer.
