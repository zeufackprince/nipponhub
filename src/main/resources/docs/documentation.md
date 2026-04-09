# Nipponhub API Documentation

## Overview

Nipponhub is a Spring Boot REST API for managing a product catalogue, user accounts, purchases (achats), and sales (ventes). Images are stored in **MongoDB GridFS**; all relational data (users, products, categories, countries, purchases, sales) live in **MySQL**.

Base URL: `http://localhost:8080`

---

## Architecture Summary

```
Client
  │
  ├── HTTP Request
  │       │
  │   JWTAuthFilter          ← validates Bearer token on every request
  │       │
  │   SecurityFilterChain    ← route-level access rules
  │       │
  │   Controller layer       ← HTTP plumbing only (parse → call → wrap)
  │       │
  │   Service layer          ← all business logic lives here
  │       │
  ├── MySQL (JPA/Hibernate)  ← users, products, categories, countries,
  │                             purchases, sales, product_images (GridFS ids)
  │
  └── MongoDB GridFS         ← binary image storage
        └── file_metadata    ← MongoDB collection mirroring GridFS entries
```

**Dual-database pattern for images:**
Every uploaded image is stored in MongoDB GridFS. The returned hex ObjectId is saved in MySQL (either in `ourusers.images` for profile pictures, or in the `product_images` join table for product images). To load an image, the client calls `GET /file/{gridFsId}`.

---

## Security

### Authentication

All protected endpoints require a JWT Bearer token in the `Authorization` header:

```
Authorization: Bearer <token>
```

Tokens are issued by `POST /auth/login` and expire after **24 hours**. A refresh token (same TTL) can be exchanged for a new access token via `POST /auth/refresh`.

### Roles

| Role       | Description                        |
|------------|------------------------------------|
| `ADMIN`    | Full system access                 |
| `USER`     | Standard authenticated user        |
| `CUSTOMER` | External customer role             |
| `PARTNER`  | Partner/supplier role              |
| `GUEST`    | Read-only guest access             |

### Endpoint Access Matrix

| Path pattern         | Access rule                        |
|----------------------|------------------------------------|
| `/auth/**`           | Public — no token required         |
| `/file/**`           | Public — no token required         |
| `/api/user/**`       | Public (see note below)            |
| `/admin/**`          | `ADMIN` role only                  |
| `/user/**`           | `USER` role only                   |
| `/adminuser/**`      | `ADMIN` or `USER`                  |
| Everything else      | Any authenticated user             |

> **Important note:** The `SecurityConfig` permits `/api/user/**` as public. The controller endpoints under `/api/admin/**` and `/api/adminuser/**` fall into the "any authenticated user" bucket at the filter level, but individual methods add `@PreAuthorize("hasRole('ADMIN')")` for admin-only operations. Always send a valid token for any `/api/**` endpoint to avoid a `403`.

---

## Data Models Reference

### `OurUsers` (MySQL — table: `ourusers`)

| Field       | Type       | Notes                                      |
|-------------|------------|--------------------------------------------|
| `userid`    | `Long`     | Primary key                                |
| `name`      | `String`   |                                            |
| `email`     | `String`   | Unique; used as the JWT subject            |
| `telephone` | `String`   | Stored as `telephone_num`                  |
| `password`  | `String`   | BCrypt-encoded; never returned in responses|
| `images`    | `String`   | GridFS ObjectId of the profile picture     |
| `role`      | `UserRole` | Enum: ADMIN, USER, CUSTOMER, PARTNER, GUEST|
| `createdAt` | `Date`     | Set automatically on first save            |

### `Product` (MySQL — table: `product`)

| Field           | Type              | Notes                                          |
|-----------------|-------------------|------------------------------------------------|
| `idProd`        | `Long`            | Primary key                                    |
| `prodName`      | `String`          |                                                |
| `unitPrice`     | `BigDecimal`      | Purchase cost                                  |
| `soldPrice`     | `BigDecimal`      | Default sale price                             |
| `prodQty`       | `Integer`         | Current stock level                            |
| `prodDescription`| `String`         |                                                |
| `prodUrl`       | `List<String>`    | GridFS ObjectIds; stored in `product_images`   |
| `categoriesProd`| `CategoriesProd`  | Many-to-one                                    |
| `countries`     | `List<Country>`   | Many-to-many via `product_country`             |
| `createdAt`     | `LocalDateTime`   | Set automatically on first save                |

