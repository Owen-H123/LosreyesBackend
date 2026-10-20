# De los Reyes — Accesorios y Repuestos ERP

Sistema ERP automotriz completo para gestión de repuestos y accesorios.
Especializado en **Peugeot**, **Citroën** y **Renault**.

---

## Stack tecnológico

| Capa            | Tecnología                    |
|-----------------|-------------------------------|
| Lenguaje        | Java 17 (funciona con 21)     |
| Framework       | Spring Boot 3.2               |
| Seguridad       | Spring Security + JWT (JJWT)  |
| Base de datos   | PostgreSQL 16                 |
| ORM             | Spring Data JPA / Hibernate   |
| Migraciones     | Flyway                        |
| Mapeo de objetos| MapStruct                     |
| Caché           | Spring Cache (Simple)         |
| Documentación   | SpringDoc OpenAPI / Swagger   |
| Reportes Excel  | Apache POI                    |
| Reportes PDF    | iText 5                       |
| Contenerización | Docker + Docker Compose       |
| Tests           | JUnit 5, Mockito, Testcontainers |

---

## Arquitectura

```
Monolito Modular — Package by Feature
src/main/java/com/delosreyes/
├── config/              → SecurityConfig, OpenApiConfig, JwtProperties
├── security/            → JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl
├── shared/
│   ├── exceptions/      → GlobalExceptionHandler + excepciones personalizadas
│   ├── responses/       → ApiResponse<T>, PagedResponse<T>
│   ├── constants/       → AppConstants
│   └── utils/           → SaleNumberGenerator
└── modules/
    ├── auth/            → Login, logout, refresh, cambio de contraseña
    ├── users/           → CRUD de usuarios (solo ADMIN)
    ├── roles/           → Consulta de roles
    ├── categories/      → CRUD de categorías de productos
    ├── vehicles/        → Vehículos compatibles (Peugeot, Citroën, Renault)
    ├── products/        → CRUD completo + soft delete + compatibilidad
    ├── inventory/       → Entradas, salidas, ajustes de stock
    ├── movements/       → Historial de movimientos de inventario
    ├── sales/           → Registro de ventas + descuento automático de stock
    ├── customers/       → CRUD de clientes
    ├── dashboard/       → Métricas, totales, top productos, alertas
    ├── reports/         → Reportes JSON / Excel / PDF
    └── audit/           → Trazabilidad de todas las acciones
```

---

## Requisitos previos

- Java 17 o superior
- Docker Desktop (para PostgreSQL 16 y para los tests)
- No hace falta instalar Maven: el proyecto trae `mvnw` (Maven Wrapper)

---

## Inicio rápido

### 1. Clonar y configurar

```bash
git clone <repositorio>
cd delosreyes
```

### 2. Levantar PostgreSQL

```bash
docker compose up -d
```

### 3. Ejecutar la aplicación

```powershell
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # Linux / Mac
```

Al arrancar, Flyway aplica las migraciones pendientes de `src/main/resources/db/migration`.
La API estará disponible en: `http://localhost:8080/api/v1`

### 4. Acceder a Swagger

`http://localhost:8080/api/v1/swagger-ui.html`

---

## Variables de entorno

Todas tienen un valor por defecto pensado para desarrollo local (ver `application.properties`).
**En producción hay que definir al menos las marcadas con ⚠.**

| Variable | Para qué | Valor por defecto |
|---|---|---|
| `DB_URL` | Conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/delosreyes_erp` |
| `DB_USERNAME` | Usuario de la base | `postgres` |
| `DB_PASSWORD` ⚠ | Contraseña de la base | `postgres123` |
| `JWT_SECRET` ⚠ | Clave para firmar los tokens (mínimo 32 caracteres) | clave de desarrollo |
| `CORS_ALLOWED_ORIGINS` ⚠ | Orígenes permitidos, separados por coma | localhost y red local (puertos 3000, 5173, 4173) |
| `FRONTEND_URL` ⚠ | Dirección del frontend, usada en el enlace de recuperación | `http://localhost:3000` |
| `REFRESH_COOKIE_SECURE` ⚠ | `true` para que la cookie de sesión solo viaje por HTTPS | `false` (la red local usa HTTP) |
| `RESEND_API_KEY` | Clave de Resend para enviar correos | vacío (no envía) |
| `TEST_EMAIL_RECIPIENT` | Correo que recibe los envíos en modo prueba | vacío |
| `UPLOAD_DIR` | Carpeta de imágenes subidas | `./uploads` |
| `LOG_PATH` | Archivo de log | `./logs/delosreyes-erp.log` |
| `SERVER_PORT` | Puerto del backend | `8080` |
| `APP_PROFILE` | Perfil de Spring | `dev` |

Además, en producción hay que poner `app.email.test-mode=false` en `application.properties`:
en modo prueba, el enlace de recuperación de contraseña se escribe en el log.

---

## Credenciales por defecto

| Campo    | Valor                    |
|----------|--------------------------|
| Email    | `admin@delosreyes.com`   |
| Password | `Admin2024!`             |
| Rol      | ADMIN                    |

> **Importante: cambiar esta contraseña después del primer ingreso y antes de desplegar en producción.**

### Si se olvida la contraseña

- **Cualquier usuario:** en el login, "¿Olvidaste tu contraseña?" envía un enlace por correo.
  Mientras no haya dominio verificado en Resend (modo prueba), el enlace aparece en el log del backend.
