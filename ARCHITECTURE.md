# ConnectSphere Architecture

This document maps the current project from React frontend to API gateway, backend microservices, databases, search, media storage, messaging, and email.

## High-Level System Diagram

```mermaid
flowchart TB
    U[User Browser]
    FE[React Frontend<br/>connectsphere-frontend<br/>Vite dev server :3000]
    GW[Spring Cloud API Gateway<br/>api-gateway :8090<br/>CORS + JWT filter + route proxy]

    U --> FE
    FE -->|/api/* via Vite proxy| GW

    subgraph Backend["Spring Boot Microservices"]
        AUTH[Auth Service :8081<br/>users, login, JWT, OAuth, profile, admin users]
        POST[Post Service :8082<br/>posts, feed, counters, visibility]
        COMMENT[Comment Service :8083<br/>comments, threaded replies]
        LIKE[Like Service :8084<br/>likes + reactions]
        FOLLOW[Follow Service :8085<br/>followers, following, suggestions]
        NOTIF[Notification Service :8086<br/>notifications + broadcast]
        MEDIA[Media Service :8087<br/>uploads, media, stories]
        SEARCH[Search Service :8088<br/>post/user search, hashtags, trending]
        FORUM[Forum Service :8089<br/>forums, posts, moderators, bans]
    end

    GW -->|/api/auth/**| AUTH
    GW -->|/api/posts/**| POST
    GW -->|/api/comments/**| COMMENT
    GW -->|/api/likes/**| LIKE
    GW -->|/api/follows/**| FOLLOW
    GW -->|/api/notifications/**| NOTIF
    GW -->|/api/media/**, /api/stories/**| MEDIA
    GW -->|/api/search/**, /api/hashtags/**| SEARCH
    GW -->|/api/forums/**| FORUM

    subgraph MySQL["MySQL :3306 - Database per Service"]
        DB_AUTH[(connectsphere_auth<br/>users)]
        DB_POST[(connectsphere_post<br/>posts, post_media_urls)]
        DB_COMMENT[(connectsphere_comment<br/>comments)]
        DB_LIKE[(connectsphere_like<br/>likes)]
        DB_FOLLOW[(connectsphere_follow<br/>follows)]
        DB_NOTIF[(connectsphere_notification<br/>notifications)]
        DB_MEDIA[(connectsphere_media<br/>media, stories)]
        DB_SEARCH[(connectsphere_search<br/>hashtags, post_hashtags)]
        DB_FORUM[(connectsphere_forum<br/>forums, forum_posts,<br/>forum_moderators, forum_bans)]
    end

    AUTH --> DB_AUTH
    POST --> DB_POST
    COMMENT --> DB_COMMENT
    LIKE --> DB_LIKE
    FOLLOW --> DB_FOLLOW
    NOTIF --> DB_NOTIF
    MEDIA --> DB_MEDIA
    SEARCH --> DB_SEARCH
    FORUM --> DB_FORUM

    ES[(Elasticsearch :9200<br/>indexes: posts, users)]
    CLOUD[(Cloudinary<br/>image/file storage)]
    MQ[(ActiveMQ :61616<br/>account.deactivation, forum.mail)]
    SMTP[(Gmail SMTP :587)]
    GOOGLE[Google OAuth<br/>tokeninfo API]

    SEARCH --> ES
    MEDIA --> CLOUD
    POST --> CLOUD
    AUTH --> MQ
    FORUM --> MQ
    AUTH --> SMTP
    NOTIF --> SMTP
    FORUM --> SMTP
    AUTH --> GOOGLE
```

## Gateway Routing

