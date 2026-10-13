import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { api, ApiError } from '@/lib/api'
import { Button, Card, EmptyState, Field, Input, Money, PageHeader, Select } from '@/components/ui'
import { messageOf, money, qty } from '@/lib/format'
import type { Customer, Product } from '@/types'

interface CartItem {
  productId: number
  sku: string
  name: string
  stock: number
  quantity: number
  unitPrice: number
  discountPercent: number
}

const METHODS: { code: string; label: string }[] = [
  { code: 'CASH', label: 'Efectivo' },
  { code: 'CARD', label: 'Tarjeta' },
  { code: 'TRANSFER', label: 'Transferencia' },
  { code: 'CHECK', label: 'Cheque' },
  { code: 'OTHER', label: 'Otro' },
]

export default function PosPage() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Product[]>([])
  const [cart, setCart] = useState<CartItem[]>([])
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [customerQuery, setCustomerQuery] = useState('')
  const [customers, setCustomers] = useState<Customer[]>([])
  const [payNow, setPayNow] = useState(true)
  const [method, setMethod] = useState('CASH')
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const debounce = useRef<number | undefined>(undefined)

  useEffect(() => {
    window.clearTimeout(debounce.current)
    if (query.trim().length < 1) {
      setResults([])
      return
    }
    debounce.current = window.setTimeout(() => {
      api
        .get<{ content: Product[] }>(`/api/products?search=${encodeURIComponent(query)}&size=8&active=true`)
        .then((r) => setResults(r.content))
        .catch(() => setResults([]))
    }, 250)
  }, [query])

  useEffect(() => {
    window.clearTimeout(debounce.current)
    if (customerQuery.trim().length < 2) {
      setCustomers([])
      return
    }
    debounce.current = window.setTimeout(() => {
      api
        .get<{ content: Customer[] }>(`/api/customers?search=${encodeURIComponent(customerQuery)}&size=5&active=true`)
        .then((r) => setCustomers(r.content))
        .catch(() => setCustomers([]))
    }, 250)
  }, [customerQuery])

  function addProduct(p: Product) {
    setCart((prev) => {
      const found = prev.find((i) => i.productId === p.id)
      if (found) {
        return prev.map((i) => (i.productId === p.id ? { ...i, quantity: i.quantity + 1 } : i))
      }
      return [
        ...prev,
        {
          productId: p.id,
          sku: p.sku,
          name: p.name,
          stock: Number(p.stock),
          quantity: 1,
          unitPrice: Number(p.salePrice),
          discountPercent: 0,
        },
      ]
    })
    setQuery('')
    setResults([])
  }

  function updateItem(productId: number, patch: Partial<CartItem>) {
    setCart((prev) => prev.map((i) => (i.productId === productId ? { ...i, ...patch } : i)))
  }

  const totals = useMemo(() => {
    let subtotal = 0
    let discount = 0
    let tax = 0
    for (const i of cart) {
      const gross = i.quantity * i.unitPrice
      const d = (gross * i.discountPercent) / 100
      const base = gross - d
      subtotal += gross
      discount += d
      tax += base * 0.15
    }
    const total = subtotal - discount + tax
    return { subtotal, discount, tax, total }
  }, [cart])

  async function submit(issue: boolean) {
    if (cart.length === 0) {
      toast.error('Agregue al menos un producto.')
      return
    }
    setBusy(true)
    setFieldErrors({})
    try {
      const sale = await api.post<{ id: number }>('/api/sales', {
        customerId: customer?.id ?? null,
        saleDate: new Date().toISOString().slice(0, 10),
        notes: notes.trim() || null,
        issue,
        items: cart.map((i) => ({
          productId: i.productId,
          quantity: i.quantity,
          unitPrice: i.unitPrice,
          discountPercent: i.discountPercent || null,
        })),
        initialPaymentAmount: issue && payNow && totals.total > 0 ? totals.total : null,
        initialPaymentMethod: issue && payNow && totals.total > 0 ? method : null,
      }, { 'Idempotency-Key': crypto.randomUUID() })
      toast.success(issue ? 'Venta emitida.' : 'Borrador guardado.')
      navigate(`/ventas/${sale.id}`)
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors) setFieldErrors(e.fieldErrors)
      toast.error(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <PageHeader title="Nueva venta" subtitle="Punto de venta" />

      <div className="grid lg:grid-cols-3 gap-4 items-start">
        <Card className="p-4 lg:col-span-2 space-y-4">
          <div>
            <Field label="Buscar producto" error={fieldErrors.items}>
              <Input
                placeholder="Escriba SKU, nombre o codigo de barras y seleccione..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                autoFocus
              />
            </Field>
            {results.length > 0 && (
              <div className="border border-border rounded-lg mt-1 divide-y divide-border bg-surface max-h-64 overflow-y-auto">
                {results.map((p) => (
                  <button
                    key={p.id}
                    onClick={() => addProduct(p)}
                    className="w-full text-left px-3 py-2 hover:bg-background flex items-center justify-between gap-3 cursor-pointer"
                  >
                    <span>
                      <span className="font-medium text-sm">{p.name}</span>
                      <span className="text-xs text-muted ml-2 font-mono">{p.sku}</span>
                    </span>
                    <span className="text-sm tabular-nums">
                      ${money(p.salePrice)} · stock {qty(p.stock)}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {cart.length === 0 ? (
            <EmptyState title="Carrito vacio" description="Busque un producto para empezar la venta." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr>
                    <th className="th">Producto</th>
                    <th className="th w-24">Cant.</th>
                    <th className="th w-32">Precio</th>
                    <th className="th w-24">Dto %</th>
                    <th className="th text-right">Total</th>
                    <th className="th w-10" />
                  </tr>
                </thead>
                <tbody>
                  {cart.map((i) => {
                    const gross = i.quantity * i.unitPrice
                    const line = gross - (gross * i.discountPercent) / 100
                    const overStock = i.quantity > i.stock
                    return (
                      <tr key={i.productId}>
                        <td className="td">
                          <p className="font-medium">{i.name}</p>
                          <p className="text-xs text-muted font-mono">
                            {i.sku} · stock {qty(i.stock)}
                          </p>
                          {overStock && <p className="text-xs text-red-600">Cantidad mayor al stock</p>}
                        </td>
                        <td className="td">
                          <Input
                            type="number"
                            min="0.001"
                            step="0.001"
                            value={i.quantity}
                            onChange={(e) => updateItem(i.productId, { quantity: Number(e.target.value) })}
                            className="!py-1"
                          />
                        </td>
                        <td className="td">
                          <Input
                            type="number"
                            min="0"
                            step="0.01"
                            value={i.unitPrice}
                            onChange={(e) => updateItem(i.productId, { unitPrice: Number(e.target.value) })}
                            className="!py-1"
                          />
                        </td>
                        <td className="td">
                          <Input
                            type="number"
                            min="0"
                            max="100"
                            step="0.01"
                            value={i.discountPercent}
                            onChange={(e) => updateItem(i.productId, { discountPercent: Number(e.target.value) })}
                            className="!py-1"
                          />
                        </td>
                        <td className="td text-right tabular-nums font-semibold">${money(line)}</td>
                        <td className="td text-center">
                          <button
                            className="text-red-500 hover:text-red-700 cursor-pointer"
                            onClick={() => setCart((c) => c.filter((x) => x.productId !== i.productId))}
                            aria-label="Quitar"
                          >
                            ×
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <Card className="p-4 space-y-4">
          <div className="relative">
            <Field label="Cliente (opcional)">
              <Input
                placeholder="Buscar cliente por cedula o nombre..."
                value={customerQuery}
                onChange={(e) => setCustomerQuery(e.target.value)}
              />
            </Field>
            {customers.length > 0 && (
              <div className="border border-border rounded-lg mt-1 divide-y divide-border bg-surface absolute z-10 w-full">
                {customers.map((c) => (
                  <button
                    key={c.id}
                    onClick={() => {
                      setCustomer(c)
                      setCustomerQuery('')
                      setCustomers([])
                    }}
                    className="w-full text-left px-3 py-2 hover:bg-background cursor-pointer"
                  >
                    <p className="text-sm font-medium">{c.businessName}</p>
                    <p className="text-xs text-muted">
                      {c.idType} {c.identification}
                    </p>
                  </button>
                ))}
              </div>
            )}
            {customer && (
              <div className="mt-2 rounded-lg bg-background border border-border px-3 py-2 flex items-start justify-between gap-2">
                <div>
                  <p className="text-sm font-medium">{customer.businessName}</p>
                  <p className="text-xs text-muted">
                    {customer.idType} {customer.identification}
                  </p>
                </div>
                <button className="text-xs text-brand hover:underline" onClick={() => setCustomer(null)}>
                  Quitar
                </button>
              </div>
            )}
            {!customer && <p className="text-xs text-muted mt-1">Se registrara como consumidor final.</p>}
          </div>

          <div className="border-t border-border pt-3 space-y-1.5 text-sm">
            <div className="flex justify-between">
              <span className="text-muted">Subtotal</span>
              <span className="tabular-nums">${money(totals.subtotal)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted">Descuentos</span>
              <span className="tabular-nums">-${money(totals.discount)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted">IVA</span>
              <span className="tabular-nums">${money(totals.tax)}</span>
            </div>
            <div className="flex justify-between font-bold text-base border-t border-border pt-2">
              <span>Total</span>
              <Money value={totals.total} />
            </div>
          </div>

          <div className="flex items-center gap-2 text-sm">
            <input type="checkbox" id="paynow" checked={payNow} onChange={(e) => setPayNow(e.target.checked)} />
            <label htmlFor="paynow">Registrar pago completo ahora</label>
          </div>
          {payNow && (
            <Field label="Metodo de pago">
              <Select value={method} onChange={(e) => setMethod(e.target.value)}>
                {METHODS.map((m) => (
                  <option key={m.code} value={m.code}>
                    {m.label}
                  </option>
                ))}
              </Select>
            </Field>
          )}

          <Field label="Notas">
            <Input value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={500} />
          </Field>

          <div className="space-y-2">
            <Button className="w-full" disabled={busy || cart.length === 0} onClick={() => void submit(true)}>
              {busy ? 'Procesando...' : 'Emitir venta'}
            </Button>
            <Button
              variant="secondary"
              className="w-full"
              disabled={busy || cart.length === 0}
              onClick={() => void submit(false)}
            >
              Guardar borrador
            </Button>
          </div>
          <p className="text-[11px] text-muted">
            El backend calcula los importes oficiales; los valores aqui son una vista previa.
          </p>
        </Card>
      </div>
    </div>
  )
}
