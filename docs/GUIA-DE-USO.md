# Guía de uso — FacturaPro EC

Guía práctica para arrancar el sistema, iniciar sesión y usar cada módulo. Para detalles técnicos ver [README](../README.md), [FUNCIONAMIENTO](FUNCIONAMIENTO.md) y [API](API.md).

---

## 1. Requisitos

- Java 21
- Node.js 18+ (probado con Node 24)
- MySQL 8 en el puerto `3306` con base de datos `facturapro` (ver [README](../README.md) para crearla)
- Los puertos `8085` (backend) y `5173` (frontend) libres

---

## 2. Cómo iniciar todo

Abre **tres terminales** en la carpeta del proyecto (`Facturacion sistema`).

### Terminal 1 — Base de datos (si MySQL no está corriendo)

```powershell
& "C:\Users\Ricar\tools\mysql-8.4.9-winx64\bin\mysqld.exe" --datadir="C:\Users\Ricar\tools\mysql-data" --console
```

Deja esta terminal abierta.

### Terminal 2 — Backend

```powershell
Set-Location backend
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Alternativa con el jar ya compilado:

```powershell
Set-Location backend
java -jar target\facturapro-backend-1.0.0.jar --spring.profiles.active=demo
```

Verifica que esté listo: abre <http://localhost:8085/actuator/health> y debe decir `{"status":"UP"}`.

### Terminal 3 — Frontend

```powershell
Set-Location frontend
npm run dev
```

Abre **<http://localhost:5173>** en el navegador. El frontend conecta con el backend automáticamente (proxy configurado).

> **Detener:** `Ctrl+C` en cada terminal. **Reiniciar el backend** es seguro en cualquier momento (no hay datos en memoria; todo vive en MySQL).

---

## 3. Iniciar sesión

1. Entra a <http://localhost:5173>. Verás la pantalla de login de **FacturaPro EC**.
2. Escribe usuario y contraseña según el perfil que necesites:

| Perfil | Usuario | Contraseña | Puede hacer |
|---|---|---|---|
| Administrador | `admin` | `AdminDemo12345!` | Todo: productos, inventario, compras, ventas, reportes, usuarios, configuración, auditoría |
| Vendedor | `vendedor` | `VendedorDemo123!` | Punto de venta, ventas, cobros, clientes, ver productos y reportes |

3. Presiona **Ingresar**. Si los datos son correctos llegas al **Panel (Dashboard)**.

### Si olvidaste la contraseña

- En la pantalla de login pulsa **¿Olvidaste tu contraseña?** — genera una recuperación por token (perfil demo; en producción requiere correo configurado).
- El administrador también puede reiniciar contraseñas desde **Usuarios**.

### Tu perfil

- Arriba a la derecha (menú del usuario) → **Mi perfil**: cambia tu contraseña o edita tu nombre. **Cerrar sesión** ahí mismo.

### Si ves un error 403 en el login

El navegador expiró la sesión: recarga con `Ctrl+Shift+R` y vuelve a entrar.

---

## 4. Recorrido por los módulos (barra lateral izquierda)

### 4.1 Panel (Dashboard)

Resumen del negocio: ventas del día, totales del mes, productos con stock bajo, últimas ventas y gráficos. Todo se calcula en el backend desde datos reales.

### 4.2 Productos (si eres admin)

1. **Productos → Nuevo producto**.
2. Completa: código/SKU, nombre, categoría, precio de venta, costo, stock inicial y stock mínimo. El IVA aplica por producto (0% o 15% según corresponda).
3. Guarda. El producto aparece en la tabla y ya está disponible para vender.
4. Desde la lista puedes **editar**, **ver ficha** (con movimientos de inventario) y **activar/desactivar**.

### 4.3 Clientes y Proveedores

- **Clientes → Nuevo cliente**: nombre o razón social, cédula/RUC, dirección, correo y teléfono. Son opcionales en una venta (si no hay, se factura a *Consumidor final*).
- **Proveedores**: igual que clientes; se usan en las compras.

### 4.4 Punto de venta (POS)

1. Abre **Punto de venta**.
2. Busca productos por nombre o código y agrégalos; ajusta cantidad o descuento si hace falta.
3. Selecciona el cliente (o déjalo en consumidor final).
4. Elige forma de pago: **Contado** (se paga ahora) o **Crédito** (queda saldo por cobrar).
5. **Emitir venta**: el backend calcula totales, descuenta stock, numera el documento (`FAC-2026-00000001`, …) y puedes descargar el **PDF**.

### 4.5 Ventas

- Lista de todas las ventas con filtros por fecha, estado, cliente y búsqueda.
- **Detalle de venta**: renglones, totales, pagos registrados, saldo pendiente y **descarga del comprobante PDF**.
- Aquí también puedes **anular** una venta (devuelve el stock) si tienes permiso.

### 4.6 Inventario (si eres admin)

- **Resumen**: existencias, valorización y productos bajo mínimo.
- **Movimientos**: historial completo (ventas, compras, ajustes, devoluciones) con filtros por producto, tipo y fecha.

### 4.7 Compras (si eres admin)

1. **Compras → Nueva compra**: elige proveedor y agrega renglones (producto, cantidad, costo).
2. Al guardar, el inventario sube con el ingreso.
3. Puedes registrar **devoluciones de compra** (el inventario baja).
4. Las compras se listan y filtran igual que las ventas.

### 4.8 Por cobrar (Cuentas por cobrar)

Ventas a crédito con saldo pendiente. Registra **cobros** (totales o parciales): el saldo se recalcula automáticamente y la venta pasa a *pagada* cuando se liquida.

### 4.9 Reportes

- **Ventas**: rango de fechas → totales, IVA, descuentos, pagos, costo y utilidad neta; desglose por día y por producto. **Exportar** a CSV disponible.
- **Por cobrar**: saldos pendientes de clientes.
- Todos los números salen del backend (mismo cálculo que el resto del sistema).

### 4.10 Usuarios (si eres admin)

Alta, edición y desactivación de usuarios con roles y permisos por módulo. El usuario inicial es `admin`.

### 4.11 Configuración (si eres admin)

Datos del negocio (nombre legal, RUC, dirección, teléfonos), símbolo de moneda, parámetros fiscales y numeración. Estos datos aparecen en los PDFs.

### 4.12 Auditoría (si eres admin)

Bitácora de quién hizo qué y cuándo (logins, creaciones, ventas, anulaciones, cambios de configuración) con filtros por fecha, usuario y acción.

---

## 5. Flujo completo recomendado (primera prueba)

1. Entra como **admin**.
2. **Configuración**: coloca nombre, RUC y dirección de tu negocio.
3. **Productos → Nuevo producto**: crea 2 o 3 productos con stock.
4. **Clientes → Nuevo cliente**: crea un cliente.
5. **Punto de venta**: haz una venta *Contado* — descarga el PDF.
6. **Punto de venta**: haz una venta *Crédito*.
7. **Por cobrar**: registra un cobro parcial y luego el total.
8. **Ventas**: revisa el detalle y verifica el saldo en 0.
9. **Inventario → Movimientos**: confirma que las ventas descontaron stock.
10. **Reportes → Ventas**: elige un rango y exporta el CSV.

---

## 6. Problemas frecuentes

| Síntoma | Solución |
|---|---|
| Página en blanco o errores | Asegúrate de que el backend está en 8085 y el frontend en 5173; recarga con `Ctrl+Shift+R` |
| Login devuelve error | Verifica credenciales de la tabla §3; si persiste, revisa que el backend está `UP` en `/actuator/health` |
| Error 403 en operaciones | Sesión expirada: cierra sesión y vuelve a entrar |
| El backend no arranca | Puerto 8085 ocupado o MySQL apagado (§2) |
| Ventana de base de datos | El perfil `demo` crea datos de demostración la primera vez (productos, clientes, usuarios) |

---

## 7. Pruebas automatizadas

```powershell
# Backend (10 pruebas: humo de API + consultas de reportes)
Set-Location backend
mvn test

# E2E del flujo completo por el proxy del frontend (27 checks)
Set-Location ..
node scripts\e2e-proxy-test.mjs
```

Estado actual: **10/10** y **27/27** en verde.
