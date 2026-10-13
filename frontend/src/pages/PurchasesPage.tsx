import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { useAuth } from '@/auth/AuthContext'
import { api, ApiError } from '@/lib/api'
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
} from '@/components/ui'
import { date, messageOf, money } from '@/lib/format'
import type { Page as PageT, Purchase, Supplier, Product } from '@/types'

interface ItemForm {
  productId: number
  sku: string
  name: string
  quantity: number
  unitCost: number
  discountPercent: number
}

export default function PurchasesPage() {
  const { can } = useAuth()
  const navigate = useNavigate()
  const [data, setData] = useState<PageT<Purchase> | null>(null)
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const [open, setOpen] = useState(false)
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [supplierId, setSupplierId] = useState('')
  const [supplierQuery, setSupplierQuery] = useState('')
  const [items, setItems] = useState<ItemForm[]>([])
  const [productQuery, setProductQuery] = useState('')
  const [results, setResults] = useState<Product[]>([])
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})

  const load = useCallback(async () => {
    setLoading(true)
    const params = new URLSearchParams({ page: String(page), size: '20' })
    if (status) params.set('status', status)
    try {
      setData(await api.get<PageT<Purchase>>(`/api/purchases?${params}`))
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setLoading(false)
    }
  }, [status, page])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (supplierQuery.trim().length < 2) {
      setSuppliers([])
      return
    }
    const t = setTimeout(() => {
      api
        .get<{ content: Supplier[] }>(`/api/suppliers?search=${encodeURIComponent(supplierQuery)}&size=5&active=true`)
        .then((r) => setSuppliers(r.content))
        .catch(() => setSuppliers([]))
    }, 250)
    return () => clearTimeout(t)
  }, [supplierQuery])

  useEffect(() => {
    if (productQuery.trim().length < 1) {
      setResults([])
      return
    }
    const t = setTimeout(() => {
      api
        .get<{ content: Product[] }>(`/api/products?search=${encodeURIComponent(productQuery)}&size=6`)
        .then((r) => setResults(r.content))
        .catch(() => setResults([]))
    }, 250)
    return () => clearTimeout(t)
  }, [productQuery])

  const total = items.reduce(
    (acc, i) => acc + i.quantity * i.unitCost - (i.quantity * i.unitCost * i.discountPercent) / 100,
    0,
  )

  async function createPurchase() {
    setBusy(true)
    setErrors({})
    try {
      const created = await api.post<Purchase>('/api/purchases', {
        supplierId: Number(supplierId),
        purchaseDate: new Date().toISOString().slice(0, 10),
        notes: notes.trim() || null,
        items: items.map((i) => ({
          productId: i.productId,
          quantity: i.quantity,
          unitCost: i.unitCost,
          discountPercent: i.discountPercent || null,
        })),
      })
      toast.success('Compra creada como borrador.')
      setOpen(false)
      resetForm()
      navigate(`/compras/${created.id}`)
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors) setErrors(e.fieldErrors)
      toast.error(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  function resetForm() {
    setSupplierId('')
    setSupplierQuery('')
    setItems([])
    setProductQuery('')
    setNotes('')
  }

  const statusTone = (s: string) =>
    s === 'CONFIRMED' ? 'success' : s === 'CANCELLED' ? 'danger' : 'default'

  return (
    <div>
      <PageHeader
        title="Compras"
        subtitle="Entradas de mercaderia y costos"
        actions={can('PURCHASES_WRITE') ? <Button onClick={() => setOpen(true)}>Nueva compra</Button> : undefined}
      />

      <Card>
        <div className="p-3 border-b border-border">
          <Select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value)
              setPage(0)
            }}
            className="w-auto"
          >
            <option value="">Todos los estados</option>
            <option value="DRAFT">Borrador</option>
            <option value="CONFIRMED">Confirmada</option>
            <option value="CANCELLED">Cancelada</option>
          </Select>
        </div>

        {loading && !data ? (
          <Spinner />
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            title="No hay compras"
            description="Registre una compra para ingresar mercaderia al inventario."
            action={
              can('PURCHASES_WRITE') ? <Button onClick={() => setOpen(true)}>Nueva compra</Button> : undefined
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr>
                  <th className="th">Numero</th>
                  <th className="th">Fecha</th>
                  <th className="th">Proveedor</th>
                  <th className="th">Estado</th>
                  <th className="th text-right">Total</th>
                  <th className="th">Creada por</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr
                    key={p.id}
                    className="hover:bg-background/60 cursor-pointer"
                    onClick={() => navigate(`/compras/${p.id}`)}
                  >
                    <td className="td font-mono text-xs font-semibold">{p.number}</td>
                    <td className="td whitespace-nowrap">{date(p.purchaseDate)}</td>
                    <td className="td font-medium">{p.supplierName}</td>
                    <td className="td">
                      <Badge tone={statusTone(p.status)}>{p.status === 'CONFIRMED' ? 'Confirmada' : p.status === 'CANCELLED' ? 'Cancelada' : 'Borrador'}</Badge>
                    </td>
                    <td className="td text-right tabular-nums font-semibold">${money(p.total)}</td>
                    <td className="td text-muted text-xs">{p.createdByName ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onPage={setPage} />
          </div>
        )}
      </Card>

      <Modal open={open} title="Nueva compra" onClose={() => setOpen(false)} wide>
        <div className="space-y-3">
          <div className="relative">
            <Field label="Proveedor" required error={errors.supplierId}>
              <Input
                placeholder="Buscar proveedor..."
                value={supplierQuery}
                onChange={(e) => setSupplierQuery(e.target.value)}
              />
            </Field>
            {suppliers.length > 0 && !supplierId && (
              <div className="border border-border rounded-lg mt-1 divide-y divide-border bg-surface absolute z-10 w-full">
                {suppliers.map((s) => (
                  <button
                    key={s.id}
                    className="w-full text-left px-3 py-2 hover:bg-background cursor-pointer"
                    onClick={() => {
                      setSupplierId(String(s.id))
                      setSupplierQuery(s.businessName)
                      setSuppliers([])
                    }}
                  >
                    <p className="text-sm font-medium">{s.businessName}</p>
                    <p className="text-xs text-muted font-mono">{s.identification}</p>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="relative">
            <Field label="Agregar producto">
              <Input
                placeholder="Buscar producto..."
                value={productQuery}
                onChange={(e) => setProductQuery(e.target.value)}
              />
            </Field>
            {results.length > 0 && (
              <div className="border border-border rounded-lg mt-1 divide-y divide-border bg-surface absolute z-10 w-full max-h-52 overflow-y-auto">
                {results.map((p) => (
                  <button
                    key={p.id}
                    className="w-full text-left px-3 py-2 hover:bg-background text-sm cursor-pointer"
                    onClick={() => {
                      setItems((prev) =>
                        prev.some((i) => i.productId === p.id)
                          ? prev
                          : [
                              ...prev,
                              {
                                productId: p.id,
                                sku: p.sku,
                                name: p.name,
                                quantity: 1,
                                unitCost: Number(p.purchasePrice),
                                discountPercent: 0,
                              },
                            ],
                      )
                      setProductQuery('')
                      setResults([])
                    }}
                  >
                    {p.name} <span className="text-muted font-mono text-xs">({p.sku})</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {items.length > 0 && (
            <div className="overflow-x-auto border border-border rounded-lg">
              <table className="w-full">
                <thead>
                  <tr>
                    <th className="th">Producto</th>
                    <th className="th w-24">Cant.</th>
                    <th className="th w-32">Costo</th>
                    <th className="th w-24">Dto %</th>
                    <th className="th text-right">Subtotal</th>
                    <th className="th w-8" />
                  </tr>
                </thead>
                <tbody>
                  {items.map((i) => (
                    <tr key={i.productId}>
                      <td className="td">
                        <p className="text-sm font-medium">{i.name}</p>
                        <p className="text-xs text-muted font-mono">{i.sku}</p>
                      </td>
                      <td className="td">
                        <Input
                          type="number"
                          min="0.001"
                          step="0.001"
                          className="!py-1"
                          value={i.quantity}
                          onChange={(e) =>
                            setItems((prev) =>
                              prev.map((x) => (x.productId === i.productId ? { ...x, quantity: Number(e.target.value) } : x)),
                            )
                          }
                        />
                      </td>
                      <td className="td">
                        <Input
                          type="number"
                          min="0"
                          step="0.01"
                          className="!py-1"
                          value={i.unitCost}
                          onChange={(e) =>
                            setItems((prev) =>
                              prev.map((x) => (x.productId === i.productId ? { ...x, unitCost: Number(e.target.value) } : x)),
                            )
                          }
                        />
                      </td>
                      <td className="td">
                        <Input
                          type="number"
                          min="0"
                          max="100"
                          step="0.01"
                          className="!py-1"
                          value={i.discountPercent}
                          onChange={(e) =>
                            setItems((prev) =>
                              prev.map((x) =>
                                x.productId === i.productId ? { ...x, discountPercent: Number(e.target.value) } : x,
                              ),
                            )
                          }
                        />
                      </td>
                      <td className="td text-right tabular-nums text-sm">
                        ${money(i.quantity * i.unitCost - (i.quantity * i.unitCost * i.discountPercent) / 100)}
                      </td>
                      <td className="td text-center">
                        <button
                          className="text-red-500 cursor-pointer"
                          onClick={() => setItems((prev) => prev.filter((x) => x.productId !== i.productId))}
                        >
                          ×
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <Field label="Notas">
            <Input value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={500} />
          </Field>

          <div className="flex justify-between items-center border-t border-border pt-3">
            <span className="text-sm text-muted">Total estimado (IVA segun producto)</span>
            <span className="font-bold tabular-nums">${money(total)}</span>
          </div>
          {errors.items && <p className="text-xs text-red-600">{errors.items}</p>}
        </div>

        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setOpen(false)}>
            Cancelar
          </Button>
          <Button disabled={busy || !supplierId || items.length === 0} onClick={createPurchase}>
            Crear borrador
          </Button>
        </div>
      </Modal>

      <p className="text-xs text-muted mt-3">
        <Link to="/inventario" className="text-brand hover:underline">
          Ver inventario
        </Link>{' '}
        · la compra solo incrementa existencias al confirmarse.
      </p>
    </div>
  )
}
