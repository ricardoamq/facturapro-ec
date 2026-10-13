# API REST

Base: `http://localhost:8085`. Todas las rutas bajo `/api`. Respuestas de paginación: `{ content, page, size, totalElements, totalPages, first, last }`.

Autenticación: sesión por cookie `JSESSIONID`. En peticiones `GET /api/auth/csrf` se obtiene el token, que debe enviarse en la cabecera `X-XSRF-TOKEN` en todo `POST/PUT/PATCH/DELETE`.

Errores: `{ timestamp, status, code, message, path, fieldErrors? }` con códigos como `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`.

## Autenticación (`/api/auth`)

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Devuelve el token CSRF (cookie `XSRF-TOKEN`) |
| POST | `/api/auth/login` | `{ username, password }` → sesión + permisos |
| POST | `/api/auth/logout` | Cierra la sesión |
| GET | `/api/auth/me` | Sesión actual: usuario, rol, permisos, `mustChangePassword` |
| POST | `/api/auth/forgot-password` | `{ username }` → genera token de restablecimiento |
| POST | `/api/auth/reset-password` | `{ token, newPassword }` |

## Usuarios (`/api/users`) — requiere permisos `USERS_*`

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/users?search&active&page&size` | Lista paginada |
| POST | `/api/users` | Crea usuario (rol y contraseña obligatorios) |
| GET/PUT | `/api/users/{id}` | Consulta / actualiza (nombre, correo, teléfono, rol, activo) |
| POST | `/api/users/{id}/reset-password` | `{ newPassword, mustChangePassword }` |
| GET | `/api/users/me` | Perfil del usuario actual |
| POST | `/api/users/me/change-password` | `{ currentPassword, newPassword }` |

## Productos y categorías (`/api`)

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/products?search&categoryId&active&lowStock&sort&page&size` | Lista paginada |
| POST | `/api/products` | Crea (SKU, nombre, precios, unidad, stock inicial, mínimo, categoría fiscal) |
| PUT | `/api/products/{id}` | Actualiza (no cambia el stock: usar inventario) |
| POST | `/api/products/{id}/deactivate` | Desactiva |
| DELETE | `/api/products/{id}` | Elimina (falla si tiene movimientos) |
| GET | `/api/categories?active` | Lista de categorías |
| POST/PUT | `/api/categories[/{id}]` | Crea / actualiza |

## Clientes (`/api/customers`) y Proveedores (`/api/suppliers`)

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/?search&active&page&size` | Lista paginada |
| POST | `/` | Crea (`idType`: RUC/CEDULA/PASAPORTE/IDENTIFICACION_EXTRANJERA) |
| PUT | `/{id}` | Actualiza (incluye `active`) |
| GET | `/{id}` | Consulta |
| POST | `/api/customers/quick` | Alta rápida desde el POS |

## Ventas (`/api/sales`)

| Método | Ruta | Permisos | Descripción |
| --- | --- | --- | --- |
| GET | `/api/sales?search&status&paymentStatus&customerId&userId&from&to&page&size` | `SALES_VIEW` | Lista paginada |
| GET | `/api/sales/{id}` | `SALES_VIEW` | Detalle con ítems y pagos |
| GET | `/api/sales/by-number/{number}` | `SALES_VIEW` | Búsqueda por número |
| POST | `/api/sales` | `SALES_WRITE` | Crea (borrador o emisión con `issue`); cabecera opcional `Idempotency-Key` |
| POST | `/api/sales/{id}/issue` | `SALES_WRITE` | Emite un borrador (asigna número y descuenta stock) |
| POST | `/api/sales/{id}/void` | `SALES_VOID` | Anula con `{ reason }` (devuelve stock) |
| POST | `/api/sales/{id}/payments` | `PAYMENTS_WRITE` | `{ amount, method, reference?, notes? }` + `Idempotency-Key` |
| GET | `/api/sales/meta/methods` | — | Métodos de pago (CASH, CARD, TRANSFER, CHECK, OTHER) |
| GET | `/api/sales/permissions` | — | `{ canVoid, canPay }` del usuario actual |

Cuerpo de creación:

```json
{
  "customerId": 1,
  "saleDate": "2026-10-12",
  "notes": "string",
  "issue": true,
  "items": [{ "productId": 3, "quantity": 2, "unitPrice": 15.0, "discountPercent": 0 }],
  "initialPaymentAmount": 34.5,
  "initialPaymentMethod": "CASH"
}
```

## Pagos (`/api/payments`)

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/api/payments/{id}/void` | Anula un pago con `{ reason }` |

## Compras (`/api/purchases`)

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/purchases?status&supplierId&from&to&page&size` | Lista paginada |
| GET | `/api/purchases/{id}` | Detalle |
| POST | `/api/purchases` | Crea borrador: `{ supplierId, purchaseDate, notes, items[{productId, quantity, unitCost, discountPercent}] }` |
| PUT | `/api/purchases/{id}` | Actualiza borrador |
| POST | `/api/purchases/{id}/confirm` | Confirma: calcula totales e ingresa stock |
| POST | `/api/purchases/{id}/cancel` | Cancela con `{ reason }` |
| POST | `/api/purchases/{id}/returns` | Devolución: `{ reason, items[{productId, quantity}] }` |

## Inventario (`/api/inventory`)

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/inventory/movements?productId&type&from&to&page&size` | Movimientos (PURCHASE, SALE, ADJUSTMENT, *_RETURN) |
| GET | `/api/inventory/low-stock` | Productos bajo mínimo |
| GET | `/api/inventory/summary` | Activos, bajo mínimo, valor de stock, stock negativo permitido |
| POST | `/api/inventory/adjustments` | `{ productId, quantity, reason }` (permiso `INVENTORY_ADJUST`) |

## Reportes (`/api/reports`)

| Método | Ruta | Parámetros |
| --- | --- | --- |
| GET | `/sales` | `from`, `to` → totales + desglose diario |
| GET | `/sales/by-product` | `from`, `to` |
| GET | `/sales/by-customer` | `from`, `to` |
| GET | `/sales/by-user` | `from`, `to` |
| GET | `/sales/export` | `from`, `to` → CSV |
| GET | `/taxes` | `from`, `to` → base e impuesto por tarifa |
| GET | `/purchases` | `from`, `to` |
| GET | `/receivables` | — → ventas con saldo |
| GET | `/inventory` | — → existencias valorizadas |

## Panel, ajustes y auditoría

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/dashboard/summary` | Indicadores de hoy/semana/mes, cartera, stock, top productos, evolución y actividad |
| GET | `/api/settings` | Todos los parámetros (requiere `SETTINGS_VIEW`) |
| PUT | `/api/settings` | `{ values: { clave: valor } }` (requiere `SETTINGS_WRITE`) |
| GET | `/api/settings/tax-categories` | Categorías fiscales con sus tarifas |
| GET | `/api/audit?action&entityType&username&from&to&page&size` | Eventos de auditoría (requiere `AUDIT_VIEW`) |

## Documentos

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/invoices/{number}/pdf` | PDF del comprobante interno |
| GET | `/api/invoices/{number}/html` | Vista HTML imprimible |

## Salud y OpenAPI

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/actuator/health` | Estado del servicio |
| GET | `/v3/api-docs` | Especificación OpenAPI (JSON) |
| GET | `/swagger-ui.html` | Swagger UI |
