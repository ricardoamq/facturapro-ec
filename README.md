# FacturaPro EC

Sistema de facturación e inventario para negocios ecuatorianos: ventas en punto de venta, compras, cuentas por cobrar, control de existencias, reportes y auditoría. Compuesto por un backend en Spring Boot y un frontend en React con TypeScript.

> Los comprobantes que genera este sistema son **internos (no autorizados electrónicamente por el SRI)**. La facturación electrónica no está implementada; el sistema deja explícito ese estado en cada documento.

**¿Primera vez usando el sistema?** Lee la [Guía de uso](docs/GUIA-DE-USO.md): cómo arrancar todo, iniciar sesión con cada rol y recorrer cada módulo paso a paso.

> **Estado:** desarrollo local activo. **No hay demo pública**: el sistema se ejecuta en local siguiendo los pasos de abajo.

<details>
<summary><b>🌐 English summary</b></summary>

**FacturaPro EC** is an invoicing and inventory system for Ecuadorian businesses: point of sale, purchases, receivables, stock control, reports and audit trail. It pairs a **Spring Boot 3 (Java 21)** API with a **React 19 + TypeScript** frontend on **MySQL 8.4**.

Highlights: backend-enforced roles (`@PreAuthorize`), CSRF-protected sessions, a POS where the **backend calculates the official amounts**, sequential yearly invoicing with PDF output, transactional inventory with traceable movements, reports with CSV export, dashboard KPIs and an immutable audit log.

> Invoices are **internal documents** — they are *not* electronically authorized by Ecuador's SRI. E-invoicing is not implemented.

**Run it locally:** create the MySQL database → `mvn -DskipTests package` and run the jar with `--spring.profiles.active=demo` (port `8085`) → `npm install && npm run dev` in `frontend/` (port `5173`). The step-by-step guide is [docs/GUIA-DE-USO.md](docs/GUIA-DE-USO.md); API reference in [docs/API.md](docs/API.md).

**No public demo.** Known limitations: no SRI XML/access keys, password recovery works in dev mode only, no Docker/CI deployment yet.

</details>

---

## Capturas

<p align="center">
  <img src="docs/screenshots/dashboard.png" alt="Panel principal de FacturaPro EC: ventas del día, cartera por cobrar, gráfico de 14 días y actividad reciente" width="48%" />
  <img src="docs/screenshots/punto-de-venta.png" alt="Punto de venta de FacturaPro EC: búsqueda de productos, totales con IVA y emisión de venta" width="48%" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/0ea42b85-afe9-4e4c-8eba-3d46a01fda8e" alt="Módulo de inventario de FacturaPro EC con existencias, costos y alertas de stock mínimo" width="48%" />
  <img src="https://github.com/user-attachments/assets/7a6fef4b-0721-4ed3-a558-d1e9fda294b9" alt="Listado de ventas de FacturaPro EC con cliente, estado de pago y totales" width="48%" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/74db05a9-1fc2-4880-b765-60e6a655900d" alt="Catálogo de productos de FacturaPro EC con SKU, precios, margen y stock" width="48%" />
  <img src="https://github.com/user-attachments/assets/4a333355-19bc-48ea-a64d-5d29c5bad1bb" alt="Cuentas por cobrar de FacturaPro EC con saldos, vencimientos y exportación a CSV" width="48%" />
</p>

---

## Características