### `CategoriesProd` (MySQL — table: `categories_prod`)

| Field        | Type     | Notes       |
|--------------|----------|-------------|
| `idCatProd`  | `Long`   | Primary key |
| `catProdName`| `String` | Unique      |
| `catProdDes` | `String` | Description |

### `Country` (MySQL — table: `country`)

| Field        | Type     | Notes                  |
|--------------|----------|------------------------|
| `idCountry`  | `Long`   | Primary key            |
| `countryName`| `String` |                        |
| `countryCode`| `String` | Unique (e.g. `"JP"`)   |
| `createdAt`  | `LocalDateTime` | Auto-set         |

### `Achats` (MySQL)

| Field  | Type             | Notes                             |
|--------|------------------|-----------------------------------|
| `Id`   | `Long`           | Primary key                       |
| `date` | `LocalDate`      | Auto-set to today by service      |
| `items`| `List<AchatItem>`| Cascade-persisted with the parent |

### `AchatItem` (MySQL)

| Field     | Type      | Notes                     |
|-----------|-----------|---------------------------|
| `id`      | `Long`    | Primary key               |
| `product` | `Product` | Must have `idProd` set    |
| `quantite`| `int`     | Units purchased           |
| `achat`   | `Achats`  | Back-reference (set by service) |

### `Vente` (MySQL)

| Field  | Type              | Notes                             |
|--------|-------------------|-----------------------------------|
| `Id`   | `Long`            | Primary key                       |
| `date` | `LocalDate`       | Auto-set to today by service      |
| `items`| `List<VenteItem>` | Cascade-persisted with the parent |

### `VenteItem` (MySQL)

| Field      | Type        | Notes                                                            |
|------------|-------------|------------------------------------------------------------------|
| `id`       | `Long`      | Primary key                                                      |
| `product`  | `Product`   | Must have `idProd` set                                           |
| `quantite` | `int`       | Units sold                                                       |
| `prixVendu`| `BigDecimal`| Actual sale price per unit. If `null` or `0`, defaults to `unitPrice` |
| `prix`     | `BigDecimal`| Total line cost — set by service                                 |
| `gain`     | `BigDecimal`| Profit per line — set by service                                 |

### `FileDocument` (MongoDB — collection: `file_metadata`)

| Field        | Type            | Notes                                   |
|--------------|-----------------|-----------------------------------------|
| `id`         | `String`        | MongoDB `_id`                           |
| `gridFsId`   | `String`        | GridFS ObjectId hex string              |
| `originalName`| `String`       | Original filename from the upload       |
| `contentType`| `String`        | MIME type e.g. `image/jpeg`             |
| `size`       | `Long`          | File size in bytes                      |
| `uploadedAt` | `LocalDateTime` | Defaults to upload timestamp            |
| `productId`  | `Long`          | Optional link to a MySQL product id     |

---

## DTO Shapes

### `ReqRes` — User request / response envelope

```json
{
  "statusCode": 200,
  "message":    "...",
  "error":      "...",
  "token":      "eyJ...",
  "refreshToken": "eyJ...",
  "expirationTime": "24Hrs",
  "userId":     1,
  "name":       "Alice",
  "email":      "alice@example.com",
  "telephone":  "+237600000000",
  "role":       "USER",
  "poster":     "<gridFsObjectId>",
  "posterUrl":  "http://localhost:8080/file/<gridFsObjectId>"
}
```

`password` is an inbound-only field and is **never** present in responses. Fields that are `null` are omitted from the JSON output.

### `ProductDto`

```json
{
  "IdProd": 1,
  "ProdName": "Blue Backpack",
  "UnitPrice": 10.00,
  "SoldPrice": 15.00,
  "ProdQty": 20,
  "Message": "Success",
  "categoryName": "Bags",
  "prodDescription": "Waterproof travel backpack",
  "countries": ["Japan"],
  "ProdUrl": ["<gridFsId1>", "<gridFsId2>"],
  "createdAt": "2026-01-15T10:30:00"
}
```

When a service error occurs, `Message` is set to the error text and other fields may be absent.

### `AchatDto`

```json
{
  "Id": 1,
  "date": "2026-03-17",
  "coutTotal": 50.00,
  "totalItem": 5,
  "items": [
    {
      "productId": 1,
      "productName": "Blue Backpack",
      "quantite": 5,
      "prixUnitaire": 10.00,
      "total": 50.00
    }
  ],
  "message": "Achat enregistré avec succès"
}
```

