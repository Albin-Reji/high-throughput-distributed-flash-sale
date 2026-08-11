# Project Aegis — User Service Implementation Checklist

> **Last Updated:** August 11, 2026  
> **Architecture:** Keycloak IDP → Event Listener SPI → Webhook → Spring Boot Resource Server → PostgreSQL

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Completed |
| 🔧 | In Progress |
| ⬜ | Not Started |
| 🚫 | Blocked / Deferred |

---

## 1. Infrastructure & Database

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1.1 | PostgreSQL database (`user_service_db`) | ✅ | |
| 1.2 | Spring Boot project setup (Java 21, Spring Boot 4.1) | ✅ | |
| 1.3 | Environment config (`application-dev.yaml`, `.env`) | ✅ | |
| 1.4 | Actuator health endpoint | ✅ | `/actuator/health` |

---

## 2. Entity & Schema Design

| # | Task | Status | Notes |
|---|------|--------|-------|
| 2.1 | `CustomerProfile` entity | ✅ | UUID PK, `keycloakUserId` (unique, indexed) |
| 2.2 | `CustomerAddress` entity | ✅ | FK to customer, address type, default flag |
| 2.3 | `CustomerPreference` entity | ✅ | 1:1 with customer, marketing/sms/currency |
| 2.4 | `AccountStatus` enum | ✅ | ACTIVE, SUSPENDED, DELETED |
| 2.5 | `AddressType` enum | ✅ | DELIVERY, BILLING |
| 2.6 | Add `INACTIVE` to `AccountStatus` enum | ⬜ | Needed for soft-deactivation |

---

## 3. Repositories

| # | Task | Status | Notes |
|---|------|--------|-------|
| 3.1 | `CustomerProfileRepository` | ✅ | `findByKeycloakUserId`, `JpaSpecificationExecutor` |
| 3.2 | `CustomerAddressRepository` | ✅ | `findAllByCustomer`, `resetDefaultAddressesForCustomer` |
| 3.3 | `CustomerPreferenceRepository` | ✅ | `findByCustomer` |

---

## 4. Security & Authentication

| # | Task | Status | Notes |
|---|------|--------|-------|
| 4.1 | OAuth2 Resource Server (JWT validation) | ✅ | Keycloak issuer URI |
| 4.2 | `KeycloakJwtAuthenticationConverter` (role mapping) | ✅ | `realm_access.roles` → `ROLE_*` |
| 4.3 | `SecurityAuthenticationEntryPoint` (401 handler) | ✅ | JSON response |
| 4.4 | `SecurityAccessDeniedHandler` (403 handler) | ✅ | JSON response |
| 4.5 | Internal webhook API key guard (`X-Internal-Api-Key`) | ✅ | `/internal/**` bypasses JWT |
| 4.6 | `InternalApiProperties` config binding | ✅ | `@ConfigurationProperties` |
| 4.7 | Update `SecurityConfig` for `/api/v1/customers/**` → `.authenticated()` | ⬜ | BOLA/IDOR defense |
| 4.8 | Update `SecurityConfig` for `/api/v1/admin/**` → `.hasRole("ADMIN")` | ✅ | Already configured |

---

## 5. Error Handling

| # | Task | Status | Notes |
|---|------|--------|-------|
| 5.1 | `GlobalExceptionHandler` (`@RestControllerAdvice`) | ✅ | |
| 5.2 | `MethodArgumentNotValidException` → 400 | ✅ | Validation errors |
| 5.3 | `MissingRequestHeaderException` → 401 | ✅ | Missing API key |
| 5.4 | `DataIntegrityViolationException` → 409 | ✅ | Duplicate keycloakUserId |
| 5.5 | `ResourceNotFoundException` → 404 | ✅ | Entity not found |
| 5.6 | Generic `Exception` → 500 | ✅ | Catch-all |

---

## 6. DTOs

| # | Task | Status | Notes |
|---|------|--------|-------|
| 6.1 | `ApiResponse<T>` (generic response wrapper) | ✅ | |
| 6.2 | `KeycloakUserRegisteredEvent` (webhook DTO) | ✅ | |
| 6.3 | `CustomerProfileResponse` | ✅ | `fromEntity()` mapper |
| 6.4 | `UpdateCustomerProfileRequest` | ✅ | firstName, lastName, phoneNumber |
| 6.5 | `CreateAddressRequest` | ⬜ | |
| 6.6 | `UpdateAddressRequest` | ⬜ | |
| 6.7 | `CustomerAddressResponse` | ⬜ | `fromEntity()` mapper |
| 6.8 | `UpdatePreferenceRequest` | ⬜ | |
| 6.9 | `CustomerPreferenceResponse` | ⬜ | `fromEntity()` mapper |
| 6.10 | `AdminUpdateCustomerRequest` | ⬜ | |
| 6.11 | `PagedResponse<T>` (paginated wrapper for admin) | ⬜ | |