- **Autenticación por sesión** (cookie `JSESSIONID`) con protección **CSRF** (`XSRF-TOKEN` / `X-XSRF-TOKEN`), bloqueo tras intentos fallidos y cambio obligatorio de contraseña temporal.
- **Roles y permisos reales**: `ADMINISTRADOR`, `GERENTE`, `VENDEDOR`, `BODEGA` aplicados en el backend con `@PreAuthorize` (401 sin sesión, 403 sin permiso).
- **Productos y categorías**: SKU, código de barras, precios de compra/venta, margen, stock mínimo y categoría fiscal configurable.
- **Punto de venta (POS)**: carrito con cantidades, precios y descuentos por línea; cliente opcional; emisión o borrador; pago inicial opcional; el **backend calcula siempre los importes oficiales**.
- **Comprobantes internos**: numeración secuencial por año (`FAC-2026-00000001`) con bloqueo pesimista; descarga **PDF** e impresión **HTML**; anulación con motivo obligatorio.
- **Pagos y cuentas por cobrar**: pagos parciales, anulación de pagos, saldos recalculados por el backend y reporte de cartera vencida.
- **Compras**: borrador → confirmación (ingreso de stock) o cancelación; devoluciones parciales con motivo.
- **Inventario**: entrada y salida solo a través de servicios transaccionales con bloqueo de filas; movimientos trazados (compra, venta, ajuste, devolución) y ajustes con motivo obligatorio.
- **Reportes**: resumen de ventas por día, por producto, por cliente y por usuario; impuestos (IVA) por tarifa; compras; inventario valorizado; exportación CSV.
- **Panel principal**: ventas de hoy/semana/mes, cartera por cobrar, productos con stock bajo, productos más vendidos, evolución de 14/30 días y actividad reciente.
- **Auditoría**: registro inmutable de acciones (quién, qué, cuándo, qué entidad) consultable por usuarios con permiso.
- **Configuración**: datos de la empresa, formato de numeración, tarifas de IVA, parámetros de inventario y sesión, editables desde la UI.

## Stack tecnológico

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Flyway, Hibernate (`ddl-auto: validate`) |
| Base de datos | MySQL 8.4 (InnoDB, `utf8mb4`) |
| API | REST + JSON, OpenAPI/Swagger en `/swagger-ui.html` |
| Documentos | openhtmltopdf (PDF) con fuentes DejaVu embebidas |
| Frontend | React 19, TypeScript, Vite, React Router, Tailwind CSS v4, Recharts, Sonner |
| Pruebas | JUnit 5 / Spring Boot Test (backend), scripts de humo y E2E (Node/PowerShell) |

## Estructura del proyecto

```
Facturacion sistema/
├── backend/                  # API Spring Boot (puerto 8085)
│   ├── src/main/java/com/facturapro/
│   │   ├── config/           # Seguridad, CORS, CSRF, datos iniciales y demo
│   │   ├── security/         # Sesión, usuario actual, permisos
│   │   ├── auth/             # Login, logout, recuperación de contraseña
│   │   ├── users/            # Usuarios y roles
│   │   ├── products/         # Productos y categorías
│   │   ├── customers/ suppliers/   # Clientes y proveedores
│   │   ├── inventory/        # Stock, movimientos y ajustes
│   │   ├── purchases/        # Compras y devoluciones
│   │   ├── sales/            # Ventas, cálculo de precios y emisión
│   │   ├── payments/         # Pagos y anulaciones
│   │   ├── invoices/         # Numeración, PDF y HTML
│   │   ├── reports/          # Consultas y exportación de reportes
│   │   ├── dashboard/        # Indicadores del panel
│   │   └── audit/            # Auditoría
│   └── src/main/resources/
│       ├── db/migration/     # V1__schema.sql, V2__settings_defaults.sql
│       └── application*.yml
├── frontend/                 # SPA React + Vite (puerto 5173)
│   └── src/
│       ├── auth/             # Contexto de sesión y control de rutas
│       ├── components/       # Diseño base (ui.tsx) y distribución (AppLayout)
│       ├── lib/              # Cliente HTTP con CSRF, formato de moneda/fechas
│       ├── pages/            # Pantallas (POS, ventas, productos, reportes, ...)
│       └── types/            # Contratos de datos del backend
├── docs/                     # Documentación funcional y de API
├── scripts/                  # Pruebas automatizadas (humo y E2E)
└── README.md
```

## Requisitos

