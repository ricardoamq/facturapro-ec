import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { Badge, Card, EmptyState, Input, PageHeader, Pagination, Spinner } from '@/components/ui'
import { date, dateTime, messageOf } from '@/lib/format'
import type { Page as PageT } from '@/types'

interface AuditRow {
  id: number
  username: string | null
  action: string
  entityType: string | null
  entityId: string | null
  details: string | null
  createdAt: string
}

export default function AuditPage() {
  const [data, setData] = useState<PageT<AuditRow> | null>(null)
  const [action, setAction] = useState('')
  const [username, setUsername] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    const params = new URLSearchParams({ page: String(page), size: '30' })
    if (action) params.set('action', action)
    if (username) params.set('username', username)
    try {
      setData(await api.get<PageT<AuditRow>>(`/api/audit?${params}`))
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setLoading(false)
    }
  }, [action, username, page])

  useEffect(() => {
    const t = setTimeout(() => void load(), 250)
    return () => clearTimeout(t)
  }, [load])

  return (
    <div>
      <PageHeader title="Auditoria" subtitle="Registro inmutable de acciones del sistema" />

      <Card>
        <div className="p-3 flex flex-wrap gap-2 border-b border-border">
          <Input
            placeholder="Accion (ej. LOGIN_SUCCESS, SALE_ISSUED)..."
            value={action}
            onChange={(e) => {
              setAction(e.target.value)
              setPage(0)
            }}
            className="max-w-xs"
          />
          <Input
            placeholder="Usuario..."
            value={username}
            onChange={(e) => {
              setUsername(e.target.value)
              setPage(0)
            }}
            className="max-w-40"
          />
        </div>

        {loading && !data ? (
          <Spinner />
        ) : !data || data.content.length === 0 ? (
          <EmptyState title="Sin eventos de auditoria" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr>
                  <th className="th">Fecha</th>
                  <th className="th">Usuario</th>
                  <th className="th">Accion</th>
                  <th className="th">Entidad</th>
                  <th className="th">Detalles</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((a) => (
                  <tr key={a.id} className="hover:bg-background/60">
                    <td className="td whitespace-nowrap text-muted text-xs">{dateTime(a.createdAt)}</td>
                    <td className="td">{a.username ?? '—'}</td>
                    <td className="td">
                      <Badge tone={a.action.includes('FAIL') ? 'danger' : a.action.includes('LOGIN') ? 'info' : 'default'}>
                        {a.action}
                      </Badge>
                    </td>
                    <td className="td text-xs text-muted">
                      {a.entityType ?? '—'} {a.entityId ? `#${a.entityId}` : ''}
                    </td>
                    <td className="td text-xs text-muted max-w-md truncate">{a.details ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onPage={setPage} />
          </div>
        )}
      </Card>
      <p className="text-xs text-muted mt-3">Consultado: {date(new Date().toISOString())}</p>
    </div>
  )
}
