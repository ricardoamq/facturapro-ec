import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { useAuth } from '@/auth/AuthContext'
import { api } from '@/lib/api'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  Pagination,
  Select,
  Spinner,
  StatCard,
} from '@/components/ui'
import { date, dateTime, messageOf, money, qty } from '@/lib/format'
import { AlertTriangle, Boxes, DollarSign } from 'lucide-react'
import type { InventoryRow, Page as PageT, StockMovement } from '@/types'

interface Summary {
  activeProducts: number
  lowStockProducts: number
  stockValue: number
  allowNegativeStock: boolean
}

const TYPE_LABEL: Record<string, string> = {
  PURCHASE: 'Compra',
  PURCHASE_RETURN: 'Devolucion compra',
  SALE: 'Venta',
  SALE_RETURN: 'Devolucion venta',
  ADJUSTMENT: 'Ajuste',
}

export default function InventoryPage() {
  const { can } = useAuth()
  const [tab, setTab] = useState<'stock' | 'movements' | 'adjust'>('stock')
  const [summary, setSummary] = useState<Summary | null>(null)
  const [rows, setRows] = useState<InventoryRow[] | null>(null)
  const [movements, setMovements] = useState<PageT<StockMovement> | null>(null)
  const [movPage, setMovPage] = useState(0)
  const [movType, setMovType] = useState('')
  const [loading, setLoading] = useState(true)
  const [adjOpen, setAdjOpen] = useState(false)
  const [adjQty, setAdjQty] = useState('')
  const [adjReason, setAdjReason] = useState('')
  const [productQuery, setProductQuery] = useState('')
  const [productResults, setProductResults] = useState<{ id: number; sku: string; name: string; stock: number }[]>([])
  const [chosen, setChosen] = useState<{ id: number; sku: string; name: string; stock: number } | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [s, inv] = await Promise.all([
        api.get<Summary>('/api/inventory/summary'),
        api.get<InventoryRow[]>('/api/reports/inventory'),
      ])
      setSummary(s)
      setRows(inv)
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    const params = new URLSearchParams({ page: String(movPage), size: '20' })
    if (movType) params.set('type', movType)
    api
      .get<PageT<StockMovement>>(`/api/inventory/movements?${params}`)
      .then(setMovements)
      .catch(() => setMovements(null))
  }, [movPage, movType, tab])

  useEffect(() => {
    if (productQuery.trim().length < 1) {
      setProductResults([])
      return
    }
    const t = setTimeout(() => {
      api
        .get<{ content: { id: number; sku: string; name: string; stock: number }[] }>(
          `/api/products?search=${encodeURIComponent(productQuery)}&size=6`,
        )
        .then((r) => setProductResults(r.content))
        .catch(() => setProductResults([]))
    }, 250)
    return () => clearTimeout(t)
  }, [productQuery])

  async function saveAdjustment() {
    if (!chosen) return
    try {
      await api.post('/api/inventory/adjustments', {
        productId: chosen.id,
        quantity: Number(adjQty),
        reason: adjReason.trim(),
      })
      toast.success('Ajuste registrado.')
      setAdjOpen(false)
      setChosen(null)
      setProductQuery('')
      setAdjQty('')
      setAdjReason('')
      setTab('movements')
      await load()
    } catch (e) {
      toast.error(messageOf(e))
    }
  }

  const totalStockValue = rows?.reduce((acc, r) => acc + Number(r.stockValue), 0) ?? 0

  return (
    <div>
      <PageHeader
        title="Inventario"
        subtitle="Existencias, movimientos y ajustes"
        actions={
          can('INVENTORY_ADJUST') ? (
            <Button
              onClick={() => {
                setAdjOpen(true)
                setTab('adjust')
              }}
            >
              Registrar ajuste
            </Button>
          ) : undefined
        }
      />

      {summary && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-5">
          <StatCard title="Productos activos" value={String(summary.activeProducts)} icon={<Boxes size={20} />} />
          <StatCard
            title="Bajo minimo"
            value={String(summary.lowStockProducts)}
            icon={<AlertTriangle size={20} />}
            tone={summary.lowStockProducts > 0 ? 'danger' : 'success'}
          />
          <StatCard title="Valor de inventario" value={`$${money(summary.stockValue)}`} icon={<DollarSign size={20} />} />
          <StatCard
            title="Stock negativo"
            value={summary.allowNegativeStock ? 'Permitido' : 'Bloqueado'}
            hint="Configuracion del sistema"
          />
        </div>
      )}

      <div className="flex gap-1 border-b border-border mb-4">
        {(
          [
            ['stock', 'Existencias'],
            ['movements', 'Movimientos'],
            ['adjust', 'Ajustes'],
          ] as const
        ).map(([id, label]) => (
          <button
            key={id}
            onClick={() => setTab(id)}
            className={`px-3 py-2 text-sm font-medium border-b-2 -mb-px cursor-pointer ${
              tab === id ? 'border-brand text-brand' : 'border-transparent text-muted hover:text-foreground'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'stock' && (
        <Card>
          {loading && !rows ? (
            <Spinner />
          ) : !rows || rows.length === 0 ? (
            <EmptyState title="Sin productos" description="Cree productos para ver sus existencias." />
          ) : (
            <div className="overflow-x-auto">
              <div className="p-3 border-b border-border text-sm text-muted">
                {rows.length} producto(s) · valor total calculado: ${money(totalStockValue)}
              </div>
              <table className="w-full">
                <thead>
                  <tr>
                    <th className="th">SKU</th>
                    <th className="th">Producto</th>
                    <th className="th">Categoria</th>
                    <th className="th text-right">Existencia</th>
                    <th className="th text-right">Minimo</th>
                    <th className="th text-right">Costo unit.</th>
                    <th className="th text-right">Valor</th>
                    <th className="th">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.productId} className="hover:bg-background/60">
                      <td className="td font-mono text-xs">{r.sku}</td>
                      <td className="td font-medium">{r.name}</td>
                      <td className="td text-muted">{r.categoryName ?? '—'}</td>
                      <td className={`td text-right tabular-nums ${r.lowStock ? 'text-red-600 font-semibold' : ''}`}>
                        {qty(r.stock)}
                      </td>
                      <td className="td text-right tabular-nums text-muted">{qty(r.minStock)}</td>
                      <td className="td text-right tabular-nums">${money(r.unitCost)}</td>
                      <td className="td text-right tabular-nums font-semibold">${money(r.stockValue)}</td>
                      <td className="td">
                        {r.lowStock ? <Badge tone="danger">Bajo minimo</Badge> : <Badge tone="success">OK</Badge>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}

      {tab === 'movements' && (
        <Card>
          <div className="p-3 border-b border-border flex gap-2">
            <Select value={movType} onChange={(e) => setMovType(e.target.value)} className="w-auto">
              <option value="">Todos los tipos</option>
              <option value="PURCHASE">Compra</option>
              <option value="SALE">Venta</option>
              <option value="ADJUSTMENT">Ajuste</option>
              <option value="PURCHASE_RETURN">Devolucion compra</option>
              <option value="SALE_RETURN">Devolucion venta</option>
            </Select>
          </div>
          {!movements ? (
            <Spinner />
          ) : movements.content.length === 0 ? (
            <EmptyState title="Sin movimientos" />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr>
                    <th className="th">Fecha</th>
                    <th className="th">Producto</th>
                    <th className="th">Tipo</th>
                    <th className="th text-right">Cantidad</th>
                    <th className="th text-right">Saldo</th>
                    <th className="th">Motivo</th>
                    <th className="th">Usuario</th>
                  </tr>
                </thead>
                <tbody>
                  {movements.content.map((m) => (
                    <tr key={m.id} className="hover:bg-background/60">
                      <td className="td whitespace-nowrap text-muted">{dateTime(m.createdAt)}</td>
                      <td className="td">
                        <p className="font-medium">{m.productName}</p>
                        <p className="text-xs text-muted font-mono">{m.productSku}</p>
                      </td>
                      <td className="td">
                        <Badge tone={m.movementType === 'SALE' ? 'info' : m.movementType === 'PURCHASE' ? 'success' : 'default'}>
                          {TYPE_LABEL[m.movementType] ?? m.movementType}
                        </Badge>
                      </td>
                      <td className="td text-right tabular-nums">{qty(m.quantity)}</td>
                      <td className="td text-right tabular-nums">{qty(m.balanceAfter)}</td>
                      <td className="td text-muted text-xs max-w-48 truncate">{m.reason ?? '—'}</td>
                      <td className="td text-muted text-xs">{m.username ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Pagination
                page={movements.page}
                totalPages={movements.totalPages}
                totalElements={movements.totalElements}
                onPage={setMovPage}
              />
            </div>
          )}
        </Card>
      )}

      {tab === 'adjust' && (
        <Card className="p-5 max-w-lg">
          {can('INVENTORY_ADJUST') ? (
            <div className="space-y-3">
              <p className="text-sm text-muted">
                El ajuste modifica la existencia de un producto con motivo obligatorio. Se registra en la auditoria.
              </p>
              <Button onClick={() => setAdjOpen(true)}>Nuevo ajuste</Button>
            </div>
          ) : (
            <p className="text-sm text-muted">Su rol no tiene permiso para ajustar inventario (INVENTORY_ADJUST).</p>
          )}
        </Card>
      )}

      <Modal open={adjOpen} title="Registrar ajuste de inventario" onClose={() => setAdjOpen(false)}>
        <div className="space-y-3">
          <Field label="Producto" required>
            <Input
              placeholder="Buscar producto..."
              value={productQuery}
              onChange={(e) => {
                setProductQuery(e.target.value)
                setChosen(null)
              }}
            />
            {productResults.length > 0 && !chosen && (
              <div className="border border-border rounded-lg mt-1 divide-y divide-border bg-surface max-h-48 overflow-y-auto">
                {productResults.map((p) => (
                  <button
                    key={p.id}
                    className="w-full text-left px-3 py-2 hover:bg-background text-sm cursor-pointer"
                    onClick={() => {
                      setChosen(p)
                      setProductQuery(`${p.sku} - ${p.name}`)
                      setProductResults([])
                    }}
                  >
                    {p.name} <span className="text-muted font-mono text-xs">({p.sku}) · stock {qty(p.stock)}</span>
                  </button>
                ))}
              </div>
            )}
          </Field>
          <Field label="Cantidad (positiva suma, negativa resta)" required>
            <Input type="number" step="0.001" value={adjQty} onChange={(e) => setAdjQty(e.target.value)} />
          </Field>
          <Field label="Motivo" required>
            <Input value={adjReason} onChange={(e) => setAdjReason(e.target.value)} maxLength={255} />
          </Field>
        </div>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setAdjOpen(false)}>
            Cancelar
          </Button>
          <Button disabled={!chosen || !adjQty || !adjReason.trim()} onClick={saveAdjustment}>
            Registrar ajuste
          </Button>
        </div>
      </Modal>

      {rows && rows.length > 0 && (
        <p className="text-xs text-muted mt-3">Ultima actualizacion: {date(new Date().toISOString())}</p>
      )}
    </div>
  )
}
