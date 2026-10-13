# Funcionamiento de FacturaPro EC

Este documento describe cómo opera el sistema: arquitectura, flujos principales y reglas de negocio aplicadas en el código.

## 1. Arquitectura general

```
Navegador (React + Vite, puerto 5173)
        │  fetch con cookies de sesión + cabecera X-XSRF-TOKEN
        ▼
Proxy de Vite (/api → localhost:8085)
        ▼
API REST Spring Boot (puerto 8085)
        │  Spring Security (sesión + CSRF) → @PreAuthorize (permisos)
        ▼
Servicios (transacciones, reglas de negocio)
        ▼
JPA / Hibernate  ──►  MySQL 8.4 (Flyway controla el esquema)
```

- El frontend **no** guarda tokens en `localStorage`: la sesión vive en la cookie `JSESSIONID` (HttpOnly) y el token CSRF en la cookie `XSRF-TOKEN`, que el cliente copia a la cabecera `X-XSRF-TOKEN` en cada petición que no sea `GET`.
- El backend es la única fuente de verdad: calcula precios, impuestos, totales, stock y numeración. La interfaz solo hace una vista previa.

## 2. Seguridad y autorización

| Mecanismo | Detalle |
| --- | --- |
| Sesión | Cookie `JSESSIONID`, timeout 30 min |
| CSRF | Cookie `XSRF-TOKEN` + cabecera `X-XSRF-TOKEN` (manejador `SpaCsrfTokenRequestHandler` sin XOR) |
| Contraseñas | BCrypt factor 12 (`SecurityConfig.ENCODER`) |
| Intentos fallidos | Máximo 5 intentos, bloqueo de 15 minutos |
| Recuperación | Token con TTL de 60 minutos (en desarrollo se devuelve/loguea, no se envía correo) |
| Cambio obligatorio | Si `mustChangePassword=true`, el frontend redirige a `/cambiar-password` |
| Autorización | Cada endpoint exige permiso con `@PreAuthorize("hasAuthority('...')")` |

### Roles y permisos

| Rol | Permisos principales |
| --- | --- |
| ADMINISTRADOR | Todos |
| GERENTE | Ventas, compras, reportes, productos, clientes, proveedores, inventario, pagos |
| VENDEDOR | Dashboard, ventas (crear/emisión), clientes, productos (consulta), pagos |
| BODEGA | Productos, inventario, compras, proveedores |

Sin sesión: **401**. Con sesión pero sin permiso: **403**.

## 3. Modelo de datos (resumen)

Tablas principales: `users`, `roles` (catálogo), `audit_log`, `settings`, `categories`, `products`, `customers`, `suppliers`, `sales`, `sale_items`, `payments`, `purchases`, `purchase_items`, `purchase_returns`, `stock_movements`.

- Dinero: `DECIMAL(19,4)`; cantidades: `DECIMAL(14,3)`; tasas: `DECIMAL(7,4)`.
- `stock_movements` es el **único** registro que explica cada cambio de existencia (tipo, cantidad, saldo resultante, referencia, usuario y motivo).
- `settings` guarda parámetros editables (empresa, numeración, tarifas IVA, inventario, sesión) con valores por defecto cargados por `V2__settings_defaults.sql`.
- El esquema lo define `V1__schema.sql` (no Hibernate): `ddl-auto: validate` verifica que coincidan.

## 4. Flujo de ventas (POS)

1. El usuario busca productos (`GET /api/products?search=`) y arma el carrito.
2. Al emitir, el frontend envía `POST /api/sales` con `issue: true`, los ítems y opcionalmente un pago inicial. Usa cabecera `Idempotency-Key` (UUID) para que un reintento **no** duplique la venta.
3. El backend (`SaleService` + `PricingService`):
   - valida existencias y permisos;
   - calcula línea por línea: bruto → descuento → base → IVA (tasa de la categoría fiscal del producto) → **redondeo HALF_UP a 2 decimales por línea**;
   - el total del documento = **suma de las líneas redondeadas** (evita descuadres de centavos);
   - descuenta stock dentro de la misma transacción con bloqueo pesimista de filas del producto;
   - registra el pago inicial si corresponde;
   - crea la auditoría correspondiente.