```mermaid
flowchart LR
    FE[React App<br/>Axios baseURL /api] --> GW[API Gateway :8090]

    GW -->|public + protected<br/>/api/auth/**| AUTH[Auth :8081]
    GW -->|JWT protected except public/search<br/>/api/posts/**| POST[Post :8082]
    GW -->|JWT protected<br/>/api/comments/**| COMMENT[Comment :8083]
    GW -->|JWT protected<br/>/api/likes/**| LIKE[Like :8084]
    GW -->|JWT protected<br/>/api/follows/**| FOLLOW[Follow :8085]
    GW -->|JWT protected<br/>/api/notifications/**| NOTIF[Notification :8086]
    GW -->|JWT protected multipart<br/>/api/media/** /api/stories/**| MEDIA[Media :8087]
    GW -->|public search<br/>/api/search/** /api/hashtags/**| SEARCH[Search :8088]
    GW -->|forums route<br/>/api/forums/**| FORUM[Forum :8089]
```

Public gateway paths include registration, login, Google OAuth, token validation/refresh, public posts, search, hashtags, forum listing, comment-by-post, and Swagger docs. Protected routes use the gateway `JwtAuthFilter` and the shared JWT secret configured across services.

## Service Ownership

| Service | Port | Main API Area | Own Database / Tables | Important External Integrations |
|---|---:|---|---|---|
| Frontend | 3000 | React pages/components, Axios API layer | Browser localStorage for JWT/user | Vite proxy to gateway |
| API Gateway | 8090 | `/api/*` routing, CORS, JWT filter, Swagger aggregation | none | Routes to all services |
| Auth | 8081 | `/auth` register/login/profile/admin/internal | `connectsphere_auth.users` | Google OAuth, Search, ActiveMQ, Gmail SMTP |
| Post | 8082 | `/posts` CRUD/feed/public/admin/internal counters | `connectsphere_post.posts`, `post_media_urls` | Follow, Auth, Search, Cloudinary |
| Comment | 8083 | `/comments` comments/replies/threaded/internal counters | `connectsphere_comment.comments` | Post, Auth, Notification |
| Like | 8084 | `/likes` like/unlike/reaction/summary | `connectsphere_like.likes` | Post, Comment, Auth, Notification |
| Follow | 8085 | `/follows` follow/unfollow/counts/suggested | `connectsphere_follow.follows` | Auth, Notification |
| Notification | 8086 | `/notifications` create/read/bulk | `connectsphere_notification.notifications` | Gmail SMTP |
| Media | 8087 | `/media`, `/stories` upload/story feed/story views | `connectsphere_media.media`, `stories` | Cloudinary |
| Search | 8088 | `/search`, `/hashtags`, internal indexing | `connectsphere_search.hashtags`, `post_hashtags` | Elasticsearch, Post, Auth |
| Forum | 8089 | `/forums` forum posts/moderators/bans/admin | `connectsphere_forum.*` | ActiveMQ, Gmail SMTP |

## Inter-Service Communication

```mermaid
flowchart TB
    AUTH[Auth Service] -->|index/remove user| SEARCH[Search Service]
    AUTH -->|deactivation/reactivation events| MQ[ActiveMQ]
    AUTH -->|emails| SMTP[Gmail SMTP]
    AUTH -->|verify Google ID token| GOOGLE[Google OAuth]

    POST -->|get followee IDs for feed| FOLLOW[Follow Service]
    POST -->|resolve author username| AUTH
    POST -->|index post| SEARCH
    POST -->|media upload support| CLOUD[Cloudinary]

    COMMENT -->|increment/decrement comment count| POST
    COMMENT -->|resolve mentions/usernames| AUTH
    COMMENT -->|create comment notifications| NOTIF[Notification Service]

    LIKE -->|increment/decrement post likes| POST
    LIKE -->|increment/decrement comment likes| COMMENT
    LIKE -->|resolve usernames| AUTH
    LIKE -->|create like notifications| NOTIF

    FOLLOW -->|resolve username| AUTH
    FOLLOW -->|create follow notifications| NOTIF

    SEARCH -->|fetch post data when needed| POST
    SEARCH -->|fetch user data when needed| AUTH
    SEARCH --> ES[Elasticsearch]

    MEDIA --> CLOUD
    NOTIF --> SMTP
    FORUM[Forum Service] --> MQ
    FORUM --> SMTP
```