### `VenteDto`

```json
{
  "id": 1,
  "date": "2026-03-17",
  "coutTotal": 20.00,
  "prixVendu": 40.00,
  "gain": 20.00,
  "totalItem": 2,
  "items": [
    {
      "productId": 1,
      "productName": "Blue Backpack",
      "quantite": 2,
      "prixUnitaire": 10.00,
      "prixVendu": 20.00,
      "total": 40.00,
      "gain": 20.00
    }
  ],
  "message": "Vente enregistrée avec succès"
}
```

### `CategoryDto`

```json
{ "idProd": 1, "catProdName": "Bags", "catProdDes": "Travel bags" }
```

### `CountryDto`

```json
{ "idCountry": 1, "countryName": "Japan", "countryCode": "JP" }
```

---

## 1) Authentication Endpoints

Base path: `/auth` — **public, no token required**

---

### 1.1 POST /auth/register

Register a new user account with an optional profile picture.

Consumes: `multipart/form-data`

Returns: `201 Created` with `ReqRes`

| Parameter    | Type              | Required | Notes                                   |
|--------------|-------------------|----------|-----------------------------------------|
| `name`       | string            | Yes      |                                         |
| `email`      | string            | Yes      | Must be unique                          |
| `password`   | string            | Yes      | Stored BCrypt-encoded                   |
| `telephone`  | string            | No       |                                         |
| `role`       | `UserRole` enum   | No       | Defaults to `USER` if omitted           |
| `profileImg` | file              | No       | Must be `image/*` MIME type             |

If no role is supplied, the service assigns `USER` automatically.

```bash
curl -X POST http://localhost:8080/auth/register \
  -F "name=Alice" \
  -F "email=alice@example.com" \
  -F "password=secret123" \
  -F "telephone=+237600000000" \
  -F "role=USER" \
  -F "profileImg=@./avatar.jpg"
```

Success response:
```json
{
  "statusCode": 200,
  "message": "User registered successfully",
  "userId": 1,
  "name": "Alice",
  "email": "alice@example.com",
  "telephone": "+237600000000",
  "role": "USER",
  "poster": "6500f7b8a4440f1aaad12f65",
  "posterUrl": "http://localhost:8080/file/6500f7b8a4440f1aaad12f65"
}
```

Common errors:
- `500` with `error: "A user with email [...] already exists."` → use a different email.
- `400` with `error: "Only image files are allowed."` → send a valid image file.

---

### 1.2 POST /auth/login

Authenticate with email and password. Returns JWT access and refresh tokens.

Consumes: `application/json`

Returns: `200 OK` with `ReqRes`

Request body:
```json
{ "email": "alice@example.com", "password": "secret123" }
```

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123"}'
```

Success response:
```json
{
  "statusCode": 200,
  "message": "Successfully logged in",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expirationTime": "24Hrs",
  "role": "USER"
}
```

Common errors:
- `500` with the Spring Security `BadCredentialsException` message → verify email and password are correct.

---

### 1.3 POST /auth/refresh

Exchange a still-valid refresh token for a new access token.

Consumes: `application/json`

Returns: `200 OK` with `ReqRes`

Request body:
```json
{ "token": "<refresh_token_string>" }
```

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"token":"eyJhbGciOiJIUzI1NiJ9..."}'
```

Success response:
```json
{
  "statusCode": 200,
  "message": "Token refreshed successfully",
  "token": "<new_access_token>",
  "refreshToken": "<same_refresh_token>",
  "expirationTime": "24Hrs"
}
```

Common errors:
- `401` → refresh token is expired or tampered with. Call `/auth/login` again to obtain fresh tokens.

---

## 2) User Management Endpoints

### 2.1 PUT /api/user/update

Update the currently authenticated user's own profile. Only fields that are provided (non-null, non-blank) are changed — omitting a field leaves it unchanged.

Consumes: `multipart/form-data`

Requires: valid Bearer token (any authenticated user)

Returns: `200 OK` with updated `ReqRes`

| Parameter   | Type            | Required | Notes                                            |
|-------------|-----------------|----------|--------------------------------------------------|
| `name`      | string          | No       |                                                  |
| `email`     | string          | No       |                                                  |
| `password`  | string          | No       | Will be BCrypt-encoded before saving             |
| `telephone` | string          | No       |                                                  |
| `role`      | `UserRole` enum | No       |                                                  |
| `file`      | file            | No       | Replaces old profile picture; old GridFS file is deleted |

