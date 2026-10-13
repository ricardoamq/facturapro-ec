# FacturaPro EC — Frontend

Aplicación de una sola página (SPA) del sistema de facturación e inventario: React + TypeScript + Vite + Tailwind CSS v4.

## Comandos

```powershell
npm install      # instalar dependencias
npm run dev      # servidor de desarrollo en http://localhost:5173
npm run build    # compilación de producción en dist/
npm run preview  # previsualizar la compilación
npx tsc -b       # verificación de tipos
```

## Requisitos

- El backend debe estar corriendo en `http://localhost:8085` (ver [README.md](../README.md)).
- El servidor de desarrollo de Vite proxyiza `/api` y `/actuator` hacia ese puerto.

## Estructura

| Ruta | Contenido |
| --- | --- |
| `src/lib/api.ts` | Cliente HTTP con manejo de sesión y CSRF |
| `src/auth/` | Contexto de sesión y control de acceso por permisos |
| `src/components/ui.tsx` | Componentes base (botones, tablas, modales, etc.) |
| `src/components/AppLayout.tsx` | Barra lateral, cabecera y menú por permisos |
| `src/pages/` | Pantallas: POS, ventas, productos, compras, reportes, etc. |
| `src/types/` | Contratos de datos del backend |

La documentación completa está en la [raíz del proyecto](../README.md) y en [docs/](../docs/).