- **Emergencia (nadie puede entrar):** se cambia directo en la base de datos. PostgreSQL genera el
  hash BCrypt con la extensión `pgcrypto`, compatible con el que usa Spring:

  ```powershell
  docker exec -it delosreyes-db psql -U postgres -d delosreyes_erp
  ```
  ```sql
  CREATE EXTENSION IF NOT EXISTS pgcrypto;
  UPDATE users SET password = crypt('NuevaClave123', gen_salt('bf', 10))
  WHERE email = 'admin@delosreyes.com';
  ```
  Debe responder `UPDATE 1`. Luego se entra con `NuevaClave123` y se cambia desde el sistema.

---

## Endpoints principales

### Autenticación

```
POST   /auth/login              → Iniciar sesión
POST   /auth/refresh            → Renovar access token
POST   /auth/logout             → Cerrar sesión
POST   /auth/change-password    → Cambiar contraseña
POST   /auth/forgot-password    → Solicitar enlace de recuperación por correo
POST   /auth/reset-password     → Cambiar la contraseña con el enlace
```

### Productos

```
GET    /products                → Listar (filtros: search, categoryId, status)
GET    /products/{id}           → Por ID
GET    /products/code/{code}    → Por código (búsqueda rápida en caja)
GET    /products/low-stock      → Con stock bajo
GET    /products/vehicle/{id}   → Compatibles con un vehículo
POST   /products                → Crear
PUT    /products/{id}           → Actualizar
DELETE /products/{id}           → Eliminar soft (solo ADMIN)
```

### Inventario

```
POST   /inventory/entry                  → Entrada de mercadería
POST   /inventory/exit                   → Salida manual
POST   /inventory/adjustment             → Ajuste de stock (solo ADMIN)
GET    /inventory/movements              → Historial de movimientos
GET    /inventory/movements/product/{id} → Por producto
GET    /inventory/movements/recent       → Últimos movimientos
```

### Ventas

```
POST   /sales                   → Registrar venta (descuenta stock automáticamente)
GET    /sales                   → Historial de ventas
GET    /sales/{id}              → Por ID
GET    /sales/number/{number}   → Por número de venta
GET    /sales/recent            → Últimas ventas
POST   /sales/{id}/cancel       → Cancelar venta (solo ADMIN)
```

### Dashboard

```
GET    /dashboard               → Datos completos del panel
GET    /dashboard/summary       → Resumen (cacheado)
GET    /dashboard/top-products  → Productos más vendidos
GET    /dashboard/low-stock     → Alertas de stock
GET    /dashboard/recent-sales  → Últimas ventas
GET    /dashboard/worker-activity → Actividad de trabajadores
```

### Reportes (solo ADMIN)

```
GET    /reports/sales/daily             → Ventas diarias (JSON)
GET    /reports/sales/daily/excel       → Descarga Excel
GET    /reports/sales/daily/pdf         → Descarga PDF
GET    /reports/sales/top-products      → Top productos
GET    /reports/inventory/low-stock     → Stock bajo (JSON)
GET    /reports/inventory/low-stock/excel → Descarga Excel
```

### Auditoría (solo ADMIN)

```
GET    /audit                    → Todos los registros
GET    /audit/user/{id}          → Por usuario
GET    /audit/module/{modulo}    → Por módulo
GET    /audit/date-range         → Por rango de fechas
```

---

## Formato de respuesta estándar

Todos los endpoints retornan:

```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": { },
  "errors": [],
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## Flujo de venta (React → API)

```
1. POST /auth/login
   → Obtener accessToken

2. GET /customers/document/12345678
   → Buscar cliente por DNI (opcional)

3. GET /products/code/PRD-001
   → Buscar producto por código (escáner o manual)

4. POST /sales
   Body: {
     "customerId": 1,
     "paymentMethod": "CASH",
     "items": [
       { "productId": 1, "quantity": 2, "discount": 0 }
     ]
   }
   → Stock se descuenta automáticamente
   → Movimiento SALE registrado
   → Auditoría registrada

5. GET /sales/{id}
   → Obtener ticket para imprimir
```

---

## Roles y permisos

| Operación              | ADMIN | WORKER |
|------------------------|:-----:|:------:|
| Gestionar usuarios     | Sí    | No     |
| Crear/editar productos | Sí    | Sí     |
| Eliminar productos     | Sí    | No     |
| Registrar ventas       | Sí    | Sí     |
| Cancelar ventas        | Sí    | No     |
| Ajustar inventario     | Sí    | No     |
| Ver reportes           | Sí    | No     |
| Ver auditoría          | Sí    | No     |
| Ver dashboard          | Sí    | Sí     |

---

## Escalabilidad futura

El sistema está diseñado para escalar:

- **Múltiples sucursales**: Agregar campo `branchId` a User, Product, Sale.
- **Notificaciones**: Módulo `notifications/` con WebSocket o email para alertas de stock bajo.
- **Facturación electrónica**: Módulo `invoicing/` que consume API SUNAT.
- **App móvil**: La API REST es consumible desde React Native sin cambios.
- **Microservicios**: Los módulos `auth`, `sales`, `inventory` están desacoplados
  y pueden extraerse como microservicios independientes sin romper el resto.

---

## Ejecutar tests

Con Docker Desktop abierto (algunos tests levantan un PostgreSQL temporal con Testcontainers):

```powershell
.\mvnw.cmd test
```

Para correr un solo test: `.\mvnw.cmd test -Dtest=SaleServiceDiscountTest`

---

*Desarrollado con Java 17 + Spring Boot 3.2 — Arquitectura Monolito Modular*