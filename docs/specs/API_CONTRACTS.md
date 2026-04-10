# API Contracts

Frontend-facing API contract reference for the Kreya backend.

This file is the shareable contract surface for the frontend team. Keep it updated whenever:
- a backend endpoint is added
- a request or response body changes
- an auth rule or response status changes

---

## 1. Conventions

### Base Path

```text
/api/v1
```

### Content Type

```http
Content-Type: application/json
```

### Standard Response Envelope

All API responses use the shared envelope below.

```json
{
  "success": true,
  "data": {},
  "message": "Operation successful",
  "timestamp": "2026-04-10T10:15:30Z",
  "path": "/api/v1/example",
  "errors": null
}
```

### Error Envelope

```json
{
  "success": false,
  "data": null,
  "message": "Validation failed",
  "timestamp": "2026-04-10T10:15:30Z",
  "path": "/api/v1/example",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

### Auth Header

Protected endpoints use bearer token authentication.

```http
Authorization: Bearer <access-token>
```

---

## 2. Auth Module

### 2.1 POST `/api/v1/auth/register`

Status: Implemented

Purpose:
- Register a shopper or seller account

Request:

```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "firstName": "Vikram",
  "lastName": "Bhat",
  "phone": "9999999999",
  "registeringAsSeller": false
}
```

Request fields:
- `email`: string, required, valid email
- `password`: string, required
- `firstName`: string, required
- `lastName`: string, required
- `phone`: string, optional
- `registeringAsSeller`: boolean, optional

Success:
- `201 Created`

Success response:

```json
{
  "success": true,
  "data": null,
  "message": "Registration successful",
  "timestamp": "2026-04-10T10:15:30Z",
  "path": "/api/v1/auth/register",
  "errors": null
}
```

Failure responses:
- `400 Bad Request` for invalid request body
- `401 Unauthorized` currently returned for duplicate registration error via shared `IllegalArgumentException` handling

Notes:
- New users get role `SHOPPER` by default
- If `registeringAsSeller=true`, new users get role `SELLER`
- Password is stored as a hash

### 2.2 POST `/api/v1/auth/login`

Status: Implemented

Purpose:
- Authenticate user and issue JWT tokens

Request:

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

Request fields:
- `email`: string, required, valid email
- `password`: string, required

Success:
- `200 OK`

Success response:

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt-access-token>",
    "refreshToken": "<jwt-refresh-token>",
    "tokenType": "Bearer"
  },
  "message": "Login successful",
  "timestamp": "2026-04-10T10:15:30Z",
  "path": "/api/v1/auth/login",
  "errors": null
}
```

Failure responses:
- `400 Bad Request` for invalid request body
- `401 Unauthorized` for invalid email or password

### 2.3 POST `/api/v1/auth/verify-email`

Status: Planned under `AUTH-004`

Purpose:
- Verify a user's email address using a verification token

Planned request:

```json
{
  "token": "email-verification-token"
}
```

Planned success:
- `200 OK`

Planned success response:

```json
{
  "success": true,
  "data": null,
  "message": "Email verified successfully",
  "timestamp": "2026-04-10T10:15:30Z",
  "path": "/api/v1/auth/verify-email",
  "errors": null
}
```

Planned failure responses:
- `400 Bad Request` for invalid request body
- `401 Unauthorized` or `400 Bad Request` for invalid, expired, or already-used token

### 2.4 Planned Auth Endpoints

Status: Planned

```text
POST /api/v1/auth/mfa/verify
POST /api/v1/auth/password/forgot
POST /api/v1/auth/password/reset
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
```

---

## 3. User Module

Status: Planned

### Planned Endpoints

```text
GET /api/v1/users/me
PUT /api/v1/users/me
PUT /api/v1/users/me/password
```

Expected purpose:
- fetch current user profile
- update current user profile
- update password with current password verification

---

## 4. Seller Module

Status: Planned

### Planned Endpoints

```text
GET /api/v1/seller/profile
PUT /api/v1/seller/profile
POST /api/v1/seller/documents
GET /api/v1/seller/documents
DELETE /api/v1/seller/documents/{id}
GET /api/v1/seller/products
POST /api/v1/seller/products
PUT /api/v1/seller/products/{id}
DELETE /api/v1/seller/products/{id}
PUT /api/v1/seller/products/{id}/stock
POST /api/v1/seller/products/{id}/images
```

Frontend note:
- Seller capabilities are role-gated within the same website, not exposed through a separate seller portal

---

## 5. Product Module

Status: Planned

### Planned Public Endpoints

```text
GET /api/v1/products
GET /api/v1/products/{id}
GET /api/v1/categories
GET /api/v1/categories/tree
```

Expected purpose:
- browse products
- view product detail
- list categories
- render category tree

---

## 6. Cart Module

Status: Planned

### Planned Endpoints

```text
GET /api/v1/cart
POST /api/v1/cart/items
PUT /api/v1/cart/items/{productId}
DELETE /api/v1/cart/items/{productId}
DELETE /api/v1/cart
```

Expected purpose:
- fetch cart
- add to cart
- update quantity
- remove item
- clear cart

---

## 7. Admin Module

Status: Planned

### Planned Endpoints

```text
GET /api/v1/admin/sellers
GET /api/v1/admin/sellers/{id}
GET /api/v1/admin/sellers/{id}/documents
POST /api/v1/admin/sellers/{id}/approve
POST /api/v1/admin/sellers/{id}/reject
POST /api/v1/admin/sellers/{id}/suspend
POST /api/v1/admin/sellers/{id}/unsuspend

GET /api/v1/admin/products
DELETE /api/v1/admin/products/{id}

GET /api/v1/admin/categories
POST /api/v1/admin/categories
PUT /api/v1/admin/categories/{id}
DELETE /api/v1/admin/categories/{id}

GET /api/v1/admin/users
GET /api/v1/admin/users/{id}
POST /api/v1/admin/users/{id}/promote
POST /api/v1/admin/users/{id}/password-reset

GET /api/v1/admin/sessions
GET /api/v1/admin/sessions/user/{id}
DELETE /api/v1/admin/sessions/{sessionId}

GET /api/v1/admin/audit-logs
POST /api/v1/admin/mfa/setup
POST /api/v1/admin/mfa/reauth
```

Frontend note:
- Admin features are part of the same website and should be shown only to users with `ADMIN` role and the required authorization state

---

## 8. Token and Role Notes

### Access Token

Current claims:
- `sub`: user id
- `email`
- `role`

### Refresh Token

Current claims:
- `sub`: user id
- `type`: `refresh`

### Roles

Current role values:
- `SHOPPER`
- `SELLER`
- `ADMIN`

---

## 9. Frontend Integration Notes

- One website serves all user roles
- Login is shared for shopper, seller, and admin
- UI should use Mantine components consistently
- Role-based rendering should happen after login based on token and current backend authorization
- Do not assume every planned endpoint is available yet; check the `Status` field in this file

---

## 10. Change Log

### 2026-04-10
- Created initial frontend-shareable API contract reference
- Added implemented contracts for registration and login
- Added planned contract sections for email verification and upcoming backend modules
