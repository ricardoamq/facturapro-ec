import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { useAuth } from '@/auth/AuthContext'
import { api, ApiError, download } from '@/lib/api'
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  Field,
  Input,
  Modal,
  PageHeader,
  Select,
  Spinner,
} from '@/components/ui'
import { date, dateTime, messageOf, money, paymentStatusLabel, saleStatusLabel } from '@/lib/format'
import type { Sale } from '@/types'

const METHODS: Record<string, string> = {
  CASH: 'Efectivo',
  CARD: 'Tarjeta',
  TRANSFER: 'Transferencia',
  CHECK: 'Cheque',
  OTHER: 'Otro',
}

export default function SaleDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { can } = useAuth()
  const [sale, setSale] = useState<Sale | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [permissions, setPermissions] = useState({ canVoid: false, canPay: false })
  const [voidOpen, setVoidOpen] = useState(false)
  const [voidReason, setVoidReason] = useState('')
  const [payOpen, setPayOpen] = useState(false)
  const [amount, setAmount] = useState('')
  const [method, setMethod] = useState('CASH')
  const [reference, setReference] = useState('')
  const [busy, setBusy] = useState(false)
  const [payVoid, setPayVoid] = useState<number | null>(null)

  const load = useCallback(async () => {
    try {
      const s = await api.get<Sale>(`/api/sales/${id}`)
      setSale(s)
      if (s && Number(s.balance) > 0 && !payOpen) setAmount(String(s.balance))
    } catch (e) {
      setError(messageOf(e))
    }
  }, [id, payOpen])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    api
      .get<{ canVoid: boolean; canPay: boolean }>('/api/sales/permissions')
      .then(setPermissions)
      .catch(() => undefined)
  }, [])

  if (error) return <Card className="p-6 text-red-600">{error}</Card>
  if (!sale) return <Spinner label="Cargando venta..." />

  async function issue() {
    setBusy(true)
    try {
      await api.post(`/api/sales/${sale!.id}/issue`, {})
      toast.success('Venta emitida.')
      await load()
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  async function voidSale() {
    setBusy(true)
    try {
      await api.post(`/api/sales/${sale!.id}/void`, { reason: voidReason.trim() })
      toast.success('Venta anulada.')
      setVoidOpen(false)
      setVoidReason('')
      await load()
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  async function registerPayment() {
    setBusy(true)
    try {
      await api.post(
        `/api/sales/${sale!.id}/payments`,
        {
          amount: Number(amount),
          method,
          reference: reference.trim() || null,
          notes: null,
        },
        { 'Idempotency-Key': crypto.randomUUID() },
      )
      toast.success('Pago registrado.')
      setPayOpen(false)
      setReference('')
      await load()
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors) toast.error(Object.values(e.fieldErrors).join(' '))
      toast.error(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  async function voidPayment() {
    if (payVoid == null) return
    try {
      await api.post(`/api/payments/${payVoid}/void`, { reason: 'Anulado desde el detalle de la venta' })
      toast.success('Pago anulado.')
      setPayVoid(null)
      await load()
    } catch (e) {
      toast.error(messageOf(e))
    }
  }

  async function getPdf() {
    if (!sale?.number) return
    try {
      await download(`/api/invoices/${sale.number}/pdf`, `${sale.number}.pdf`)
    } catch (e) {
      toast.error(messageOf(e))
    }
  }

  const editable = sale.status === 'DRAFT' && can('SALES_WRITE')

  return (
    <div>
      <PageHeader
        title={sale.number ?? `Venta #${sale.id}`}
        subtitle={`Fecha ${date(sale.saleDate)} · ${saleStatusLabel(sale.status)}`}
        actions={
          <>
            <Link to="/ventas" className="btn-secondary">
              Volver
            </Link>
            {sale.status === 'DRAFT' && editable && (
              <Button onClick={issue} disabled={busy}>
                Emitir
              </Button>
            )}
            {sale.number && (
              <>
                <Button variant="secondary" onClick={() => window.open(`/api/invoices/${sale.number}/html`, '_blank')}>
                  Ver HTML
                </Button>
                <Button variant="secondary" onClick={getPdf}>
                  Descargar PDF
                </Button>
              </>
            )}
            {sale.status === 'ISSUED' && permissions.canVoid && (
              <Button variant="danger" onClick={() => setVoidOpen(true)}>
                Anular venta
              </Button>
            )}
          </>
        }
      />

      <div className="grid lg:grid-cols-3 gap-4 items-start">
        <Card className="lg:col-span-2 p-4">
          <div className="grid sm:grid-cols-2 gap-4 mb-4 text-sm">
            <div>
              <p className="label">Cliente</p>
              <p className="font-medium">{sale.customerName ?? 'Consumidor final'}</p>
              <p className="text-xs text-muted font-mono">
                {sale.customerIdentification} {sale.customerAddress ? `· ${sale.customerAddress}` : ''}
              </p>
            </div>
            <div>
              <p className="label">Estados</p>
              <div className="flex gap-1.5 mt-1">
                <Badge tone={sale.status === 'VOIDED' ? 'danger' : sale.status === 'ISSUED' ? 'success' : 'default'}>
                  {saleStatusLabel(sale.status)}
                </Badge>
                <Badge tone={sale.paymentStatus === 'PAID' ? 'success' : 'warning'}>
                  {paymentStatusLabel(sale.paymentStatus)}
                </Badge>
              </div>
              {sale.voidedAt && (
                <p className="text-xs text-red-600 mt-1">
                  Anulada {dateTime(sale.voidedAt)}: {sale.voidReason}
                </p>
              )}
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr>
                  <th className="th">Producto</th>
                  <th className="th text-right">Cant.</th>
                  <th className="th text-right">Precio</th>
                  <th className="th text-right">Dto</th>
                  <th className="th text-right">IVA</th>
                  <th className="th text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                {sale.items.map((i) => (
                  <tr key={i.id}>
                    <td className="td">
                      <p className="font-medium">{i.productName}</p>
                      <p className="text-xs text-muted font-mono">{i.sku}</p>
                    </td>
                    <td className="td text-right tabular-nums">{i.quantity}</td>
                    <td className="td text-right tabular-nums">${money(i.unitPrice)}</td>
                    <td className="td text-right tabular-nums">{Number(i.lineDiscount) > 0 ? `-$${money(i.lineDiscount)}` : '—'}</td>
                    <td className="td text-right tabular-nums">{Number(i.taxRate) > 0 ? `${i.taxRate}%` : '0%'}</td>
                    <td className="td text-right tabular-nums font-semibold">${money(i.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex justify-end mt-4">
            <div className="w-64 text-sm space-y-1">
              <div className="flex justify-between">
                <span className="text-muted">Subtotal</span>
                <span className="tabular-nums">${money(sale.subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted">Descuentos</span>
                <span className="tabular-nums">-${money(sale.discountTotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted">IVA</span>
                <span className="tabular-nums">${money(sale.taxTotal)}</span>
              </div>
              <div className="flex justify-between font-bold border-t border-border pt-1.5">
                <span>Total</span>
                <span className="tabular-nums">${money(sale.total)}</span>
              </div>
              <div className="flex justify-between text-emerald-600">
                <span>Pagado</span>
                <span className="tabular-nums">${money(sale.amountPaid)}</span>
              </div>
              <div className="flex justify-between font-semibold text-red-600">
                <span>Saldo</span>
                <span className="tabular-nums">${money(sale.balance)}</span>
              </div>
            </div>
          </div>

          {sale.notes && (
            <p className="text-xs text-muted mt-3 border-t border-border pt-2">Notas: {sale.notes}</p>
          )}
        </Card>

        <div className="space-y-4">
          <Card className="p-4">
            <div className="flex items-center justify-between mb-3">
              <h2 className="font-semibold text-sm">Pagos</h2>
              {permissions.canPay && sale.status === 'ISSUED' && Number(sale.balance) > 0 && (
                <Button onClick={() => setPayOpen(true)}>Registrar pago</Button>
              )}
            </div>
            {sale.payments.length === 0 ? (
              <p className="text-sm text-muted">Sin pagos registrados.</p>
            ) : (
              <ul className="space-y-2">
                {sale.payments.map((p) => (
                  <li key={p.id} className="border border-border rounded-lg px-3 py-2 text-sm">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold tabular-nums">${money(p.amount)}</span>
                      <Badge tone={p.voided ? 'danger' : 'success'}>
                        {p.voided ? 'Anulado' : METHODS[p.paymentMethod] ?? p.paymentMethod}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted mt-0.5">
                      {dateTime(p.paymentDate)}
                      {p.reference ? ` · ref ${p.reference}` : ''}
                      {p.createdByName ? ` · ${p.createdByName}` : ''}
                    </p>
                    {!p.voided && permissions.canVoid && (
                      <button className="text-xs text-red-600 hover:underline mt-1" onClick={() => setPayVoid(p.id)}>
                        Anular pago
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card className="p-4 text-xs text-muted space-y-1">
            <p>Creada por {sale.createdByName ?? '—'} · {dateTime(sale.createdAt)}</p>
            {sale.issuedAt && <p>Emitida: {dateTime(sale.issuedAt)}</p>}
            <p>Comprobante interno - no autorizado electronicamente por el SRI.</p>
          </Card>
        </div>
      </div>

      <Modal open={voidOpen} title="Anular venta" onClose={() => setVoidOpen(false)}>
        <Field label="Motivo del anulacion" required>
          <Input value={voidReason} onChange={(e) => setVoidReason(e.target.value)} autoFocus />
        </Field>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setVoidOpen(false)}>
            Cancelar
          </Button>
          <Button variant="danger" disabled={!voidReason.trim() || busy} onClick={voidSale}>
            Anular
          </Button>
        </div>
      </Modal>

      <Modal open={payOpen} title="Registrar pago" onClose={() => setPayOpen(false)}>
        <div className="space-y-3">
          <Field label="Monto" required>
            <Input type="number" step="0.01" min="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} autoFocus />
          </Field>
          <Field label="Metodo" required>
            <Select value={method} onChange={(e) => setMethod(e.target.value)}>
              {Object.entries(METHODS).map(([k, v]) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Referencia">
            <Input value={reference} onChange={(e) => setReference(e.target.value)} maxLength={60} />
          </Field>
        </div>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setPayOpen(false)}>
            Cancelar
          </Button>
          <Button disabled={busy || !amount || Number(amount) <= 0} onClick={registerPayment}>
            Registrar
          </Button>
        </div>
      </Modal>

      <ConfirmDialog
        open={payVoid != null}
        title="Anular pago"
        message="Se anulara el pago y se recalculara el saldo de la venta."
        confirmLabel="Anular pago"
        danger
        onConfirm={voidPayment}
        onClose={() => setPayVoid(null)}
      />
      <button className="hidden" onClick={() => navigate(0)} />
    </div>
  )
}