The user id is resolved server-side from the JWT — it cannot be supplied in the request.

```bash
curl -X PUT http://localhost:8080/api/user/update \
  -H "Authorization: Bearer <token>" \
  -F "telephone=+237699999999" \
  -F "file=@./new_avatar.jpg"
```

---

### 2.2 GET /api/adminuser/get-profile

Return the currently authenticated user's own profile.

Requires: valid Bearer token (ADMIN or USER)

Returns: `200 OK` with `ReqRes`

```bash
curl http://localhost:8080/api/adminuser/get-profile \
  -H "Authorization: Bearer <token>"
```

---

### 2.3 GET /api/admin/get-all-users

Return all users in the system.

Requires: `ADMIN` role

Returns: `200 OK` array of `ReqRes`

```bash
curl http://localhost:8080/api/admin/get-all-users \
  -H "Authorization: Bearer <admin_token>"
```

---

### 2.4 GET /api/admin/users/{role}

Return all users with a specific role.

Requires: `ADMIN` role

Path parameter: `role` — one of `ADMIN`, `USER`, `CUSTOMER`, `PARTNER`, `GUEST`

Returns: `200 OK` array of `ReqRes`

```bash
curl http://localhost:8080/api/admin/users/USER \
  -H "Authorization: Bearer <admin_token>"
```

---

### 2.5 GET /api/user/get-users/{userId}

Fetch a specific user by their MySQL id.

Requires: `ADMIN` role (`@PreAuthorize`)

Path parameter: `userId` (Long)

Returns: `200 OK` with `ReqRes`

```bash
curl http://localhost:8080/api/user/get-users/1 \
  -H "Authorization: Bearer <admin_token>"
```

---

### 2.6 DELETE /api/admin/delete/{userId}

Permanently delete a user. The user's profile picture is also deleted from GridFS before the row is removed from MySQL.

Requires: `ADMIN` role

Path parameter: `userId` (Long)

Returns: `200 OK` with `ReqRes`

```bash
curl -X DELETE http://localhost:8080/api/admin/delete/3 \
  -H "Authorization: Bearer <admin_token>"
```

Success response:
```json
{ "statusCode": 200, "message": "User deleted successfully" }
```

---

## 3) Product Endpoints

Base path: `/api/v0/product`

Requires: valid Bearer token (any authenticated user) unless noted.

---

### 3.1 POST /api/v0/product/newProduct

Create a new product with optional image upload(s).

Consumes: `multipart/form-data`

Returns:
- `201 Created` with `ProductDto` on success
- `400 Bad Request` with `{ "error": "..." }` if validation fails
- `500 Internal Server Error` on storage failure (uploaded GridFS files are rolled back)

| Parameter        | Type              | Required | Notes                              |
|------------------|-------------------|----------|------------------------------------|
| `ProdName`       | string            | Yes      |                                    |
| `UnitPrice`      | decimal           | Yes      | Purchase cost                      |
| `SoldPrice`      | decimal           | Yes      | Default selling price              |
| `ProdQty`        | integer           | Yes      | Must be ≥ 0                        |
| `prodDescription`| string            | Yes      |                                    |
| `category`       | string            | Yes      | Must match an existing `catProdName` |
| `Country`        | string[]          | Yes      | At least one; must match existing country names |
| `ProdUrl`        | file[]            | No       | One or more `image/*` files        |

```bash
curl -X POST http://localhost:8080/api/v0/product/newProduct \
  -H "Authorization: Bearer <token>" \
  -F "ProdName=Blue Backpack" \
  -F "UnitPrice=10.00" \
  -F "SoldPrice=15.00" \
  -F "ProdQty=20" \
  -F "category=Bags" \
  -F "prodDescription=Waterproof travel backpack" \
  -F "Country=Japan" \
  -F "ProdUrl=@./backpack.jpg"
```

Success response:
```json
{
  "IdProd": 1,
  "ProdName": "Blue Backpack",
  "UnitPrice": 10.00,
  "SoldPrice": 15.00,
  "ProdQty": 20,
  "Message": "Success",
  "categoryName": "Bags",
  "prodDescription": "Waterproof travel backpack",
  "countries": ["Japan"],
  "ProdUrl": ["6500f7b8a4440f1aaad12f65"],
  "createdAt": "2026-03-17T10:00:00"
}
```

