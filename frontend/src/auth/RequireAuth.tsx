import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'

export function RequireAuth() {
  const { session, loading } = useAuth()
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-muted text-sm">Cargando sesion...</div>
      </div>
    )
  }
  if (!session) return <Navigate to="/login" replace />
  return <Outlet />
}

export function RequirePermission({ permission }: { permission: string }) {
  const { can, session } = useAuth()
  if (!can(permission)) {
    return (
      <div className="card p-8 max-w-lg mx-auto mt-16 text-center">
        <h2 className="text-lg font-semibold mb-2">Acceso restringido</h2>
        <p className="text-sm text-muted">
          Su rol <b>{session?.role}</b> no tiene el permiso <code>{permission}</code> para ver esta seccion.
        </p>
      </div>
    )
  }
  return <Outlet />
}