---

## 7. Keycloak Event Listener SPI (Separate Module)

| # | Task | Status | Notes |
|---|------|--------|-------|
| 7.1 | `keycloak-event-listener` Maven module | ✅ | Keycloak 25, Java 21 |
| 7.2 | `UserRegistrationEventListenerProvider` | ✅ | `EventType.REGISTER` → HTTP webhook |
| 7.3 | `UserRegistrationEventListenerProviderFactory` | ✅ | Reads webhookUrl + apiKey from config |
| 7.4 | `META-INF/services` SPI registration | ✅ | ServiceLoader discovery |
| 7.5 | Build verification (`mvnw clean package`) | ✅ | |
| 7.6 | Deploy to Keycloak `providers/` directory | ⬜ | Manual deployment step |
| 7.7 | Enable `user-registration-sync` listener in realm | ⬜ | Keycloak admin console |

---

## 8. Service Layer

| # | Task | Status | Notes |
|---|------|--------|-------|
| 8.1 | `CustomerProfileService` interface | ✅ | |
| 8.2 | `CustomerProfileServiceImpl` (webhook flow) | ✅ | Idempotent creation |
| 8.3 | `getProfileByKeycloakUserId()` | ⬜ | For `GET /me` |
| 8.4 | `updateProfile()` | ⬜ | For `PUT /me` |
| 8.5 | `deactivateAccount()` | ⬜ | Soft deactivation |
| 8.6 | `CustomerAddressService` interface | ⬜ | |
| 8.7 | `CustomerAddressServiceImpl` | ⬜ | CRUD + default address logic |
| 8.8 | `CustomerPreferenceService` interface | ⬜ | |
| 8.9 | `CustomerPreferenceServiceImpl` | ⬜ | Get + Update |
| 8.10 | `AdminCustomerService` interface | ⬜ | |
| 8.11 | `AdminCustomerServiceImpl` | ⬜ | Search, paginate, activate/deactivate |

---

## 9. API Endpoints

### 9.1 Internal APIs

| # | Method | Endpoint | Auth | Status |
|---|--------|----------|------|--------|
| 9.1.1 | `POST` | `/internal/webhook/keycloak/user-registered` | API Key | ✅ |

### 9.2 Public APIs

| # | Method | Endpoint | Auth | Status |
|---|--------|----------|------|--------|
| 9.2.1 | `POST` | `/api/v1/public/customers` | None (`permitAll`) | ⬜ |

### 9.3 Customer Self-Service APIs (`/api/v1/customers/me/**`)

> Identity resolved from JWT `sub` claim → `keycloakUserId` (BOLA-safe)

#### Profile

| # | Method | Endpoint | Description | Status |
|---|--------|----------|-------------|--------|
| 9.3.1 | `GET` | `/api/v1/customers/me` | Get my profile | ⬜ |
| 9.3.2 | `PUT` | `/api/v1/customers/me` | Update my profile (name, phone) | ⬜ |

#### Addresses

| # | Method | Endpoint | Description | Status |
|---|--------|----------|-------------|--------|
| 9.3.3 | `POST` | `/api/v1/customers/me/addresses` | Add new address | ⬜ |
| 9.3.4 | `GET` | `/api/v1/customers/me/addresses` | List my addresses | ⬜ |
| 9.3.5 | `GET` | `/api/v1/customers/me/addresses/{addressId}` | Get single address | ⬜ |
| 9.3.6 | `PUT` | `/api/v1/customers/me/addresses/{addressId}` | Update address | ⬜ |
| 9.3.7 | `DELETE` | `/api/v1/customers/me/addresses/{addressId}` | Delete address | ⬜ |
| 9.3.8 | `PUT` | `/api/v1/customers/me/addresses/{addressId}/default` | Set default address | ⬜ |

#### Preferences

| # | Method | Endpoint | Description | Status |
|---|--------|----------|-------------|--------|
| 9.3.9 | `GET` | `/api/v1/customers/me/preferences` | Get my preferences | ⬜ |
| 9.3.10 | `PUT` | `/api/v1/customers/me/preferences` | Update my preferences | ⬜ |

#### Account

| # | Method | Endpoint | Description | Status |
|---|--------|----------|-------------|--------|
| 9.3.11 | `PATCH` | `/api/v1/customers/me/deactivate` | Soft-deactivate my account | ⬜ |