Common errors:
- `400 "Product name is required"` → supply `ProdName`.
- `400 "Category is required"` → supply `category`.
- `400 "At least one country is required"` → supply at least one `Country`.
- `400 "Only image files are allowed."` → upload a file with a valid `image/*` MIME type.
- `RuntimeException "Category not found: ..."` (surfaces as 500) → create the category first via `POST /api/v0/categories/createCategory`.
- `RuntimeException "No countries found for: ..."` (surfaces as 500) → create the countries first.

---

### 3.2 PUT /api/v0/product/updateProduct/{idProd}

Partially update an existing product. Any omitted field retains its current value.

Consumes: `multipart/form-data`

Returns:
- `200 OK` with updated `ProductDto`
- `500 Internal Server Error` with `ProductDto` (check the `Message` field for details)

Path parameter: `idProd` (Long)

Optional fields: same as `newProduct` — `ProdName`, `UnitPrice`, `SoldPrice`, `ProdQty`, `ProdUrl`, `Country`, `category`, `prodDescription`.

If new images are provided, the old GridFS files are deleted only after the MySQL save succeeds. If the save fails, newly uploaded files are rolled back.

```bash
curl -X PUT http://localhost:8080/api/v0/product/updateProduct/1 \
  -H "Authorization: Bearer <token>" \
  -F "ProdName=Updated Backpack" \
  -F "ProdQty=15"
```

---

### 3.3 GET /api/v0/product/all

Return all products.

Returns: `200 OK` array of `ProductDto`. Returns an empty array `[]` if none exist.

```bash
curl http://localhost:8080/api/v0/product/all \
  -H "Authorization: Bearer <token>"
```

---

### 3.4 GET /api/v0/product/{idProd}

Retrieve a product by its MySQL id.

Returns:
- `200 OK` with `ProductDto`
- `404 Not Found` with `ProductDto` where `Message` contains the error

```bash
curl http://localhost:8080/api/v0/product/1 \
  -H "Authorization: Bearer <token>"
```

---

### 3.5 GET /api/v0/product/searchByName?prodName=...

Search a single product by name (case-insensitive).

Query parameter: `prodName` (string)

Returns:
- `200 OK` with `ProductDto`
- `404 Not Found` with `ProductDto` where `Message` contains the error

```bash
curl "http://localhost:8080/api/v0/product/searchByName?prodName=blue%20backpack" \
  -H "Authorization: Bearer <token>"
```

---

### 3.6 GET /api/v0/product/searchByCountry?country=...

Return all products associated with a country (by exact country name).

Query parameter: `country` (string)

Returns: `200 OK` array of `ProductDto`. Returns an empty array `[]` if no products are found for that country.

```bash
curl "http://localhost:8080/api/v0/product/searchByCountry?country=Japan" \
  -H "Authorization: Bearer <token>"
```

---

### 3.7 GET /api/v0/product/greetings

Health-check endpoint. No authentication required in practice (hits the `anyRequest().authenticated()` rule but useful for quick checks).

Returns: `200 OK` plain string.

```bash
curl http://localhost:8080/api/v0/product/greetings
```

---

## 4) Category Endpoints

Base path: `/api/v0/categories`

---

### 4.1 POST /api/v0/categories/createCategory

Create a new product category.

Consumes: `application/json` with `CategoryDto`

Returns:
- `201 Created` with `{ "message": "Category Created Successfully" }`
- `500 Internal Server Error` with `{ "message": "Error Creating Category: ..." }`

Request body:
```json
{
  "catProdName": "Bags",
  "catProdDes": "Travel and laptop bags"
}
```

```bash
curl -X POST http://localhost:8080/api/v0/categories/createCategory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"catProdName":"Bags","catProdDes":"Travel and laptop bags"}'
```

Common errors:
- `"Error Creating Category: name must not be blank"` → provide `catProdName`.
- `"Error Creating Category: 'Bags' already exists"` → that category name is taken; use a different name.

---

### 4.2 GET /api/v0/categories/getAllCategories

Return all categories.

Returns: `200 OK` array of `CategoryDto`.

```bash
curl http://localhost:8080/api/v0/categories/getAllCategories \
  -H "Authorization: Bearer <token>"
```

---

### 4.3 GET /api/v0/categories/{id}

Fetch a single category by its primary key.

Returns:
- `200 OK` with the `CategoriesProd` entity
- `404 Not Found` with `{ "error": "Category not found with id: <id>" }`

