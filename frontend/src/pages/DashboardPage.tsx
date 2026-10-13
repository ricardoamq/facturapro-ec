import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle, Banknote, Boxes, DollarSign, FileText, TrendingUp } from 'lucide-react'
import { api } from '@/lib/api'
import { Card, PageHeader, Spinner, StatCard, Badge, EmptyState } from '@/components/ui'
import { dateTime, money } from '@/lib/format'
import type { DashboardSummary } from '@/types'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

export default function DashboardPage() {
  const [data, setData] = useState<DashboardSummary | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<DashboardSummary>('/api/dashboard/summary')
      .then(setData)
      .catch((e) => setError(e.message))
  }, [])

  if (error) return <Card className="p-6 text-red-600">{error}</Card>
  if (!data) return <Spinner label="Cargando panel..." />

  const chartData = data.evolution.map((d) => ({ ...d, fecha: d.date.slice(5) }))

  return (
    <div>
      <PageHeader title="Panel principal" subtitle="Resumen del negocio" />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-5">
        <StatCard
          title="Ventas de hoy"
          value={`$${money(data.today.total)}`}
          hint={`${data.today.count} venta(s)`}
          icon={<DollarSign size={20} />}
        />
        <StatCard
          title="Mes en curso"
          value={`$${money(data.month.total)}`}
          hint={`${data.month.count} venta(s)`}
          icon={<TrendingUp size={20} />}
        />
        <StatCard
          title="Por cobrar"
          value={`$${money(data.receivablesTotal)}`}
          hint={`${data.pendingInvoicesCount} factura(s) pendientes`}
          icon={<Banknote size={20} />}
          tone="warning"
        />
        <StatCard
          title="Stock bajo"
          value={String(data.lowStockCount)}
          hint={`Valor de inventario: $${money(data.stockValue)}`}
          icon={<AlertTriangle size={20} />}
          tone={data.lowStockCount > 0 ? 'danger' : 'success'}
        />
      </div>

      <div className="grid lg:grid-cols-3 gap-4">
        <Card className="p-4 lg:col-span-2">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-semibold text-sm">Ventas de los ultimos 14 dias</h2>
            <Link to="/reportes" className="text-xs text-brand hover:underline">
              Ver reportes
            </Link>
          </div>
          {chartData.length === 0 ? (
            <EmptyState title="Sin ventas registradas" description="Registre una venta para ver la evolucion." />
          ) : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="fecha" fontSize={11} stroke="var(--muted)" />
                  <YAxis fontSize={11} stroke="var(--muted)" />
                  <Tooltip
                    formatter={(v) => `$${money(Number(v ?? 0))}`}
                    contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }}
                  />
                  <Bar dataKey="total" fill="var(--brand)" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <Card className="p-4">
          <h2 className="font-semibold text-sm mb-3">Productos mas vendidos</h2>
          {data.topProducts.length === 0 ? (
            <p className="text-sm text-muted">Sin datos.</p>
          ) : (
            <ul className="space-y-2">
              {data.topProducts.map((p) => (
                <li key={p.sku} className="flex items-center justify-between gap-2 text-sm">
                  <div className="min-w-0">
                    <p className="truncate font-medium">{p.name}</p>
                    <p className="text-xs text-muted">{p.sku}</p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="font-semibold tabular-nums">${money(p.total)}</p>
                    <p className="text-xs text-muted">{p.quantity} ud.</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <Link to="/productos" className="inline-flex items-center gap-1 text-xs text-brand hover:underline mt-3">
            <Boxes size={13} /> Ver productos
          </Link>
        </Card>
      </div>

      <Card className="p-4 mt-4">
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-semibold text-sm">Actividad reciente</h2>
          <Link to="/auditoria" className="text-xs text-brand hover:underline">
            Auditoria completa
          </Link>
        </div>
        {data.recentActivity.length === 0 ? (
          <p className="text-sm text-muted">Sin actividad registrada.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr>
                  <th className="th">Fecha</th>
                  <th className="th">Usuario</th>
                  <th className="th">Accion</th>
                  <th className="th">Detalle</th>
                </tr>
              </thead>
              <tbody>
                {data.recentActivity.map((a) => (
                  <tr key={a.id}>
                    <td className="td whitespace-nowrap text-muted">{dateTime(a.createdAt)}</td>
                    <td className="td">{a.username ?? '—'}</td>
                    <td className="td">
                      <Badge tone="info">{a.action}</Badge>
                    </td>
                    <td className="td text-muted max-w-md truncate">
                      {a.details || `${a.entityType ?? ''} ${a.entityId ?? ''}`}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <div className="mt-4 flex gap-2 flex-wrap">
        <Link to="/pos" className="btn-primary">
          <FileText size={15} /> Nueva venta
        </Link>
        <Link to="/inventario" className="btn-secondary">
          Revisar inventario
        </Link>
      </div>
    </div>
  )
}