## Main User Flows

### Login / Authenticated API Calls

```mermaid
sequenceDiagram
    participant Browser
    participant FE as React Frontend
    participant GW as API Gateway
    participant AUTH as Auth Service
    participant DB as connectsphere_auth

    Browser->>FE: Submit email/password
    FE->>GW: POST /api/auth/login
    GW->>AUTH: POST /auth/login
    AUTH->>DB: Find user by email
    AUTH-->>GW: JWT token
    GW-->>FE: token
    FE->>GW: GET /api/auth/profile with Authorization Bearer token
    GW->>AUTH: GET /auth/profile
    AUTH->>DB: Load user profile
    AUTH-->>FE: user profile
    FE->>Browser: Store cs_token and cs_user in localStorage
```

### Feed Loading

```mermaid
sequenceDiagram
    participant FE as React Frontend
    participant GW as API Gateway
    participant POST as Post Service
    participant FOLLOW as Follow Service
    participant POSTDB as connectsphere_post
    participant AUTH as Auth Service

    FE->>GW: GET /api/posts/feed with JWT
    GW->>POST: GET /posts/feed
    POST->>FOLLOW: GET /follows/{userId}/following-ids
    FOLLOW-->>POST: followee user IDs
    POST->>POSTDB: Query public/followers-only posts by followee IDs
    POST->>AUTH: GET /auth/internal/username-by-id/{authorId}
    POST-->>GW: feed posts
    GW-->>FE: feed posts
```

### Create Post and Search Indexing

```mermaid
sequenceDiagram
    participant FE as React Frontend
    participant GW as API Gateway
    participant POST as Post Service
    participant POSTDB as connectsphere_post
    participant SEARCH as Search Service
    participant ES as Elasticsearch

    FE->>GW: POST /api/posts
    GW->>POST: POST /posts
    POST->>POSTDB: Save post
    POST->>SEARCH: POST /search/internal/index
    SEARCH->>ES: Index post document
    SEARCH->>SEARCH: Extract/update hashtags in MySQL
    POST-->>FE: Saved post
```

### Like / Comment Notification Flow

```mermaid
sequenceDiagram
    participant FE as React Frontend
    participant GW as API Gateway
    participant LIKE as Like Service
    participant POST as Post Service
    participant COMMENT as Comment Service
    participant AUTH as Auth Service
    participant NOTIF as Notification Service
    participant DB as connectsphere_notification

    FE->>GW: POST /api/likes
    GW->>LIKE: POST /likes
    LIKE->>POST: Increment post like count, if target is POST
    LIKE->>COMMENT: Increment comment like count, if target is COMMENT
    LIKE->>AUTH: Resolve actor username
    LIKE->>NOTIF: POST /notifications/internal/create
    NOTIF->>DB: Save notification
    LIKE-->>FE: Like/reaction result
```

## Technology Stack

- Frontend: React 18, Vite, React Router, Axios, Tailwind CSS, React Icons, React Hot Toast, Jest + React Testing Library.
- Gateway: Spring Cloud Gateway, JWT route filter, CORS, Swagger aggregation.
- Backend services: Spring Boot 3.2, Spring Web, Spring Data JPA, Spring Security, Bean Validation, Lombok.
- Databases: MySQL with one schema per service.
- Search: Elasticsearch 8.x plus MySQL hashtag metadata.
- Messaging: ActiveMQ for account deactivation and forum mail queues.
- Media storage: Cloudinary.
- Email: Gmail SMTP.
- Auth: JWT shared secret across services, plus Google OAuth login.

## How to View This Diagram

Most Markdown viewers that support Mermaid can render this file directly. In VS Code, install a Mermaid Markdown preview extension, then open `ARCHITECTURE.md` and use Markdown Preview.

You can also paste any Mermaid block into https://mermaid.live to render/export PNG or SVG.