```bash
curl http://localhost:8080/api/v0/categories/1 \
  -H "Authorization: Bearer <token>"
```

---

## 5) Country Endpoints

Base path: `/api/v0/country`

---

### 5.1 POST /api/v0/country/newCountry

Create a new country record.

Consumes: `application/json` with `CountryDto`

Returns: `200 OK` with created `CountryDto`

```json
{ "countryName": "Japan", "countryCode": "JP" }
```

```bash
curl -X POST http://localhost:8080/api/v0/country/newCountry \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"countryName":"Japan","countryCode":"JP"}'
```

Success response:
```json
{
  "idCountry": 1,
  "countryName": "Japan",
  "countryCode": "JP",
  "message": "Country Created Successfully..."
}
```

Common errors:
- Duplicate `countryCode` causes a database unique constraint violation → use a different code.

---

### 5.2 DELETE /api/v0/country/deleteCountry

Delete a country by id.

> **Known issue:** The route is declared as `/deleteCountry/{idCountry}` in `@DeleteMapping` but the method parameter uses `@RequestParam`. Pass the id as a **query parameter**, not a path segment.

Query parameter: `idCountry` (Long)

Returns: `200 OK` plain string

```bash
curl -X DELETE "http://localhost:8080/api/v0/country/deleteCountry?idCountry=1" \
  -H "Authorization: Bearer <token>"
```

---

### 5.3 GET /api/v0/country/getAllCountries

Return all countries.

Returns: `200 OK` array of `Country` entities.

```bash
curl http://localhost:8080/api/v0/country/getAllCountries \
  -H "Authorization: Bearer <token>"
```

---

## 6) File / Image Endpoints

Base path: `/file` — **public, no token required**

All images are stored in MongoDB GridFS. The `fileId` path parameter is a 24-character hex string MongoDB ObjectId (e.g. `6500f7b8a4440f1aaad12f65`).

---

### 6.1 POST /file/upload

Upload a single image file.

Consumes: `multipart/form-data`

Returns:
- `201 Created` with `{ "fileId": "...", "message": "File uploaded successfully" }`
- `400 Bad Request` if the file is not an image
- `500 Internal Server Error` on GridFS failure

| Parameter   | Type   | Required | Notes                          |
|-------------|--------|----------|--------------------------------|
| `file`      | file   | Yes      | Must be `image/*`              |
| `productId` | Long   | No       | Links the file to a product    |

```bash
curl -X POST http://localhost:8080/file/upload \
  -F "file=@./photo.jpg" \
  -F "productId=1"
```

Success response:
```json
{ "fileId": "6500f7b8a4440f1aaad12f65", "message": "File uploaded successfully" }
```

---

### 6.2 POST /file/upload/batch

Upload multiple image files at once.

Consumes: `multipart/form-data`

Returns:
- `201 Created` with `{ "fileIds": [...], "count": N, "message": "Files uploaded successfully" }`

| Parameter   | Type     | Required | Notes                           |
|-------------|----------|----------|---------------------------------|
| `files`     | file[]   | Yes      | Each must be `image/*`          |
| `productId` | Long     | No       | Links all files to one product  |

```bash
curl -X POST http://localhost:8080/file/upload/batch \
  -F "files=@./img1.jpg" \
  -F "files=@./img2.jpg" \
  -F "productId=1"
```

---

### 6.3 GET /file/{fileId}

Stream an image directly (binary response). Sets the correct `Content-Type` header from stored metadata.

Returns:
- `200 OK` binary image stream
- `400 Bad Request` if `fileId` is not a valid ObjectId hex string
- `404 Not Found` if no file exists for that id

```bash
# View in browser or save to disk:
curl http://localhost:8080/file/6500f7b8a4440f1aaad12f65 --output downloaded.jpg
```

Use this URL as an `<img src="...">` value in a frontend application.

---

### 6.4 GET /file/metadata/{fileId}

Return the `FileDocument` metadata for a given GridFS id (no binary content).

Returns:
- `200 OK` with `FileDocument`
- `404 Not Found` with `{ "error": "Metadata not found for fileId: ..." }`

```bash
curl http://localhost:8080/file/metadata/6500f7b8a4440f1aaad12f65
```

---

### 6.5 GET /file/product/{productId}

Return all `FileDocument` metadata records associated with a product.

Returns:
- `200 OK` array of `FileDocument`
- `404 Not Found` (empty array body) if no files are linked to that product