- Java 21 (JDK) y Maven 3.9+
- Node.js 20+ y npm
- MySQL 8.4 (local o remoto)
- Windows, macOS o Linux (los comandos de este README están en PowerShell)

## Puesta en marcha

### 1. Base de datos

```powershell
# Crear la base de datos y el usuario (ejecutar como administrador de MySQL)
mysql -u root -e "CREATE DATABASE facturapro CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -e "CREATE USER 'facturapro'@'localhost' IDENTIFIED BY 'facturapro_dev_2026';"
mysql -u root -e "GRANT ALL PRIVILEGES ON facturapro.* TO 'facturapro'@'localhost';"
```

Las migraciones de Flyway se ejecutan automáticamente al arrancar el backend.

### 2. Backend

```powershell
cd backend
mvn -DskipTests package
# Opción A: con datos de demostración (recomendada para probar)
java -jar target/facturapro-backend-1.0.0.jar --spring.profiles.active=demo
```

Verificar: `http://localhost:8085/actuator/health` debe devolver `{"status":"UP"}`.

### 3. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173`. El servidor de desarrollo proxyiza `/api` y `/actuator` hacia `http://localhost:8085`.

## Credenciales de desarrollo

| Usuario | Contraseña | Perfil | Rol |
| --- | --- | --- | --- |
| `admin` | `AdminDemo12345!` | `demo` | ADMINISTRADOR |
| `vendedor` | `VendedorDemo123!` | `demo` | VENDEDOR |

- Sin el perfil `demo`, el usuario `admin` se crea con la contraseña indicada en la variable `APP_ADMIN_INITIAL_PASSWORD` (si no se define, el backend imprime una advertencia en el log).
- **Estas credenciales son solo para desarrollo local.** Nunca usar el perfil `demo` ni estas contraseñas en producción.

## Variables de entorno relevantes (backend)

| Variable | Valor por defecto | Descripción |
| --- | --- | --- |
| `SERVER_PORT` | `8085` | Puerto del backend |
| `DB_URL` | `jdbc:mysql://localhost:3306/facturapro...` | Cadena de conexión |
| `DB_USER` / `DB_PASSWORD` | `facturapro` / `facturapro_dev_2026` | Credenciales de la base |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes permitidos |
| `APP_ADMIN_INITIAL_USERNAME` | `admin` | Usuario administrador inicial |
| `APP_ADMIN_INITIAL_PASSWORD` | (vacío) | Contraseña del administrador inicial |
| `APP_DEMO_DATA_ENABLED` | `false` | Carga datos ficticios |

## Pruebas

```powershell
# Pruebas unitarias e integración del backend (requiere BD facturapro_test)
cd backend
mvn test

# Humo manual contra el backend corriendo
powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1 -Password "AdminDemo12345!"

# Prueba E2E del flujo completo a través del proxy del frontend (5173)
node scripts\e2e-proxy-test.mjs
```

## Documentación

- [docs/GUIA-DE-USO.md](docs/GUIA-DE-USO.md) — **guía práctica**: arranque, inicio de sesión y uso de cada módulo.
- [docs/FUNCIONAMIENTO.md](docs/FUNCIONAMIENTO.md) — arquitectura, flujos y reglas de negocio.
- [docs/API.md](docs/API.md) — referencia de los endpoints REST.
- Swagger UI: `http://localhost:8085/swagger-ui.html`

## Limitaciones conocidas

- No genera comprobantes electrónicos XML ni clave de acceso del SRI (los PDF dicen explícitamente "no autorizados electrónicamente").
- El envío de correos de recuperación de contraseña está en modo desarrollo (el token se devuelve/loguea en lugar de enviarse).
- No hay despliegue configurado (Docker/CI); la base de datos debe crearse manualmente.
- Las pruebas automatizadas del backend están en proceso de ampliación.

---

Proyecto en desarrollo activo. Los commits siguen el formato convencional en español (`feat:`, `fix:`, `docs:`, `test:`).
