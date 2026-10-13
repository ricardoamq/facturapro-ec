import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { useAuth } from '@/auth/AuthContext'
import { api } from '@/lib/api'
import { Badge, Button, Card, Field, Input, Modal, PageHeader, Spinner } from '@/components/ui'
import { date, dateTime, messageOf, money } from '@/lib/format'
import type { Purchase } from '@/types'

export default function PurchaseDetailPage() {
  const { id } = useParams()
  const { can } = useAuth()
  const [purchase, setPurchase] = useState<Purchase | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [cancelOpen, setCancelOpen] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [returnOpen, setReturnOpen] = useState(false)
  const [returnQty, setReturnQty] = useState<Record<number, string>>({})
  const [returnReason, setReturnReason] = useState('')

  const load = useCallback(async () => {
    try {
      setPurchase(await api.get<Purchase>(`/api/purchases/${id}`))
    } catch (e) {
      setError(messageOf(e))
    }
  }, [id])

  useEffect(() => {
    void load()
  }, [load])

  if (error) return <Card className="p-6 text-red-600">{error}</Card>
  if (!purchase) return <Spinner label="Cargando compra..." />

  const writable = can('PURCHASES_WRITE')

  async function confirm() {
    setBusy(true)
    try {
      await api.post(`/api/purchases/${purchase!.id}/confirm`)
      toast.success('Compra confirmada. Stock actualizado.')
      await load()
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  async function cancel() {
    setBusy(true)
    try {
      await api.post(`/api/purchases/${purchase!.id}/cancel`, { reason: cancelReason.trim() || 'Sin motivo' })
      toast.success('Compra cancelada.')
      setCancelOpen(false)
      await load()
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  async function doReturn() {
    const items = Object.entries(returnQty)
      .filter(([, v]) => Number(v) > 0)
      .map(([productId, v]) => ({ productId: Number(productId), quantity: Number(v) }))
    if (items.length === 0 || !returnReason.trim()) {
      toast.error('Indique cantidad y motivo.')
      return
    }
    setBusy(true)
    try {
      await api.post(`/api/purchases/${purchase!.id}/returns`, { reason: returnReason.trim(), items })
      toast.success('Devolucion registrada.')
      setReturnOpen(false)
      setReturnQty({})
      setReturnReason('')
      await load()
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <PageHeader
        title={`Compra ${purchase.number}`}
        subtitle={`${purchase.supplierName} · ${date(purchase.purchaseDate)}`}
        actions={
          <>
            <Link to="/compras" className="btn-secondary">
              Volver
            </Link>
            {purchase.status === 'DRAFT' && writable && (
              <Button onClick={confirm} disabled={busy}>
                Confirmar (ingresar stock)
              </Button>
            )}
            {purchase.status === 'CONFIRMED' && writable && (
              <Button variant="secondary" onClick={() => setReturnOpen(true)}>
                Registrar devolucion
              </Button>
            )}
            {purchase.status === 'DRAFT' && writable && (
              <Button variant="danger" onClick={() => setCancelOpen(true)}>
                Cancelar compra
              </Button>
            )}
          </>
        }
      />

      <Card className="p-4">
        <div className="flex flex-wrap items-center gap-3 mb-4">
          <Badge tone={purchase.status === 'CONFIRMED' ? 'success' : purchase.status === 'CANCELLED' ? 'danger' : 'default'}>
            {purchase.status === 'CONFIRMED' ? 'Confirmada' : purchase.status === 'CANCELLED' ? 'Cancelada' : 'Borrador'}
          </Badge>
          <span className="text-sm text-muted font-mono">{purchase.supplierIdentification ?? ''}</span>
          {purchase.confirmedAt && (
            <span className="text-sm text-muted">Confirmada: {dateTime(purchase.confirmedAt)}</span>
          )}
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="th">Producto</th>
                <th className="th text-right">Cant.</th>
                <th className="th text-right">Costo</th>
                <th className="th text-right">Dto</th>
                <th className="th text-right">IVA</th>
                <th className="th text-right">Total</th>
              </tr>
            </thead>
            <tbody>
              {purchase.items.map((i) => (
                <tr key={i.id}>
                  <td className="td">
                    <p className="font-medium">{i.productName}</p>
                    <p className="text-xs text-muted font-mono">{i.sku}</p>
                  </td>
                  <td className="td text-right tabular-nums">{i.quantity}</td>
                  <td className="td text-right tabular-nums">${money(i.unitCost)}</td>
                  <td className="td text-right tabular-nums">{Number(i.discountPercent) > 0 ? `${i.discountPercent}%` : '—'}</td>
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
              <span className="tabular-nums">${money(purchase.subtotal)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted">Descuentos</span>
              <span className="tabular-nums">-${money(purchase.discountTotal)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted">IVA</span>
              <span className="tabular-nums">${money(purchase.taxTotal)}</span>
            </div>
            <div className="flex justify-between font-bold border-t border-border pt-1.5">
              <span>Total</span>
              <span className="tabular-nums">${money(purchase.total)}</span>
            </div>
          </div>
        </div>

        {purchase.notes && <p className="text-xs text-muted mt-3">Notas: {purchase.notes}</p>}
      </Card>

      <Modal open={cancelOpen} title="Cancelar compra" onClose={() => setCancelOpen(false)}>
        <Field label="Motivo">
          <Input value={cancelReason} onChange={(e) => setCancelReason(e.target.value)} autoFocus />
        </Field>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setCancelOpen(false)}>
            Volver
          </Button>
          <Button variant="danger" disabled={busy} onClick={cancel}>
            Cancelar compra
          </Button>
        </div>
      </Modal>

      <Modal open={returnOpen} title="Devolucion de mercaderia" onClose={() => setReturnOpen(false)} wide>
        <p className="text-sm text-muted mb-3">
          La devolucion reduce el stock y crea un comprobante de devolucion vinculado.
        </p>
        <div className="space-y-2">
          {purchase.items.map((i) => (
            <div key={i.id} className="flex items-center gap-3 text-sm">
              <span className="flex-1">
                {i.productName} <span className="text-muted font-mono text-xs">({i.sku}) · comprada {i.quantity}</span>
              </span>
              <Input
                type="number"
                min="0"
                step="0.001"
                max={Number(i.quantity)}
                className="!py-1 w-24"
                placeholder="0"
                value={returnQty[i.productId] ?? ''}
                onChange={(e) => setReturnQty((prev) => ({ ...prev, [i.productId]: e.target.value }))}
              />
            </div>
          ))}
        </div>
        <div className="mt-3">
          <Field label="Motivo" required>
            <Input value={returnReason} onChange={(e) => setReturnReason(e.target.value)} />
          </Field>
        </div>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setReturnOpen(false)}>
            Volver
          </Button>
          <Button disabled={busy} onClick={doReturn}>
            Registrar devolucion
          </Button>
        </div>
      </Modal>
    </div>
  )
}