```bash
curl http://localhost:8080/file/product/1
```

---

### 6.6 DELETE /file/{fileId}

Delete a file from GridFS and remove its metadata document.

Returns:
- `200 OK` with `{ "message": "File deleted successfully" }`
- `400 Bad Request` if the `fileId` format is invalid
- `500 Internal Server Error` on deletion failure

```bash
curl -X DELETE http://localhost:8080/file/6500f7b8a4440f1aaad12f65
```

---

## 7) Achat (Purchase) Endpoints

Base path: `/api/v0/achat`

When an achat is registered, stock is **increased** for each product via `ProductService.ajouterQuantite()`.

---

### 7.1 POST /api/v0/achat/new-achat

Register a new purchase and add quantities to product stock.

Consumes: `application/json` with the `Achats` model

Returns: `201 Created` with `AchatDto`

The `date` field is ignored in the request — the service always sets it to today's date.

Each item in the `items` array must include a `product` object with the `idProd` field set (the other product fields can be omitted; the service fetches the full product from the database).

Request body:
```json
{
  "items": [
    {
      "product": { "idProd": 1 },
      "quantite": 10
    },
    {
      "product": { "idProd": 2 },
      "quantite": 5
    }
  ]
}
```

```bash
curl -X POST http://localhost:8080/api/v0/achat/new-achat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"items":[{"product":{"idProd":1},"quantite":10}]}'
```

Success response:
```json
{
  "Id": 1,
  "date": "2026-03-17",
  "coutTotal": 100.00,
  "totalItem": 10,
  "items": [
    {
      "productId": 1,
      "productName": "Blue Backpack",
      "quantite": 10,
      "prixUnitaire": 10.00,
      "total": 100.00
    }
  ],
  "message": "Achat enregistré avec succès"
}
```

Common errors:
- `EntityNotFoundException "Produit not found with ID: ..."` → verify the `idProd` value exists.

---

### 7.2 GET /api/v0/achat/get-all-achat

Return all purchase records.

Returns: `200 OK` array of `AchatDto`

```bash
curl http://localhost:8080/api/v0/achat/get-all-achat \
  -H "Authorization: Bearer <token>"
```

---

## 8) Vente (Sale) Endpoints

Base path: `/api/v0/vente`

When a vente is registered, stock is **decreased** for each product via `ProductService.retirerQuantite()`. If stock is insufficient, the entire transaction is rolled back.

---

### 8.1 POST /api/v0/vente/register

Register a sale, deduct stock, and calculate profit.

Consumes: `application/json` with the `Vente` model

Returns: `201 Created` with `VenteDto`

The `date` is auto-set to today. Each item must have `product.idProd`. The `prixVendu` field is optional — if `null` or `0`, the product's `unitPrice` is used and `gain` will be `0`.

Request body:
```json
{
  "items": [
    {
      "product": { "idProd": 1 },
      "quantite": 2,
      "prixVendu": 20.00
    }
  ]
}
```

```bash
curl -X POST http://localhost:8080/api/v0/vente/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"items":[{"product":{"idProd":1},"quantite":2,"prixVendu":20.00}]}'
```

Success response:
```json
{
  "id": 1,
  "date": "2026-03-17",
  "coutTotal": 20.00,
  "prixVendu": 40.00,
  "gain": 20.00,
  "totalItem": 2,
  "items": [
    {
      "productId": 1,
      "productName": "Blue Backpack",
      "quantite": 2,
      "prixUnitaire": 10.00,
      "prixVendu": 20.00,
      "total": 40.00,
      "gain": 20.00
    }
  ],
  "message": "Vente enregistrée avec succès"
}
```

Common errors:
- `EntityNotFoundException "Produit non trouvé avec ID: ..."` → verify `idProd`.
- `IllegalStateException "Insufficient stock. Current: X, requested: Y"` → reduce quantity or top up stock via an achat.

---

### 8.2 GET /api/v0/vente/all

Return all sales.

Returns: `200 OK` array of `VenteDto`

Throws `EntityNotFoundException` (surfaces as `500`) if no sales exist yet.

```bash
curl http://localhost:8080/api/v0/vente/all \
  -H "Authorization: Bearer <token>"
```

---

### 8.3 GET /api/v0/vente/{id}

Retrieve a sale by its id.

Path parameter: `id` (Long)

Returns: `200 OK` with `VenteDto`

