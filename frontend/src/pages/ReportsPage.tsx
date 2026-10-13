import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { api, download } from '@/lib/api'
import { Button, Card, EmptyState, Field, Input, PageHeader, Spinner, StatCard, Tabs } from '@/components/ui'
import { daysAgoIso, messageOf, money, monthStartIso, qty, todayIso } from '@/lib/format'
import { Banknote, DollarSign, Percent, TrendingUp } from 'lucide-react'

interface ProductRow {
  sku: string
  name: string
  quantity: number
  netSales: number
  tax: number
  total: number
  cost: number
  profit: number
}
interface PartyRow {
  identification: string
  name: string
  salesCount: number
  total: number
  netSales: number
  cost: number
  profit: number
}
interface TaxRow {
  rate: number
  taxAmount: number
  netSales: number
}
interface PurchasesSummary {
  from: string
  to: string
  purchasesCount: number
  total: number
  subtotal: number
  tax: number
  byDay: { date: string; purchasesCount: number; subtotal: number; tax: number; total: number }[]
}

type TabId = 'summary' | 'product' | 'customer' | 'user' | 'taxes' | 'purchases'

export default function ReportsPage() {
  const [tab, setTab] = useState<TabId>('summary')
  const [from, setFrom] = useState(monthStartIso)
  const [to, setTo] = useState(todayIso)
  const [loading, setLoading] = useState(false)
  const [summary, setSummary] = useState<import('@/types').SalesSummary | null>(null)
  const [products, setProducts] = useState<ProductRow[]>([])
  const [customers, setCustomers] = useState<PartyRow[]>([])
  const [users, setUsers] = useState<PartyRow[]>([])
  const [taxes, setTaxes] = useState<TaxRow[]>([])
  const [purchases, setPurchases] = useState<PurchasesSummary | null>(null)

  const load = useCallback(async () => {
    if (from > to) {
      toast.error('La fecha inicial no puede ser mayor que la final.')
      return
    }
    setLoading(true)
    try {
      const q = `from=${from}&to=${to}`
      const [s, p, c, u, t, pc] = await Promise.all([
        api.get<import('@/types').SalesSummary>(`/api/reports/sales?${q}`),
        api.get<ProductRow[]>(`/api/reports/sales/by-product?${q}`),
        api.get<PartyRow[]>(`/api/reports/sales/by-customer?${q}`),
        api.get<PartyRow[]>(`/api/reports/sales/by-user?${q}`),
        api.get<TaxRow[]>(`/api/reports/taxes?${q}`),
        api.get<PurchasesSummary>(`/api/reports/purchases?${q}`),
      ])
      setSummary(s)
      setProducts(p)
      setCustomers(c)
      setUsers(u)
      setTaxes(t)
      setPurchases(pc)
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setLoading(false)
    }
  }, [from, to])

  useEffect(() => {
    void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function exportCsv() {
    try {
      await download(`/api/reports/sales/export?from=${from}&to=${to}`, `ventas_${from}_${to}.csv`)
    } catch (e) {
      toast.error(messageOf(e))
    }
  }

  return (
    <div>
      <PageHeader
        title="Reportes"
        subtitle="Analisis de ventas, impuestos e inventario"
        actions={
          <Button variant="secondary" onClick={exportCsv}>
            Exportar ventas (CSV)
          </Button>
        }
      />

      <Card className="p-4 mb-4">
        <div className="flex flex-wrap gap-3 items-end">
          <Field label="Desde">
            <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </Field>
          <Field label="Hasta">
            <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </Field>
          <Button onClick={load} disabled={loading}>
            Aplicar
          </Button>
          <Button variant="ghost" onClick={() => { setFrom(daysAgoIso(30)); setTo(todayIso()) }}>
            Ultimos 30 dias
          </Button>
        </div>
      </Card>

      <Tabs
        active={tab}
        onChange={(id) => setTab(id as TabId)}
        tabs={[
          { id: 'summary', label: 'Resumen' },
          { id: 'product', label: 'Por producto' },
          { id: 'customer', label: 'Por cliente' },
          { id: 'user', label: 'Por usuario' },
          { id: 'taxes', label: 'Impuestos' },
          { id: 'purchases', label: 'Compras' },
        ]}
      />

      {loading ? (
        <Spinner label="Calculando reportes..." />
      ) : tab === 'summary' && summary ? (
        <div className="space-y-4">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <StatCard title="Ventas" value={String(summary.salesCount)} hint={`${summary.from} a ${summary.to}`} />
            <StatCard title="Total vendido" value={`$${money(summary.totalSales)}`} icon={<DollarSign size={20} />} />
            <StatCard title="Cobros recibidos" value={`$${money(summary.paymentsReceived)}`} icon={<Banknote size={20} />} />
            <StatCard
              title="Utilidad estimada"
              value={`$${money(summary.estimatedProfit)}`}
              hint={`Costo: $${money(summary.estimatedCost)}`}
              icon={<TrendingUp size={20} />}
              tone={Number(summary.estimatedProfit) >= 0 ? 'success' : 'danger'}
            />
          </div>
          <Card>
            <div className="p-3 border-b border-border flex justify-between text-sm">
              <span className="text-muted">Desglose</span>
              <span className="text-muted">IVA: ${money(summary.taxTotal)} · Descuentos: ${money(summary.discountTotal)}</span>
            </div>
            <div className="overflow-x-auto max-h-96 overflow-y-auto">
              <table className="w-full">
                <thead>
                  <tr>
                    <th className="th">Fecha</th>
                    <th className="th text-right">Ventas</th>
                    <th className="th text-right">Subtotal</th>
                    <th className="th text-right">IVA</th>
                    <th className="th text-right">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.byDay.map((d) => (
                    <tr key={d.date}>
                      <td className="td">{d.date}</td>
                      <td className="td text-right tabular-nums">{d.salesCount}</td>
                      <td className="td text-right tabular-nums">${money(d.subtotal)}</td>
                      <td className="td text-right tabular-nums">${money(d.tax)}</td>
                      <td className="td text-right tabular-nums font-semibold">${money(d.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {summary.byDay.length === 0 && <EmptyState title="Sin ventas en el rango" />}
          </Card>
        </div>
      ) : tab === 'product' ? (
        <Card>
          {products.length === 0 ? (
            <EmptyState title="Sin datos por producto" />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr>
                    <th className="th">SKU</th>
                    <th className="th">Producto</th>
                    <th className="th text-right">Cant.</th>
                    <th className="th text-right">Neto</th>
                    <th className="th text-right">IVA</th>
                    <th className="th text-right">Total</th>
                    <th className="th text-right">Costo</th>
                    <th className="th text-right">Utilidad</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((p) => (
                    <tr key={p.sku}>
                      <td className="td font-mono text-xs">{p.sku}</td>
                      <td className="td font-medium">{p.name}</td>
                      <td className="td text-right tabular-nums">{qty(p.quantity)}</td>
                      <td className="td text-right tabular-nums">${money(p.netSales)}</td>
                      <td className="td text-right tabular-nums">${money(p.tax)}</td>
                      <td className="td text-right tabular-nums">${money(p.total)}</td>
                      <td className="td text-right tabular-nums">${money(p.cost)}</td>
                      <td className={`td text-right tabular-nums font-semibold ${Number(p.profit) < 0 ? 'text-red-600' : 'text-emerald-600'}`}>
                        ${money(p.profit)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      ) : tab === 'customer' ? (
        <PartyTable rows={customers} firstHeader="Identificacion" />
      ) : tab === 'user' ? (
        <PartyTable rows={users} firstHeader="Usuario" />
      ) : tab === 'taxes' ? (
        <Card>
          {taxes.length === 0 ? (
            <EmptyState title="Sin datos de impuestos" />
          ) : (
            <table className="w-full">
              <thead>
                <tr>
                  <th className="th">Tarifa</th>
                  <th className="th text-right">Base imponible</th>
                  <th className="th text-right">Impuesto</th>
                </tr>
              </thead>
              <tbody>
                {taxes.map((t) => (
                  <tr key={String(t.rate)}>
                    <td className="td">
                      {t.rate}% <Percent size={12} className="inline" />
                    </td>
                    <td className="td text-right tabular-nums">${money(t.netSales)}</td>
                    <td className="td text-right tabular-nums font-semibold">${money(t.taxAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      ) : (
        <div className="space-y-4">
          {purchases && (
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
              <StatCard title="Compras" value={String(purchases.purchasesCount)} />
              <StatCard title="Total comprado" value={`$${money(purchases.total)}`} />
              <StatCard title="Subtotal" value={`$${money(purchases.subtotal)}`} />
              <StatCard title="IVA compras" value={`$${money(purchases.tax)}`} />
            </div>
          )}
          <Card>
            {purchases && purchases.byDay.length > 0 ? (
              <table className="w-full">
                <thead>
                  <tr>
                    <th className="th">Fecha</th>
                    <th className="th text-right">Compras</th>
                    <th className="th text-right">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {purchases.byDay.map((d) => (
                    <tr key={d.date}>
                      <td className="td">{d.date}</td>
                      <td className="td text-right tabular-nums">{d.purchasesCount}</td>
                      <td className="td text-right tabular-nums font-semibold">${money(d.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <EmptyState title="Sin compras en el rango" />
            )}
          </Card>
        </div>
      )}
    </div>
  )
}

function PartyTable({ rows, firstHeader }: { rows: PartyRow[]; firstHeader: string }) {
  if (rows.length === 0) return <Card><EmptyState title="Sin datos" /></Card>
  return (
    <Card>
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr>
              <th className="th">{firstHeader}</th>
              <th className="th">Nombre</th>
              <th className="th text-right">Ventas</th>
              <th className="th text-right">Total</th>
              <th className="th text-right">Neto</th>
              <th className="th text-right">Utilidad</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.identification ?? r.name}>
                <td className="td font-mono text-xs">{r.identification ?? '—'}</td>
                <td className="td font-medium">{r.name}</td>
                <td className="td text-right tabular-nums">{r.salesCount}</td>
                <td className="td text-right tabular-nums font-semibold">${money(r.total)}</td>
                <td className="td text-right tabular-nums">${money(r.netSales)}</td>
                <td className={`td text-right tabular-nums ${Number(r.profit) < 0 ? 'text-red-600' : 'text-emerald-600'}`}>
                  ${money(r.profit)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}