4. Estados de la venta: `DRAFT` → `ISSUED` (anula stock) → `VOIDED` (devuelve stock, requiere motivo y permiso `SALES_VOID`).
5. Estados de pago: `PENDING` → `PARTIAL` → `PAID`, recalculados por el backend en cada pago.

### Numeración de comprobantes

`DocumentNumberService` asigna números con formato `FAC-{año}-{secuencia}` (8 dígitos) dentro de la transacción de emisión, con bloqueo pesimista para evitar duplicados. Los borradores no tienen número.

### PDF y HTML

- `GET /api/invoices/{number}/pdf` → PDF (openhtmltopdf, fuentes DejaVu) con leyenda *"No autorizado electrónicamente por el SRI"*.
- `GET /api/invoices/{number}/html` → vista imprimible.

## 5. Flujo de compras

1. `POST /api/purchases` crea un **borrador** (no toca stock).
2. `POST /api/purchases/{id}/confirm` → en una transacción: calcula totales con IVA, **incrementa el stock** y registra el movimiento `PURCHASE`.
3. `POST /api/purchases/{id}/cancel` → cancela con motivo (solo borradores sin confirmar).
4. `POST /api/purchases/{id}/returns` → devolución parcial con motivo: reduce stock y crea el comprobante de devolución (`PURCHASE_RETURN`).

## 6. Inventario

- Ningún endpoint modifica `products.stock` directamente: todo pasa por `StockService` (bloqueo pesimista + movimiento + auditoría).
- Ajustes manuales: `POST /api/inventory/adjustments` con producto, cantidad (positiva suma, negativa resta) y motivo obligatorio (permiso `INVENTORY_ADJUST`).
- Alertas: producto entra en "bajo mínimo" cuando `stock <= min_stock`.
- Stock negativo: configurable (`inventory.allowNegativeStock`); por defecto bloqueado.

## 7. Pagos y cuentas por cobrar

- `POST /api/sales/{id}/payments` registra un pago (monto > 0, método, referencia opcional) con `Idempotency-Key`.
- `POST /api/payments/{id}/void` anula un pago y recalcula el saldo.
- `GET /api/reports/receivables` devuelve las ventas emitidas con saldo > 0 y días de antigüedad.

## 8. Reportes

Todos aceptan `from` y `to` (ISO `yyyy-MM-dd`):

| Endpoint | Contenido |
| --- | --- |
| `/api/reports/sales` | Totales del rango + desglose por día |
| `/api/reports/sales/by-product` | Cantidad, neto, IVA, costo y utilidad por producto |
| `/api/reports/sales/by-customer` / `by-user` | Totales y utilidad por cliente/vendedor |
| `/api/reports/taxes` | Base e impuesto por tarifa |
| `/api/reports/purchases` | Compras del rango |
| `/api/reports/inventory` | Existencias valorizadas |
| `/api/reports/receivables` | Cartera pendiente |
| `/api/reports/sales/export` | CSV de ventas del rango |

## 9. Auditoría

`AuditService` registra eventos como `LOGIN_SUCCESS`, `LOGIN_FAILED`, `SALE_ISSUED`, `PAYMENT_REGISTERED`, `STOCK_ADJUSTED`, etc., con usuario, entidad y detalles. La auditoría es de solo escritura por los servicios; la consulta requiere `AUDIT_VIEW`.

## 10. Frontend

- **Sesión**: `AuthProvider` consulta `/api/auth/me` al iniciar; si responde 401 (evento `auth:unauthorized`) se cierra la sesión y redirige a `/login`.
- **Rutas**: `RequireAuth` exige sesión; `RequirePermission` muestra pantalla de "Acceso restringido" si falta el permiso (además de la validación del backend).
- **Cliente HTTP** (`src/lib/api.ts`): asegura el token CSRF antes de cada mutación, traduce errores del backend a mensajes de usuario y dispara el cierre de sesión en 401.
- **Tema claro/oscuro** persistido en `localStorage`.

## 11. Perfiles de Spring

| Perfil | Uso |
| --- | --- |
| (ninguno) | Entorno normal; requiere `APP_ADMIN_INITIAL_PASSWORD` |
| `demo` | Desarrollo: carga datos ficticios (productos, clientes, proveedor, vendedor, ventas de 14 días) y define credenciales de demostración |

Nunca usar `demo` en producción.
