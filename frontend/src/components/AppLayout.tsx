import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  BarChart3,
  Boxes,
  ClipboardList,
  CreditCard,
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Package,
  Settings,
  ShoppingCart,
  Sun,
  Users,
  UserSquare2,
  Warehouse,
  X,
} from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { cn } from '@/lib/format'

const NAV = [
  { to: '/', label: 'Panel principal', icon: LayoutDashboard, perm: 'DASHBOARD_VIEW', end: true },
  { to: '/pos', label: 'Nueva venta', icon: ShoppingCart, perm: 'SALES_WRITE' },
  { to: '/ventas', label: 'Ventas', icon: FileText, perm: 'SALES_VIEW' },
  { to: '/cobranzas', label: 'Cuentas por cobrar', icon: CreditCard, perm: 'PAYMENTS_VIEW' },
  { to: '/productos', label: 'Productos', icon: Package, perm: 'PRODUCTS_VIEW' },
  { to: '/inventario', label: 'Inventario', icon: Warehouse, perm: 'INVENTORY_VIEW' },
  { to: '/compras', label: 'Compras', icon: ClipboardList, perm: 'PURCHASES_VIEW' },
  { to: '/clientes', label: 'Clientes', icon: Users, perm: 'CUSTOMERS_VIEW' },
  { to: '/proveedores', label: 'Proveedores', icon: Boxes, perm: 'SUPPLIERS_VIEW' },
  { to: '/reportes', label: 'Reportes', icon: BarChart3, perm: 'REPORTS_VIEW' },
  { to: '/usuarios', label: 'Usuarios', icon: UserSquare2, perm: 'USERS_VIEW' },
  { to: '/configuracion', label: 'Configuracion', icon: Settings, perm: 'SETTINGS_VIEW' },
]

export default function AppLayout() {
  const { session, logout, can } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [dark, setDark] = useState(() => localStorage.getItem('theme') === 'dark')

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
    localStorage.setItem('theme', dark ? 'dark' : 'light')
  }, [dark])

  const items = NAV.filter((n) => can(n.perm))

  return (
    <div className="min-h-screen bg-background flex">
      <aside
        className={cn(
          'no-print fixed lg:static inset-y-0 left-0 z-40 w-64 border-r border-border bg-surface',
          'flex flex-col transition-transform lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="h-14 flex items-center gap-2 px-4 border-b border-border">
          <div className="h-8 w-8 rounded-lg bg-brand text-white grid place-items-center font-bold text-sm">FP</div>
          <div>
            <p className="font-bold leading-none text-sm">FacturaPro EC</p>
            <p className="text-[10px] text-muted mt-0.5">Facturacion e inventario</p>
          </div>
          <button className="ml-auto lg:hidden text-muted" onClick={() => setOpen(false)} aria-label="Cerrar menu">
            <X size={18} />
          </button>
        </div>
        <nav className="flex-1 overflow-y-auto py-3 px-2 space-y-0.5">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive ? 'bg-brand text-white' : 'text-muted hover:bg-background hover:text-foreground',
                )
              }
            >
              <item.icon size={16} />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-border p-3 text-xs text-muted">
          Comprobantes internos · no autorizados por el SRI
        </div>
      </aside>

      {open && <div className="fixed inset-0 bg-black/40 z-30 lg:hidden" onClick={() => setOpen(false)} />}

      <div className="flex-1 flex flex-col min-w-0">
        <header className="no-print h-14 border-b border-border bg-surface flex items-center gap-3 px-4">
          <button className="lg:hidden text-muted" onClick={() => setOpen(true)} aria-label="Abrir menu">
            <Menu size={20} />
          </button>
          <div className="ml-auto flex items-center gap-2">
            <button
              className="p-2 rounded-lg hover:bg-background text-muted"
              onClick={() => setDark((d) => !d)}
              aria-label="Cambiar tema"
            >
              {dark ? <Sun size={17} /> : <Moon size={17} />}
            </button>
            <div className="text-right hidden sm:block">
              <p className="text-sm font-semibold leading-none">{session?.fullName}</p>
              <p className="text-[11px] text-muted mt-0.5">{session?.role}</p>
            </div>
            <button
              className="btn-secondary !px-2.5 !py-1.5"
              onClick={async () => {
                await logout()
                navigate('/login')
              }}
            >
              <LogOut size={15} />
              <span className="hidden sm:inline">Salir</span>
            </button>
          </div>
        </header>
        <main className="flex-1 p-4 lg:p-6 overflow-x-hidden">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
