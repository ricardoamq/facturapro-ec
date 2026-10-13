import { useAuth } from '@/auth/AuthContext'
import { Badge, Card, PageHeader } from '@/components/ui'
import { dateTime } from '@/lib/format'
import { api } from '@/lib/api'
import { useEffect, useState } from 'react'
import type { AppUser } from '@/types'

export default function ProfilePage() {
  const { session } = useAuth()
  const [me, setMe] = useState<AppUser | null>(null)

  useEffect(() => {
    api.get<AppUser>('/api/users/me').then(setMe).catch(() => setMe(null))
  }, [])

  return (
    <div className="max-w-2xl">
      <PageHeader title="Mi perfil" subtitle="Datos de la cuenta activa" />
      <Card className="p-5 space-y-4">
        <div className="grid sm:grid-cols-2 gap-4 text-sm">
          <div>
            <p className="label">Nombre completo</p>
            <p className="font-medium">{session?.fullName}</p>
          </div>
          <div>
            <p className="label">Usuario</p>
            <p className="font-medium">{session?.username}</p>
          </div>
          <div>
            <p className="label">Rol</p>
            <Badge tone="info">{session?.role}</Badge>
          </div>
          <div>
            <p className="label">Correo</p>
            <p className="font-medium">{me?.email || '—'}</p>
          </div>
          <div>
            <p className="label">Telefono</p>
            <p className="font-medium">{me?.phone || '—'}</p>
          </div>
          <div>
            <p className="label">Ultimo acceso</p>
            <p className="font-medium">{dateTime(me?.lastLoginAt)}</p>
          </div>
        </div>
        <div>
          <p className="label">Permisos ({session?.permissions.length ?? 0})</p>
          <div className="flex flex-wrap gap-1.5">
            {session?.permissions.map((p) => (
              <Badge key={p}>{p}</Badge>
            ))}
          </div>
        </div>
      </Card>
    </div>
  )
}