Throws `EntityNotFoundException` if not found.

```bash
curl http://localhost:8080/api/v0/vente/1 \
  -H "Authorization: Bearer <token>"
```

---

### 8.4 GET /api/v0/vente/search?date=YYYY-MM-DD

Search all sales made on a specific date.

Query parameter: `date` in `YYYY-MM-DD` format

Returns: `200 OK` array of `VenteDto`

Throws `EntityNotFoundException` if no sales exist for that date.

```bash
curl "http://localhost:8080/api/v0/vente/search?date=2026-03-17" \
  -H "Authorization: Bearer <token>"
```

---

## 9) Common Errors Reference

| HTTP Status | Typical cause | Resolution |
|-------------|---------------|------------|
| `400 Bad Request` | Malformed JSON body, invalid path/query param, non-image file uploaded, invalid GridFS ObjectId format | Check field names, types, and values; ensure files are `image/*` |
| `401 Unauthorized` | Expired or invalid token | Re-authenticate via `POST /auth/login` or refresh via `POST /auth/refresh` |
| `403 Forbidden` | Token is valid but role is insufficient | Use an account with the required role |
| `404 Not Found` | Requested entity does not exist in the database | Verify the id or name via a list endpoint first |
| `500 Internal Server Error` | Database error, GridFS failure, constraint violation, or uncaught exception in service | Check application logs; verify MySQL and MongoDB are running; check for duplicate unique fields |

### DTO-level errors

Some endpoints return `200 OK` but with an error embedded in the DTO:

- **`ProductDto.Message`** — if this field is anything other than `"Success"`, the service encountered an error. Read the value for details.
- **`ReqRes.error`** — set when a service operation fails mid-flow (e.g. during user update).
- **`ReqRes.statusCode`** — always check this; it reflects the actual outcome even when the HTTP status is `200`.

---

## 10) Workflow Guide

### Register → Login → Call protected endpoints

```bash
# 1. Register
curl -X POST http://localhost:8080/auth/register \
  -F "name=Alice" -F "email=alice@example.com" -F "password=secret"

# 2. Login — capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 3. Use the token
curl http://localhost:8080/api/v0/product/all -H "Authorization: Bearer $TOKEN"
```

### Create a product (full prerequisite chain)

```bash
# Step 1 — create a category
curl -X POST http://localhost:8080/api/v0/categories/createCategory \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"catProdName":"Bags","catProdDes":"Travel bags"}'

# Step 2 — create a country
curl -X POST http://localhost:8080/api/v0/country/newCountry \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"countryName":"Japan","countryCode":"JP"}'

# Step 3 — create the product
curl -X POST http://localhost:8080/api/v0/product/newProduct \
  -H "Authorization: Bearer $TOKEN" \
  -F "ProdName=Blue Backpack" -F "UnitPrice=10" -F "SoldPrice=15" \
  -F "ProdQty=0" -F "category=Bags" -F "Country=Japan" \
  -F "prodDescription=Waterproof" -F "ProdUrl=@./img.jpg"
```

### Register a purchase (add stock) then register a sale

```bash
# Add 20 units of product id=1
curl -X POST http://localhost:8080/api/v0/achat/new-achat \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"items":[{"product":{"idProd":1},"quantite":20}]}'

# Sell 2 units at 20.00 each
curl -X POST http://localhost:8080/api/v0/vente/register \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"items":[{"product":{"idProd":1},"quantite":2,"prixVendu":20.00}]}'
```

---

## 11) Configuration Notes

- **`app.base-url`** — set this in `application.properties`. It is prepended to all `posterUrl` and `ProdUrl` image links returned in responses.
  ```properties
  app.base-url=http://localhost:8080
  ```

- **JWT secret** — currently hardcoded in `JWTUtils.java`. For production, move it to an environment variable or secrets manager.

- **MongoDB database name** — defaults to `nipponhubfiles`; override via `spring.data.mongodb.database` in `application.properties`.

- **`CountryController.deleteCountry`** — the `@DeleteMapping` declares `{idCountry}` as a path segment, but the Java method uses `@RequestParam`. Always pass the id as a **query parameter**: `?idCountry=1`.

- **`/api/user/**` is public** — `SecurityConfig` permits all requests to `/api/user/**` without authentication. If you want `PUT /api/user/update` and `GET /api/user/get-users/{userId}` to require a token, change `permitAll()` to `authenticated()` in `SecurityConfig`.