import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { Badge, Card, EmptyState, Input, PageHeader, Pagination, Select, Spinner } from '@/components/ui'
import { date, dateTime, money, paymentStatusLabel, saleStatusLabel } from '@/lib/format'
import type { Page as PageT, Sale } from '@/types'

const STATUS_TONE: Record<string, 'default' | 'success' | 'warning' | 'danger'> = {
  DRAFT: 'default',
  ISSUED: 'success',
  VOIDED: 'danger',
}
const PAY_TONE: Record<string, 'default' | 'warning' | 'success'> = {
  PENDING: 'warning',
  PARTIAL: 'default',
  PAID: 'success',
}

export default function SalesPage() {
  const navigate = useNavigate()
  const [data, setData] = useState<PageT<Sale> | null>(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [paymentStatus, setPaymentStatus] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    const params = new URLSearchParams({ page: String(page), size: '20' })
    if (search) params.set('search', search)
    if (status) params.set('status', status)
    if (paymentStatus) params.set('paymentStatus', paymentStatus)
    try {
      setData(await api.get<PageT<Sale>>(`/api/sales?${params}`))
    } catch (e) {
      toast.error((e as Error).message)
    } finally {
      setLoading(false)
    }
  }, [search, status, paymentStatus, page])

  useEffect(() => {
    const t = setTimeout(() => void load(), 250)
    return () => clearTimeout(t)
  }, [load])

  return (
    <div>
      <PageHeader
        title="Ventas"
        subtitle="Comprobantes internos emitidos"
        actions={
          <Link to="/pos" className="btn-primary">
            Nueva venta
          </Link>
        }
      />

      <Card>
        <div className="p-3 flex flex-wrap gap-2 border-b border-border">
          <Input
            placeholder="Buscar numero o cliente..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(0)
            }}
            className="max-w-xs"
          />
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
            <option value="ISSUED">Emitido</option>
            <option value="VOIDED">Anulado</option>
          </Select>
          <Select
            value={paymentStatus}
            onChange={(e) => {
              setPaymentStatus(e.target.value)
              setPage(0)
            }}
            className="w-auto"
          >
            <option value="">Todos los pagos</option>
            <option value="PENDING">Pendiente</option>
            <option value="PARTIAL">Parcial</option>
            <option value="PAID">Pagado</option>
          </Select>
        </div>

        {loading && !data ? (
          <Spinner />
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            title="No hay ventas"
            description="Registre su primera venta desde el punto de venta."
            action={
              <Link to="/pos" className="btn-primary">
                Nueva venta
              </Link>
            }
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr>
                  <th className="th">Numero</th>
                  <th className="th">Fecha</th>
                  <th className="th">Cliente</th>
                  <th className="th">Estado</th>
                  <th className="th">Pago</th>
                  <th className="th text-right">Total</th>
                  <th className="th text-right">Saldo</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((s) => (
                  <tr
                    key={s.id}
                    className="hover:bg-background/60 cursor-pointer"
                    onClick={() => navigate(`/ventas/${s.id}`)}
                  >
                    <td className="td font-mono text-xs font-semibold">{s.number ?? `BORRADOR #${s.id}`}</td>
                    <td className="td whitespace-nowrap">{date(s.saleDate)}</td>
                    <td className="td">
                      <p className="font-medium">{s.customerName ?? 'Consumidor final'}</p>
                      <p className="text-xs text-muted font-mono">{s.customerIdentification ?? ''}</p>
                    </td>
                    <td className="td">
                      <Badge tone={STATUS_TONE[s.status] ?? 'default'}>{saleStatusLabel(s.status)}</Badge>
                    </td>
                    <td className="td">
                      <Badge tone={PAY_TONE[s.paymentStatus] ?? 'default'}>
                        {paymentStatusLabel(s.paymentStatus)}
                      </Badge>
                    </td>
                    <td className="td text-right tabular-nums font-semibold">${money(s.total)}</td>
                    <td className={`td text-right tabular-nums ${Number(s.balance) > 0 ? 'text-red-600' : 'text-muted'}`}>
                      ${money(s.balance)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onPage={setPage} />
          </div>
        )}
      </Card>
      <p className="text-xs text-muted mt-3">Ultima sincronizacion: {dateTime(new Date().toISOString())}</p>
    </div>
  )
}
