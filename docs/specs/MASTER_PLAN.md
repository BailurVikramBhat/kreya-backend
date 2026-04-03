# Kreya — Master Plan & Project Specification

> Production-Grade Ecommerce Modulith  
> Java 25 | Spring Boot 4.0.5 | React | PostgreSQL | Redis  
> *Kreya (क्रेय) — Sanskrit for "purchasable"*

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Architecture](#2-architecture)
3. [Module Breakdown](#3-module-breakdown)
4. [Database Design](#4-database-design)
5. [Security Architecture](#5-security-architecture)
6. [API Contract](#6-api-contract)
7. [Event Flows](#7-event-flows)
8. [Infrastructure & DevOps](#8-infrastructure--devops)
9. [Observability](#9-observability)
10. [Release Process](#10-release-process)
11. [Design Patterns & DSA Usage](#11-design-patterns--dsa-usage)
12. [MVP Scope](#12-mvp-scope)
13. [Post-MVP Roadmap](#13-post-mvp-roadmap)
14. [Project Structure](#14-project-structure)

---

## 1. System Overview

### 1.1 Vision

Kreya is a production-grade ecommerce platform with three distinct roles:

| Role       | Description                                                                 |
|------------|-----------------------------------------------------------------------------|
| **Shopper** | Browses products, adds to cart, views details, manages profile             |
| **Seller**  | Lists products under admin-approved categories, manages inventory          |
| **Admin**   | Full platform control. `isVikram=true` grants permanent super-admin rights |

### 1.2 Core Principles

- **Modulith-first**: Logical module boundaries with schema-level isolation, deployable as a single unit
- **Event-driven**: In-process events via `ApplicationEventPublisher` (MVP); external broker post-MVP
- **Stateless**: No server-side sessions. JWT access + refresh tokens. Redis for revocation
- **Test-mandatory**: Every service has corresponding tests. No exceptions
- **Observable from day one**: Prometheus + Grafana even in MVP
- **Corporate-grade**: Full design pattern usage, audit trails, rate limiting, zero shortcuts

### 1.3 Tech Stack

| Layer            | Technology                                      |
|------------------|------------------------------------------------|
| Language         | Java 25 (virtual threads, pattern matching, records) |
| Framework        | Spring Boot 4.0.5 + Spring Modulith            |
| Database         | PostgreSQL 17 (single instance, schema-per-module) |
| Cache/Store      | Redis 7 (caching, cart, rate limiting, token revocation) |
| Auth             | JWT (access + refresh) + TOTP MFA (admin)       |
| API Docs         | Springdoc OpenAPI (Swagger UI)                  |
| Search           | PostgreSQL full-text search (MVP) → Elasticsearch (later) |
| File Storage     | Local filesystem (MVP) → S3/MinIO (later)       |
| Monitoring       | Prometheus + Grafana + Spring Boot Actuator     |
| CI/CD            | GitHub Actions                                  |
| Containerization | Docker + Docker Compose                         |
| Email            | SMTP (free tier: Gmail SMTP / Mailhog for dev)  |
| Frontend         | React (managed by Codex — out of scope)         |

---

## 2. Architecture

### 2.1 Modulith Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       Kreya Modulith                            │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │   Auth   │  │   User   │  │  Seller  │  │ Product  │       │
│  │  Module  │  │  Module  │  │  Module  │  │  Module  │       │
│  │          │  │          │  │          │  │          │       │
│  │auth_schema│ │user_schema│ │seller_   │ │product_  │       │
│  │          │  │          │  │schema    │ │schema    │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
│       │              │              │              │             │
│       └──────────────┴──────┬───────┴──────────────┘             │
│                             │                                    │
│                    ApplicationEventPublisher                     │
│                             │                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │   Cart   │  │  Admin   │  │ Notifi-  │  │  Audit   │       │
│  │  Module  │  │  Module  │  │ cation   │  │  Module  │       │
│  │          │  │          │  │  Module   │  │          │       │
│  │  Redis   │  │admin_    │ │notif_    │ │audit_    │       │
│  │          │  │schema    │ │schema    │ │schema    │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Shared Kernel                          │   │
│  │  Security Config │ Rate Limiting │ Exception Handling    │   │
│  │  Base Entities   │ Common DTOs   │ Event Definitions     │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Request Flow

```
Client Request
     │
     ▼
[Rate Limiter (Redis + Bucket4j)]
     │
     ▼
[Spring Security Filter Chain]
     │
     ├── JWT Validation Filter
     ├── Role-Based Access Filter
     └── Admin MFA Verification Filter (admin endpoints only)
     │
     ▼
[REST Controller] ──▶ [Service Layer] ──▶ [Repository Layer]
                           │
                           ├── Publishes Domain Events
                           ├── Redis Operations (cache/cart)
                           └── Audit Logging
```

---

## 3. Module Breakdown

### 3.1 Auth Module (`auth`)

**Owns:** `auth_schema`  
**Responsibility:** Registration, authentication, JWT lifecycle, email verification, password reset, TOTP MFA

| Component              | Details                                                    |
|------------------------|------------------------------------------------------------|
| Registration           | Shopper (direct), Seller (requires `registeringAsSeller=true`), Admin (bootstrapped/promoted) |
| Login                  | Email + password → access token (15 min) + refresh token (7 days) |
| Email Verification     | Token-based link sent on registration. Account limited until verified |
| Password Reset         | Email link with time-limited token (1 hour expiry)         |
| Refresh Token          | Stored in Redis with TTL. Supports revocation per-user, per-session, or global |
| MFA (Admin only)       | TOTP via Google Authenticator. Required on every login + every sensitive action |
| Logout                 | Blacklist access token in Redis (remaining TTL), delete refresh token |

**Key Classes:**
- `AuthController`, `AuthService`, `JwtProvider`, `TotpService`
- `RefreshTokenRepository` (Redis)
- `EmailVerificationToken` (DB entity)

### 3.2 User Module (`user`)

**Owns:** `user_schema`  
**Responsibility:** User profiles, settings, role management

| Component       | Details                                              |
|-----------------|------------------------------------------------------|
| Profile         | Name, email (read-only), phone, role, avatar         |
| Settings        | Update name, phone, password (requires current password) |
| Role Enum       | `SHOPPER`, `SELLER`, `ADMIN`                         |
| isVikram Flag   | Boolean on User entity. Set only during initial seed. Immutable via API |

**Key Classes:**
- `UserController`, `UserService`, `UserRepository`
- `User` entity with `Role` enum and `isVikram` flag

### 3.3 Seller Module (`seller`)

**Owns:** `seller_schema`  
**Responsibility:** Seller onboarding, document management, approval workflow

| Component          | Details                                                     |
|--------------------|-------------------------------------------------------------|
| Seller Profile     | Business name, description, documents, approval status      |
| Document Upload    | Local filesystem storage. Metadata in DB                    |
| Approval Status    | Enum: `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`        |
| Status Transitions | `PENDING → APPROVED`, `PENDING → REJECTED`, `APPROVED → SUSPENDED`, `SUSPENDED → APPROVED` |

**State Machine:**
```
   PENDING ──────► APPROVED ──────► SUSPENDED
      │                                 │
      └──────► REJECTED          ◄──────┘
                                (can be re-approved)
```

**Key Classes:**
- `SellerController`, `SellerService`, `SellerRepository`
- `SellerProfile`, `SellerDocument` entities
- `SellerApprovalStatus` enum with valid transition validation

### 3.4 Product Module (`product`)

**Owns:** `product_schema`  
**Responsibility:** Products, hierarchical categories, inventory

| Component      | Details                                                          |
|----------------|------------------------------------------------------------------|
| Category       | Hierarchical tree (self-referential `parent_id`). Admin CRUD     |
| Product        | Title, description, price, images (local), category, seller_id, stock |
| Inventory      | Seller manages stock count. Events emitted on low stock          |
| Product Status | `DRAFT`, `ACTIVE`, `INACTIVE`, `REMOVED_BY_ADMIN`               |
| Listing Rule   | One seller per product. No duplicate listings                    |

**Category Tree Example:**
```
Electronics
├── Phones
│   ├── Smartphones
│   └── Feature Phones
├── Laptops
└── Accessories

Fashion
├── Men
├── Women
└── Kids
```

**Key Classes:**
- `ProductController`, `ProductService`, `ProductRepository`
- `CategoryController`, `CategoryService`, `CategoryRepository`
- `Product`, `Category` entities
- `ProductSpecification` (for filtering/search using Spring Specification pattern)

### 3.5 Cart Module (`cart`)

**Owns:** Redis (no PostgreSQL schema)  
**Responsibility:** Shopping cart management

| Component   | Details                                                    |
|-------------|-----------------------------------------------------------|
| Cart        | Redis hash: `cart:{userId}` → `{productId: quantity}`     |
| TTL         | 30 days from last modification                            |
| Operations  | Add item, remove item, update quantity, get cart, clear cart |
| Validation  | Check product exists, is active, has sufficient stock      |

**Redis Data Structure:**
```
Key:    cart:{userId}
Type:   Hash
Fields: productId → JSON{quantity, addedAt, priceAtAdd}
TTL:    30 days (reset on modification)
```

**Key Classes:**
- `CartController`, `CartService`, `CartRedisRepository`
- `CartItem` record, `CartResponse` DTO

### 3.6 Admin Module (`admin`)

**Owns:** `admin_schema`  
**Responsibility:** Platform administration, seller management, session monitoring, metrics

| Component           | Details                                                      |
|---------------------|--------------------------------------------------------------|
| Seller Approval     | View pending sellers, approve/reject with reason             |
| Seller Management   | View all sellers, suspend/unsuspend, view non-PII details    |
| Product Moderation  | View all products, remove products                           |
| Category Management | CRUD hierarchical categories                                 |
| Session Monitoring  | View active sessions (from Redis), view per-user sessions    |
| User Overview       | Non-PII view of shoppers and sellers (registration date, status, last active) |
| Promote to Admin    | Elevate any seller to admin role (requires MFA re-auth)      |
| Password Reset      | Trigger password-reset email for any user (requires MFA)     |
| System Metrics      | Expose Prometheus metrics via admin dashboard                |
| MFA Gate            | Every sensitive action requires TOTP re-verification         |

**Sensitive Actions (require MFA re-auth every time):**
- Approve/reject seller
- Suspend seller
- Remove product
- Promote user to admin
- Trigger password reset for user
- Create/update/delete categories
- View session details

**Key Classes:**
- `AdminController`, `AdminService`
- `AdminSellerController`, `AdminProductController`, `AdminCategoryController`
- `AdminSessionController`, `AdminMetricsController`

### 3.7 Notification Module (`notification`)

**Owns:** `notification_schema`  
**Responsibility:** Email dispatch, notification templates

| Component        | Details                                            |
|------------------|----------------------------------------------------|
| Email Dispatch   | Async event-driven. Listens for domain events      |
| Templates        | Thymeleaf HTML templates for emails                |
| Email Types      | Verification, password reset, seller approved/rejected, seller suspended |
| Dev Mode         | Mailhog for local dev (no real emails sent)        |
| Retry            | Failed emails retried 3 times with exponential backoff |

**Key Classes:**
- `EmailService`, `EmailTemplateEngine`
- `NotificationEventListener` (listens to domain events)
- `EmailLog` entity (tracks sent/failed emails)

### 3.8 Audit Module (`audit`)

**Owns:** `audit_schema`  
**Responsibility:** Immutable audit trail for all admin actions

| Component   | Details                                                       |
|-------------|---------------------------------------------------------------|
| Audit Log   | Who, what, when, target entity, before/after state (JSON)     |
| Immutable   | Insert-only table. No updates, no deletes                     |
| Query       | Filterable by admin, action type, target, date range          |

**Audit Log Schema:**
```
audit_log (
  id              BIGSERIAL PRIMARY KEY,
  admin_user_id   BIGINT NOT NULL,
  action          VARCHAR(100) NOT NULL,
  target_type     VARCHAR(50),
  target_id       BIGINT,
  details_before  JSONB,
  details_after   JSONB,
  ip_address      VARCHAR(45),
  user_agent      TEXT,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
)
-- No updated_at. Immutable.
```

**Key Classes:**
- `AuditService`, `AuditRepository`
- `AuditLog` entity
- `@Auditable` custom annotation + AOP aspect for automatic capture

---

## 4. Database Design

### 4.1 Schema Layout

Single PostgreSQL instance, schema-per-module:

```
PostgreSQL Instance
├── auth_schema
│   ├── credentials
│   └── email_verification_tokens
│   └── password_reset_tokens
├── user_schema
│   └── users
├── seller_schema
│   ├── seller_profiles
│   └── seller_documents
├── product_schema
│   ├── categories
│   └── products
├── admin_schema
│   └── (admin-specific config if needed)
├── notification_schema
│   └── email_logs
└── audit_schema
    └── audit_logs
```

### 4.2 Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────┐
│                   auth_schema                        │
│                                                      │
│  credentials                                         │
│  ┌──────────────────────────────────────┐            │
│  │ id (PK)           BIGSERIAL          │            │
│  │ user_id (FK→users) BIGINT UNIQUE     │            │
│  │ email              VARCHAR(255) UQ    │            │
│  │ password_hash      VARCHAR(255)       │            │
│  │ email_verified     BOOLEAN DEFAULT F  │            │
│  │ mfa_enabled        BOOLEAN DEFAULT F  │            │
│  │ mfa_secret         VARCHAR(64)        │            │
│  │ account_locked     BOOLEAN DEFAULT F  │            │
│  │ failed_attempts    INT DEFAULT 0      │            │
│  │ lock_expires_at    TIMESTAMP          │            │
│  │ created_at         TIMESTAMP          │            │
│  │ updated_at         TIMESTAMP          │            │
│  └──────────────────────────────────────┘            │
│                                                      │
│  email_verification_tokens                           │
│  ┌──────────────────────────────────────┐            │
│  │ id (PK)           BIGSERIAL          │            │
│  │ user_id            BIGINT            │            │
│  │ token              VARCHAR(255) UQ    │            │
│  │ expires_at         TIMESTAMP          │            │
│  │ used               BOOLEAN DEFAULT F  │            │
│  │ created_at         TIMESTAMP          │            │
│  └──────────────────────────────────────┘            │
│                                                      │
│  password_reset_tokens                               │
│  ┌──────────────────────────────────────┐            │
│  │ id (PK)           BIGSERIAL          │            │
│  │ user_id            BIGINT            │            │
│  │ token              VARCHAR(255) UQ    │            │
│  │ expires_at         TIMESTAMP          │            │
│  │ used               BOOLEAN DEFAULT F  │            │
│  │ created_at         TIMESTAMP          │            │
│  └──────────────────────────────────────┘            │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│                   user_schema                        │
│                                                      │
│  users                                               │
│  ┌──────────────────────────────────────┐            │
│  │ id (PK)           BIGSERIAL          │            │
│  │ email              VARCHAR(255) UQ    │            │
│  │ first_name         VARCHAR(100)       │            │
│  │ last_name          VARCHAR(100)       │            │
│  │ phone              VARCHAR(20)        │            │
│  │ role               VARCHAR(20)        │  ← ENUM   │
│  │ is_vikram          BOOLEAN DEFAULT F  │            │
│  │ active             BOOLEAN DEFAULT T  │            │
│  │ avatar_path        VARCHAR(500)       │            │
│  │ last_login_at      TIMESTAMP          │            │
│  │ created_at         TIMESTAMP          │            │
│  │ updated_at         TIMESTAMP          │            │
│  └──────────────────────────────────────┘            │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│                  seller_schema                       │
│                                                      │
│  seller_profiles                                     │
│  ┌──────────────────────────────────────┐            │
│  │ id (PK)           BIGSERIAL          │            │
│  │ user_id (FK→users) BIGINT UNIQUE     │            │
│  │ business_name      VARCHAR(255)       │            │
│  │ business_desc      TEXT               │            │
│  │ approval_status    VARCHAR(20)        │  ← ENUM   │
│  │ rejection_reason   TEXT               │            │
│  │ approved_by        BIGINT             │            │
│  │ approved_at        TIMESTAMP          │            │
│  │ created_at         TIMESTAMP          │            │
│  │ updated_at         TIMESTAMP          │            │
│  └──────────────────────────────────────┘            │
│                                                      │
│  seller_documents                                    │
│  ┌──────────────────────────────────────┐            │
│  │ id (PK)           BIGSERIAL          │            │
│  │ seller_profile_id   BIGINT (FK)       │            │
│  │ document_type      VARCHAR(50)        │            │
│  │ file_path          VARCHAR(500)       │            │
│  │ file_name          VARCHAR(255)       │            │
│  │ file_size          BIGINT             │            │
│  │ mime_type          VARCHAR(100)       │            │
│  │ uploaded_at        TIMESTAMP          │            │
│  └──────────────────────────────────────┘            │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│                 product_schema                       │
│                                                      │
│  categories                                          │
│  ┌──────────────────────────────────────┐            │
│  │ id (PK)           BIGSERIAL          │            │
│  │ name               VARCHAR(100)       │            │
│  │ slug               VARCHAR(100) UQ    │            │
│  │ parent_id (FK→self) BIGINT NULLABLE   │            │
│  │ depth              INT DEFAULT 0      │            │
│  │ sort_order         INT DEFAULT 0      │            │
│  │ active             BOOLEAN DEFAULT T  │            │
│  │ created_at         TIMESTAMP          │            │
│  │ updated_at         TIMESTAMP          │            │
│  └──────────────────────────────────────┘            │
│  INDEX: idx_categories_parent ON (parent_id)         │
│  INDEX: idx_categories_slug ON (slug)                │
│                                                      │
│  products                                            │
│  ┌──────────────────────────────────────┐            │
│  │ id (PK)           BIGSERIAL          │            │
│  │ seller_id (ref)    BIGINT NOT NULL    │            │
│  │ category_id (FK)   BIGINT NOT NULL    │            │
│  │ title              VARCHAR(255)       │            │
│  │ slug               VARCHAR(255) UQ    │            │
│  │ description        TEXT               │            │
│  │ price              DECIMAL(12,2)      │            │
│  │ stock_quantity     INT DEFAULT 0      │            │
│  │ status             VARCHAR(20)        │  ← ENUM   │
│  │ image_paths        JSONB              │            │
│  │ attributes         JSONB              │            │
│  │ created_at         TIMESTAMP          │            │
│  │ updated_at         TIMESTAMP          │            │
│  └──────────────────────────────────────┘            │
│  INDEX: idx_products_seller ON (seller_id)           │
│  INDEX: idx_products_category ON (category_id)       │
│  INDEX: idx_products_status ON (status)              │
│  INDEX: idx_products_title_trgm ON (title) USING gin │
│         (pg_trgm for full-text search MVP)           │
└─────────────────────────────────────────────────────┘
```

### 4.3 Redis Data Structures

```
# JWT Refresh Tokens
Key:    refresh:{userId}:{tokenId}
Type:   String (token hash)
TTL:    7 days

# JWT Blacklist (logged-out access tokens)
Key:    blacklist:{jti}
Type:   String ("1")
TTL:    remaining access token lifetime

# Shopping Cart
Key:    cart:{userId}
Type:   Hash { productId → JSON{quantity, addedAt, priceSnapshot} }
TTL:    30 days (reset on write)

# Rate Limiting (Bucket4j)
Key:    rate:{endpoint}:{clientIp|userId}
Type:   String (Bucket4j state)
TTL:    varies by endpoint

# Active Sessions (for admin monitoring)
Key:    session:{userId}:{tokenId}
Type:   Hash { loginAt, ip, userAgent, lastActive }
TTL:    matches refresh token TTL

# Cache: Category Tree
Key:    cache:categories:tree
Type:   String (JSON)
TTL:    1 hour

# Cache: Product Detail
Key:    cache:product:{productId}
Type:   String (JSON)
TTL:    15 minutes
```

---

## 5. Security Architecture

### 5.1 Authentication Flow

```
┌──────────┐     POST /api/v1/auth/login      ┌──────────────┐
│  Client  │ ──────────────────────────────▶   │ Auth Module  │
│          │     {email, password}              │              │
│          │                                    │  1. Validate │
│          │                                    │  2. Check    │
│          │                                    │     locked?  │
│          │                                    │  3. Verify   │
│          │                                    │     password │
│          │     {accessToken, refreshToken}    │  4. If admin │
│          │ ◀──────────────────────────────    │     → require│
│          │                                    │     TOTP     │
└──────────┘                                    └──────────────┘

Admin Login (2-step):
  Step 1: POST /api/v1/auth/login → {mfaRequired: true, mfaToken: "temp"}
  Step 2: POST /api/v1/auth/mfa/verify → {accessToken, refreshToken}
```

### 5.2 JWT Token Structure

**Access Token (15 min):**
```json
{
  "sub": "userId",
  "jti": "unique-token-id",
  "role": "SELLER",
  "isVikram": false,
  "emailVerified": true,
  "iat": 1234567890,
  "exp": 1234568790
}
```

**Refresh Token (7 days):**
```json
{
  "sub": "userId",
  "jti": "unique-token-id",
  "type": "REFRESH",
  "iat": 1234567890,
  "exp": 1235172690
}
```

### 5.3 Rate Limiting Strategy

| Endpoint Category      | Rate Limit               | Window  |
|------------------------|--------------------------|---------|
| Auth (login/register)  | 5 requests               | 1 min   |
| Auth (password reset)  | 3 requests               | 15 min  |
| Public API (products)  | 60 requests              | 1 min   |
| Authenticated API      | 120 requests             | 1 min   |
| Admin API              | 200 requests             | 1 min   |
| File Upload            | 10 requests              | 5 min   |

Implementation: **Bucket4j + Redis** (distributed rate limiting across instances)

### 5.4 Security Checklist

- [x] BCrypt password hashing (cost factor 12)
- [x] Account lockout after 5 failed login attempts (30 min lock)
- [x] JWT signed with RS256 (asymmetric keys)
- [x] CORS configured per environment
- [x] CSRF disabled (stateless API)
- [x] Input validation via Jakarta Bean Validation
- [x] SQL injection prevention (JPA parameterized queries)
- [x] XSS prevention (output encoding, Content-Security-Policy headers)
- [x] Rate limiting on all endpoints
- [x] File upload: type validation, size limits (10MB), sanitized filenames
- [x] Sensitive data never logged (passwords, tokens)
- [x] Admin MFA re-auth on every sensitive action
- [x] Refresh token rotation on use (old token invalidated)

---

## 6. API Contract

### 6.1 Base URL

```
/api/v1
```

### 6.2 Auth Endpoints

```
POST   /api/v1/auth/register              # Register shopper or seller
POST   /api/v1/auth/login                 # Login (returns JWT or MFA challenge)
POST   /api/v1/auth/mfa/verify            # Complete MFA login (admin)
POST   /api/v1/auth/refresh               # Refresh access token
POST   /api/v1/auth/logout                # Logout (blacklist token)
POST   /api/v1/auth/email/verify          # Verify email with token
POST   /api/v1/auth/email/resend          # Resend verification email
POST   /api/v1/auth/password/forgot       # Request password reset email
POST   /api/v1/auth/password/reset        # Reset password with token
```

### 6.3 User Endpoints

```
GET    /api/v1/users/me                   # Get current user profile
PUT    /api/v1/users/me                   # Update profile (name, phone)
PUT    /api/v1/users/me/password          # Change password
PUT    /api/v1/users/me/avatar            # Upload avatar
```

### 6.4 Seller Endpoints

```
GET    /api/v1/seller/profile             # Get seller profile + approval status
PUT    /api/v1/seller/profile             # Update seller profile
POST   /api/v1/seller/documents           # Upload verification document
GET    /api/v1/seller/documents           # List uploaded documents
DELETE /api/v1/seller/documents/{id}      # Delete document (only if PENDING)

GET    /api/v1/seller/products            # List seller's products
POST   /api/v1/seller/products            # Create product
PUT    /api/v1/seller/products/{id}       # Update product
DELETE /api/v1/seller/products/{id}       # Soft-delete product
PUT    /api/v1/seller/products/{id}/stock # Update stock quantity
POST   /api/v1/seller/products/{id}/images # Upload product images
```

### 6.5 Shopper Endpoints

```
GET    /api/v1/products                   # Browse products (paginated, filterable)
GET    /api/v1/products/{slug}            # Product detail
GET    /api/v1/categories                 # Get category tree

GET    /api/v1/cart                       # Get cart
POST   /api/v1/cart/items                 # Add item to cart
PUT    /api/v1/cart/items/{productId}     # Update item quantity
DELETE /api/v1/cart/items/{productId}     # Remove item from cart
DELETE /api/v1/cart                       # Clear cart
```

### 6.6 Admin Endpoints

All admin endpoints require `role=ADMIN` + TOTP re-verification header.

```
# Seller Management
GET    /api/v1/admin/sellers                      # List all sellers (filterable by status)
GET    /api/v1/admin/sellers/{id}                 # Seller detail (non-PII)
GET    /api/v1/admin/sellers/{id}/documents       # View seller documents
POST   /api/v1/admin/sellers/{id}/approve         # Approve seller
POST   /api/v1/admin/sellers/{id}/reject          # Reject seller (with reason)
POST   /api/v1/admin/sellers/{id}/suspend         # Suspend seller
POST   /api/v1/admin/sellers/{id}/unsuspend       # Unsuspend seller

# Product Moderation
GET    /api/v1/admin/products                     # List all products
DELETE /api/v1/admin/products/{id}                # Remove product

# Category Management
GET    /api/v1/admin/categories                   # List categories (tree)
POST   /api/v1/admin/categories                   # Create category
PUT    /api/v1/admin/categories/{id}              # Update category
DELETE /api/v1/admin/categories/{id}              # Delete category (only if empty)

# User Management
GET    /api/v1/admin/users                        # List users (non-PII)
GET    /api/v1/admin/users/{id}                   # User detail (non-PII)
POST   /api/v1/admin/users/{id}/promote           # Promote to admin
POST   /api/v1/admin/users/{id}/password-reset    # Trigger password reset email

# Session Monitoring
GET    /api/v1/admin/sessions                     # All active sessions
GET    /api/v1/admin/sessions/user/{id}           # Sessions for specific user
DELETE /api/v1/admin/sessions/{sessionId}         # Kill specific session

# Audit
GET    /api/v1/admin/audit-logs                   # Query audit logs (paginated, filterable)

# MFA
POST   /api/v1/admin/mfa/setup                   # Setup TOTP (returns QR code)
POST   /api/v1/admin/mfa/reauth                  # Re-authenticate with TOTP for sensitive action
```

### 6.7 Standard Response Envelope

```json
{
  "success": true,
  "data": { },
  "message": "Operation completed successfully",
  "timestamp": "2026-04-03T10:15:30Z",
  "path": "/api/v1/products",
  "errors": null
}
```

**Error Response:**
```json
{
  "success": false,
  "data": null,
  "message": "Validation failed",
  "timestamp": "2026-04-03T10:15:30Z",
  "path": "/api/v1/auth/register",
  "errors": [
    { "field": "email", "message": "Email already registered" }
  ]
}
```

**Pagination Response:**
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "last": false
  },
  "message": null,
  "timestamp": "2026-04-03T10:15:30Z",
  "path": "/api/v1/products"
}
```

---

## 7. Event Flows

### 7.1 MVP Events (In-Process via ApplicationEventPublisher)

```
┌─────────────────────────────────────────────────────────────┐
│                    Domain Events (MVP)                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  UserRegisteredEvent                                        │
│    → NotificationModule: Send verification email            │
│    → AuditModule: Log registration                          │
│                                                             │
│  EmailVerifiedEvent                                         │
│    → UserModule: Mark email as verified                     │
│                                                             │
│  SellerRegistrationRequestedEvent                           │
│    → AdminModule: Create pending approval entry             │
│    → NotificationModule: Notify admin (email)               │
│                                                             │
│  SellerApprovedEvent                                        │
│    → SellerModule: Update status, grant product rights      │
│    → NotificationModule: Notify seller                      │
│    → AuditModule: Log approval                              │
│                                                             │
│  SellerRejectedEvent                                        │
│    → SellerModule: Update status with reason                │
│    → NotificationModule: Notify seller                      │
│    → AuditModule: Log rejection                             │
│                                                             │
│  SellerSuspendedEvent                                       │
│    → ProductModule: Deactivate all seller products          │
│    → NotificationModule: Notify seller                      │
│    → AuditModule: Log suspension                            │
│                                                             │
│  ProductCreatedEvent                                        │
│    → AuditModule: Log product creation                      │
│    → CacheModule: Invalidate relevant caches                │
│                                                             │
│  ProductUpdatedEvent                                        │
│    → CacheModule: Invalidate product cache                  │
│                                                             │
│  ProductRemovedByAdminEvent                                 │
│    → NotificationModule: Notify seller                      │
│    → AuditModule: Log removal                               │
│                                                             │
│  PasswordResetRequestedEvent                                │
│    → NotificationModule: Send reset email                   │
│                                                             │
│  UserPromotedToAdminEvent                                   │
│    → AuthModule: Enable MFA requirement                     │
│    → NotificationModule: Notify user                        │
│    → AuditModule: Log promotion                             │
│                                                             │
│  StockUpdatedEvent                                          │
│    → CacheModule: Invalidate product cache                  │
│                                                             │
│  SessionCreatedEvent                                        │
│    → Redis: Store session metadata                          │
│                                                             │
│  SessionDestroyedEvent                                      │
│    → Redis: Remove session metadata                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 Event Base Class

```java
public sealed abstract class DomainEvent permits
    UserRegisteredEvent, EmailVerifiedEvent,
    SellerRegistrationRequestedEvent, SellerApprovedEvent,
    SellerRejectedEvent, SellerSuspendedEvent,
    ProductCreatedEvent, ProductUpdatedEvent,
    ProductRemovedByAdminEvent, PasswordResetRequestedEvent,
    UserPromotedToAdminEvent, StockUpdatedEvent,
    SessionCreatedEvent, SessionDestroyedEvent {

    private final UUID eventId = UUID.randomUUID();
    private final Instant occurredAt = Instant.now();
    private final String source;

    // getters...
}
```

---

## 8. Infrastructure & DevOps

### 8.1 Docker Compose (Development)

```yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
    depends_on: [postgres, redis]
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_URL: jdbc:postgresql://postgres:5432/kreya
      REDIS_HOST: redis

  postgres:
    image: postgres:17
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: kreya
      POSTGRES_USER: kreya
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./init-schemas.sql:/docker-entrypoint-initdb.d/01-schemas.sql

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --requirepass ${REDIS_PASSWORD}

  mailhog:
    image: mailhog/mailhog
    ports: ["1025:1025", "8025:8025"]

  prometheus:
    image: prom/prometheus
    ports: ["9090:9090"]
    volumes:
      - ./infra/prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana
    ports: ["3001:3000"]
    volumes:
      - ./infra/grafana/dashboards:/var/lib/grafana/dashboards
      - ./infra/grafana/provisioning:/etc/grafana/provisioning

volumes:
  pgdata:
```

### 8.2 GitHub Actions CI Pipeline

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [main, "release/*"]
  pull_request:
    branches: [main, "release/*"]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:17
        env:
          POSTGRES_DB: kreya_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports: [5432:5432]
      redis:
        image: redis:7-alpine
        ports: [6379:6379]

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: oracle
          java-version: 25
      - name: Build & Test
        run: ./gradlew build test jacocoTestReport
      - name: Architecture Tests (Spring Modulith)
        run: ./gradlew test --tests "*ArchitectureTests*"
      - name: Upload Coverage
        uses: codecov/codecov-action@v4

  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: OWASP Dependency Check
        run: ./gradlew dependencyCheckAnalyze

  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Checkstyle
        run: ./gradlew checkstyleMain checkstyleTest
```

### 8.3 Branch Protection Rules

```
main branch:
  ✓ Require pull request before merging
  ✓ Require at least 1 approval
  ✓ Require status checks to pass (build-and-test, security-scan, lint)
  ✓ Require branches to be up to date
  ✓ No force pushes
  ✓ No deletions

release/* branches:
  ✓ Same as main
```

---

## 9. Observability

### 9.1 Metrics (Prometheus)

**Application Metrics:**
- `http_server_requests_seconds` (built-in Micrometer)
- `kreya_auth_login_total{status=success|failure|locked}`
- `kreya_auth_registration_total{role=shopper|seller}`
- `kreya_seller_approval_total{action=approved|rejected|suspended}`
- `kreya_product_created_total`
- `kreya_cart_operations_total{op=add|remove|clear}`
- `kreya_active_sessions_gauge`
- `kreya_rate_limit_exceeded_total`

**JVM Metrics (built-in):**
- Memory, GC, threads, CPU

**Database Metrics:**
- HikariCP pool stats (active, idle, pending)

### 9.2 Grafana Dashboards

1. **Application Overview** — Request rate, error rate, latency p50/p95/p99
2. **Auth Dashboard** — Login success/failure, registrations, locked accounts
3. **Business Dashboard** — Active sellers, products listed, cart activity
4. **JVM Dashboard** — Memory, GC pauses, thread counts
5. **Infrastructure** — PostgreSQL connections, Redis hit rate, cache stats

### 9.3 Logging

- Structured JSON logging (Logback + Logstash encoder)
- Correlation ID in MDC (propagated via filter)
- Log levels: ERROR (alerts), WARN (investigate), INFO (business events), DEBUG (dev only)
- Sensitive data redacted via custom pattern layout

---

## 10. Release Process

### 10.1 Monthly Release Calendar

```
Day 1-24:  Feature development on release/YYYY-MM branch
Day 25:    Branch locked. No new features. Bug fixes only
Day 25-29: Regression testing (UI + API)
Day 29/30: Merge to main. Tag: v{YYYY}.{MM}.0
Day 30/31: Final smoke test on main
```

### 10.2 Branch Strategy

```
main (protected)
  │
  ├── release/2026-05 (monthly working branch)
  │     ├── feature/CART-001-add-to-cart
  │     ├── feature/AUTH-002-mfa-setup
  │     └── bugfix/PROD-003-login-timeout
  │
  └── release/2026-06 (next month)
```

### 10.3 Git Tags

```
v2026.05.0    # May release
v2026.05.1    # Hotfix if needed
v2026.06.0    # June release
```

### 10.4 Versioning

Calendar-based: `YYYY.MM.PATCH`

---

## 11. Design Patterns & DSA Usage

### 11.1 Design Patterns

| Pattern                  | Where Used                                              |
|--------------------------|---------------------------------------------------------|
| **Builder**              | Complex DTOs, query specifications, JWT token building  |
| **Strategy**             | Rate limiting strategies per endpoint tier               |
| **State Machine**        | Seller approval status transitions                      |
| **Observer**             | Domain event publishing and listeners                   |
| **Repository**           | Data access layer (Spring Data JPA)                     |
| **Specification**        | Product filtering/search (JPA Specification)            |
| **Factory Method**       | Creating domain events, notification types              |
| **Template Method**      | Base service classes with common audit/validation logic  |
| **Decorator**            | Adding caching, rate limiting, audit logging via AOP    |
| **Facade**               | AdminService orchestrating across modules               |
| **Singleton**            | Spring beans (default scope)                            |
| **Chain of Responsibility** | Spring Security filter chain                         |
| **Command**              | Admin actions (approve, reject, suspend) as command objects |
| **Adapter**              | File storage adapter (local now, S3 later)              |

### 11.2 Data Structures & Algorithms

| DSA                      | Where Used                                              |
|--------------------------|---------------------------------------------------------|
| **Tree (N-ary)**         | Hierarchical category structure, tree traversal for display |
| **HashMap**              | Redis cart storage, in-memory caches                    |
| **Queue**                | Event processing, email retry queue                     |
| **Trie**                 | Product search autocomplete (post-MVP)                  |
| **Graph (DAG)**          | Category dependency validation (no circular parents)    |
| **Sliding Window**       | Rate limiting (token bucket / sliding window counter)   |
| **Binary Search**        | Paginated result navigation, sorted product listings    |
| **LRU Cache**            | Redis eviction policy for cache entries                 |

---

## 12. MVP Scope

### 12.1 MVP Feature Checklist

**Auth Module:**
- [x] Shopper registration
- [x] Seller registration (with seller flag)
- [x] Login (JWT access + refresh)
- [x] Email verification
- [x] Password reset via email
- [x] Admin MFA (TOTP) login
- [x] Logout with token blacklisting
- [x] Rate limiting on auth endpoints
- [x] Account lockout

**User Module:**
- [x] View profile
- [x] Update name, phone, password
- [x] isVikram flag (seed only)

**Seller Module:**
- [x] Seller profile creation
- [x] Document upload (local storage)
- [x] View approval status
- [x] Product CRUD (after approval)
- [x] Stock management

**Product Module:**
- [x] Hierarchical categories (admin CRUD)
- [x] Product listing (paginated, filterable by category)
- [x] Product detail page
- [x] Simple DB search (title, description)

**Cart Module:**
- [x] Add/remove/update items (Redis)
- [x] View cart with product details
- [x] Stock validation on add

**Admin Module:**
- [x] Seller approval/rejection workflow
- [x] Seller suspension
- [x] Product removal
- [x] Category CRUD
- [x] User listing (non-PII)
- [x] Promote to admin
- [x] Trigger password reset for users
- [x] Active session monitoring
- [x] Audit log viewing
- [x] MFA re-auth on every sensitive action
- [x] Vintage 2003 UI aesthetic

**Notification Module:**
- [x] Verification email
- [x] Password reset email
- [x] Seller approved/rejected email
- [x] Seller suspended email

**Audit Module:**
- [x] Log all admin actions
- [x] Queryable audit trail

**Cross-Cutting:**
- [x] Rate limiting (Bucket4j + Redis)
- [x] Prometheus + Grafana
- [x] Springdoc OpenAPI
- [x] Docker Compose dev environment
- [x] GitHub Actions CI
- [x] Architecture tests (Spring Modulith)
- [x] Unit + integration tests for every service

### 12.2 Explicitly NOT in MVP

- [ ] Payment/checkout flow
- [ ] Order management
- [ ] Product reviews/ratings
- [ ] Product flagging by shoppers
- [ ] LLM product-category matching
- [ ] Elasticsearch
- [ ] S3/MinIO file storage
- [ ] External message broker (Kafka/RabbitMQ)
- [ ] WebSocket real-time notifications
- [ ] Discount/coupon system
- [ ] Wishlist
- [ ] Multi-seller per product
- [ ] Seller MFA (optional, post-MVP consideration)

---

## 13. Post-MVP Roadmap

| Phase | Features                                                    |
|-------|-------------------------------------------------------------|
| **2** | Payment integration, order management, checkout flow        |
| **3** | Elasticsearch, product search autocomplete                  |
| **4** | External event broker (Kafka), async event processing       |
| **5** | LLM product-category validation                            |
| **6** | S3/MinIO file storage migration                             |
| **7** | Product reviews/ratings, shopper flagging                   |
| **8** | WebSocket notifications, real-time admin dashboard          |
| **9** | Kubernetes deployment, staging environment                  |

---

## 14. Project Structure

```
kreya/
├── build.gradle                          # Root build file
├── settings.gradle                       # Module declarations
├── gradle/
│   └── libs.versions.toml                # Version catalog
├── docker-compose.yml
├── Dockerfile
├── init-schemas.sql                      # Create all schemas
│
├── infra/
│   ├── prometheus.yml
│   └── grafana/
│       ├── dashboards/
│       └── provisioning/
│
├── src/main/java/com/kreya/
│   │
│   ├── KreyaApplication.java             # Main class
│   │
│   ├── shared/                           # Shared Kernel
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── JacksonConfig.java
│   │   │   ├── OpenApiConfig.java
│   │   │   └── RateLimitConfig.java
│   │   ├── security/
│   │   │   ├── JwtProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── AdminMfaFilter.java
│   │   │   └── CurrentUserProvider.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── BusinessException.java
│   │   │   └── ErrorCode.java
│   │   ├── dto/
│   │   │   ├── ApiResponse.java
│   │   │   └── PagedResponse.java
│   │   ├── event/
│   │   │   └── DomainEvent.java
│   │   ├── entity/
│   │   │   └── BaseEntity.java
│   │   ├── filter/
│   │   │   ├── CorrelationIdFilter.java
│   │   │   └── RateLimitFilter.java
│   │   └── util/
│   │       └── SlugUtils.java
│   │
│   ├── auth/                             # Auth Module
│   │   ├── controller/
│   │   │   └── AuthController.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── TotpService.java
│   │   │   └── RefreshTokenService.java
│   │   ├── repository/
│   │   │   ├── CredentialRepository.java
│   │   │   ├── EmailVerificationTokenRepository.java
│   │   │   └── PasswordResetTokenRepository.java
│   │   ├── entity/
│   │   │   ├── Credential.java
│   │   │   ├── EmailVerificationToken.java
│   │   │   └── PasswordResetToken.java
│   │   ├── dto/
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── TokenResponse.java
│   │   │   └── MfaVerifyRequest.java
│   │   └── event/
│   │       ├── UserRegisteredEvent.java
│   │       ├── EmailVerifiedEvent.java
│   │       └── PasswordResetRequestedEvent.java
│   │
│   ├── user/                             # User Module
│   │   ├── controller/
│   │   │   └── UserController.java
│   │   ├── service/
│   │   │   └── UserService.java
│   │   ├── repository/
│   │   │   └── UserRepository.java
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   └── Role.java
│   │   └── dto/
│   │       ├── UserProfileResponse.java
│   │       └── UpdateProfileRequest.java
│   │
│   ├── seller/                           # Seller Module
│   │   ├── controller/
│   │   │   └── SellerController.java
│   │   ├── service/
│   │   │   ├── SellerService.java
│   │   │   └── DocumentStorageService.java
│   │   ├── repository/
│   │   │   ├── SellerProfileRepository.java
│   │   │   └── SellerDocumentRepository.java
│   │   ├── entity/
│   │   │   ├── SellerProfile.java
│   │   │   ├── SellerDocument.java
│   │   │   └── ApprovalStatus.java
│   │   ├── dto/
│   │   │   ├── SellerProfileResponse.java
│   │   │   └── DocumentUploadResponse.java
│   │   └── event/
│   │       ├── SellerRegistrationRequestedEvent.java
│   │       ├── SellerApprovedEvent.java
│   │       ├── SellerRejectedEvent.java
│   │       └── SellerSuspendedEvent.java
│   │
│   ├── product/                          # Product Module
│   │   ├── controller/
│   │   │   ├── ProductController.java
│   │   │   └── CategoryController.java
│   │   ├── service/
│   │   │   ├── ProductService.java
│   │   │   └── CategoryService.java
│   │   ├── repository/
│   │   │   ├── ProductRepository.java
│   │   │   └── CategoryRepository.java
│   │   ├── entity/
│   │   │   ├── Product.java
│   │   │   ├── Category.java
│   │   │   └── ProductStatus.java
│   │   ├── specification/
│   │   │   └── ProductSpecification.java
│   │   ├── dto/
│   │   │   ├── ProductResponse.java
│   │   │   ├── CreateProductRequest.java
│   │   │   ├── CategoryResponse.java
│   │   │   └── CategoryTreeResponse.java
│   │   └── event/
│   │       ├── ProductCreatedEvent.java
│   │       ├── ProductUpdatedEvent.java
│   │       ├── ProductRemovedByAdminEvent.java
│   │       └── StockUpdatedEvent.java
│   │
│   ├── cart/                             # Cart Module
│   │   ├── controller/
│   │   │   └── CartController.java
│   │   ├── service/
│   │   │   └── CartService.java
│   │   ├── repository/
│   │   │   └── CartRedisRepository.java
│   │   └── dto/
│   │       ├── CartResponse.java
│   │       ├── CartItemRequest.java
│   │       └── CartItemResponse.java
│   │
│   ├── admin/                            # Admin Module
│   │   ├── controller/
│   │   │   ├── AdminSellerController.java
│   │   │   ├── AdminProductController.java
│   │   │   ├── AdminCategoryController.java
│   │   │   ├── AdminUserController.java
│   │   │   ├── AdminSessionController.java
│   │   │   ├── AdminAuditController.java
│   │   │   └── AdminMfaController.java
│   │   ├── service/
│   │   │   ├── AdminSellerService.java
│   │   │   ├── AdminProductService.java
│   │   │   ├── AdminUserService.java
│   │   │   └── AdminSessionService.java
│   │   └── dto/
│   │       ├── SellerApprovalRequest.java
│   │       ├── AdminUserResponse.java
│   │       └── SessionResponse.java
│   │
│   ├── notification/                     # Notification Module
│   │   ├── service/
│   │   │   └── EmailService.java
│   │   ├── listener/
│   │   │   └── NotificationEventListener.java
│   │   ├── template/
│   │   │   └── EmailTemplateEngine.java
│   │   ├── entity/
│   │   │   └── EmailLog.java
│   │   └── repository/
│   │       └── EmailLogRepository.java
│   │
│   └── audit/                            # Audit Module
│       ├── service/
│       │   └── AuditService.java
│       ├── repository/
│       │   └── AuditLogRepository.java
│       ├── entity/
│       │   └── AuditLog.java
│       ├── aspect/
│       │   ├── Auditable.java
│       │   └── AuditAspect.java
│       └── listener/
│           └── AuditEventListener.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── db/migration/                     # Flyway migrations
│   │   ├── V1__create_schemas.sql
│   │   ├── V2__auth_tables.sql
│   │   ├── V3__user_tables.sql
│   │   ├── V4__seller_tables.sql
│   │   ├── V5__product_tables.sql
│   │   ├── V6__notification_tables.sql
│   │   ├── V7__audit_tables.sql
│   │   └── V8__seed_admin_vikram.sql
│   └── templates/email/
│       ├── verification.html
│       ├── password-reset.html
│       ├── seller-approved.html
│       ├── seller-rejected.html
│       └── seller-suspended.html
│
├── src/test/java/com/kreya/
│   ├── ArchitectureTests.java            # Spring Modulith boundary tests
│   ├── auth/
│   │   ├── AuthControllerTest.java
│   │   ├── AuthServiceTest.java
│   │   └── AuthIntegrationTest.java
│   ├── user/
│   │   ├── UserControllerTest.java
│   │   └── UserServiceTest.java
│   ├── seller/
│   │   ├── SellerControllerTest.java
│   │   ├── SellerServiceTest.java
│   │   └── SellerApprovalFlowIntegrationTest.java
│   ├── product/
│   │   ├── ProductControllerTest.java
│   │   ├── ProductServiceTest.java
│   │   ├── CategoryServiceTest.java
│   │   └── CategoryTreeTest.java
│   ├── cart/
│   │   ├── CartControllerTest.java
│   │   └── CartServiceTest.java
│   ├── admin/
│   │   ├── AdminSellerControllerTest.java
│   │   ├── AdminSessionControllerTest.java
│   │   └── AdminIntegrationTest.java
│   ├── notification/
│   │   └── NotificationEventListenerTest.java
│   └── audit/
│       ├── AuditServiceTest.java
│       └── AuditAspectTest.java
│
└── .github/
    ├── workflows/
    │   └── ci.yml
    └── PULL_REQUEST_TEMPLATE.md
```

---

## Appendix A: Initial Data Seed

```sql
-- V8__seed_admin_vikram.sql
-- Seed the super admin (Vikram)
-- Password must be set manually or via environment variable during first deployment

INSERT INTO user_schema.users (email, first_name, last_name, role, is_vikram, active, created_at, updated_at)
VALUES ('vikram@kreya.com', 'Vikram', 'Bhatt', 'ADMIN', true, true, NOW(), NOW());

-- Credential to be inserted via application startup with BCrypt-hashed password from env
```

## Appendix B: Non-PII Fields (Admin View)

When admin views user/seller details, only these fields are shown:

| Field               | Shown to Admin |
|---------------------|---------------|
| User ID             | Yes           |
| First Name          | Yes           |
| Role                | Yes           |
| Registration Date   | Yes           |
| Email Verified      | Yes           |
| Last Login          | Yes           |
| Account Status      | Yes           |
| Email               | **No** (masked: v***@gmail.com) |
| Phone               | **No** (masked: ***-***-1234)   |
| Password            | **Never**     |
| Full Documents      | View-only during approval       |

## Appendix C: Admin Vintage UI Reference

The admin portal follows a **2003 Goldman Sachs Private Wealth Management / early Amazon** aesthetic:

- Monospace or serif fonts (Georgia, Times New Roman, Courier)
- Dense data tables with thin borders
- Minimal whitespace — information-dense layouts
- Muted color palette: navy, dark grey, off-white, gold accents
- No rounded corners, no shadows, no gradients
- Plain HTML form elements (native selects, checkboxes, radio buttons)
- Status indicators: colored text, not badges or pills
- Navigation: simple top bar or left sidebar with text links
- No animations, no transitions, no loading spinners (use text: "Loading...")
- Favicon: simple monogram or plain icon

This aesthetic communicates **raw power and control** — every pixel serves a function.