### 9.4 Admin APIs (`/api/v1/admin/customers/**`)

> Requires JWT with `ROLE_ADMIN`

| # | Method | Endpoint | Description | Status |
|---|--------|----------|-------------|--------|
| 9.4.1 | `GET` | `/api/v1/admin/customers` | List/search customers (paginated) | ⬜ |
| 9.4.2 | `GET` | `/api/v1/admin/customers/{customerId}` | Get customer by ID | ⬜ |
| 9.4.3 | `PUT` | `/api/v1/admin/customers/{customerId}` | Update customer profile | ⬜ |
| 9.4.4 | `PATCH` | `/api/v1/admin/customers/{customerId}/activate` | Activate customer account | ⬜ |
| 9.4.5 | `PATCH` | `/api/v1/admin/customers/{customerId}/deactivate` | Deactivate customer account | ⬜ |
| 9.4.6 | `GET` | `/api/v1/admin/customers/{customerId}/addresses` | Get customer addresses (read-only) | ⬜ |

---

## 10. Controllers

| # | Task | Status | Notes |
|---|------|--------|-------|
| 10.1 | `WebhookController` (`/internal/webhook/keycloak`) | ✅ | |
| 10.2 | `CustomerProfileController` refactor to `/api/v1/customers/me` | ⬜ | GET, PUT |
| 10.3 | `CustomerAddressController` (`/api/v1/customers/me/addresses`) | ⬜ | CRUD + set default |
| 10.4 | `CustomerPreferenceController` (`/api/v1/customers/me/preferences`) | ⬜ | GET, PUT |
| 10.5 | `CustomerAccountController` (`/api/v1/customers/me/deactivate`) | ⬜ | PATCH |
| 10.6 | `AdminCustomerController` (`/api/v1/admin/customers`) | ⬜ | List, get, update, activate/deactivate |

---

## 11. Testing

| # | Task | Status | Notes |
|---|------|--------|-------|
| 11.1 | Keycloak SPI build verification | ✅ | |
| 11.2 | User-service build verification | ✅ | |
| 11.3 | Webhook API key security (Postman) | ⬜ | Missing key, wrong key |
| 11.4 | Webhook idempotency (Postman) | ⬜ | 201 → 200 on retry |
| 11.5 | Webhook validation errors (Postman) | ⬜ | Bad payload → 400 |
| 11.6 | Webhook → DB insertion verification | ⬜ | SQL check |
| 11.7 | JWT auth on `/customers/me` endpoints | ⬜ | |
| 11.8 | BOLA security audit (user A ≠ user B data) | ⬜ | |
| 11.9 | Admin role enforcement on `/admin/**` | ⬜ | |
| 11.10 | Pagination & search on admin list | ⬜ | |

---

## 12. Deployment

| # | Task | Status | Notes |
|---|------|--------|-------|
| 12.1 | Deploy SPI JAR to Keycloak `providers/` | ⬜ | |
| 12.2 | Configure Keycloak SPI env vars | ⬜ | `KC_SPI_EVENTS_LISTENER_*` |
| 12.3 | Enable event listener in Keycloak realm | ⬜ | |
| 12.4 | Production `INTERNAL_API_KEY` rotation | ⬜ | |
| 12.5 | E2E: Register in Keycloak → verify DB row | ⬜ | |

---

## Progress Summary

| Category | Done | Total | Progress |
|----------|------|-------|----------|
| Infrastructure & DB | 4 | 4 | ████████████████████ 100% |
| Entities & Schema | 5 | 6 | ████████████████░░░░ 83% |
| Repositories | 3 | 3 | ████████████████████ 100% |
| Security & Auth | 7 | 8 | ██████████████████░░ 88% |
| Error Handling | 6 | 6 | ████████████████████ 100% |
| DTOs | 4 | 11 | ████████░░░░░░░░░░░░ 36% |
| Keycloak SPI | 5 | 7 | ██████████████░░░░░░ 71% |
| Service Layer | 2 | 11 | ████░░░░░░░░░░░░░░░░ 18% |
| API Endpoints | 1 | 18 | █░░░░░░░░░░░░░░░░░░░ 6% |
| Controllers | 1 | 6 | ███░░░░░░░░░░░░░░░░░ 17% |
| Testing | 2 | 10 | ████░░░░░░░░░░░░░░░░ 20% |
| Deployment | 0 | 5 | ░░░░░░░░░░░░░░░░░░░░ 0% |
| **TOTAL** | **40** | **95** | **████████░░░░░░░░░░░░ 42%** |
