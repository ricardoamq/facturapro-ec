import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { Button, Card, EmptyState, PageHeader, Spinner } from '@/components/ui'
import { date, messageOf, money } from '@/lib/format'
import type { ReceivableRow } from '@/types'

export default function ReceivablesPage() {
  const [rows, setRows] = useState<ReceivableRow[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<ReceivableRow[]>('/api/reports/receivables')
      .then(setRows)
      .catch((e) => setError(messageOf(e)))
  }, [])

  if (error) return <Card className="p-6 text-red-600">{error}</Card>
  if (!rows) return <Spinner label="Cargando cuentas por cobrar..." />

  const total = rows.reduce((acc, r) => acc + Number(r.balance), 0)

  return (
    <div>
      <PageHeader
        title="Cuentas por cobrar"
        subtitle="Ventas emitidas con saldo pendiente"
        actions={
          rows.length > 0 ? (
            <a
              className="btn-secondary"
              href={`/api/reports/sales/export?from=2000-01-01&to=${new Date().toISOString().slice(0, 10)}`}
            >
              Exportar CSV de ventas
            </a>
          ) : undefined
        }
      />

      <Card>
        {rows.length === 0 ? (
          <EmptyState title="No hay saldos pendientes" description="Todas las ventas emitidas estan cobradas." />
        ) : (
          <>
            <div className="p-3 border-b border-border flex justify-between text-sm">
              <span className="text-muted">{rows.length} venta(s) con saldo</span>
              <span className="font-bold tabular-nums">
                Total por cobrar: ${money(total)}
              </span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr>
                    <th className="th">Numero</th>
                    <th className="th">Fecha</th>
                    <th className="th">Cliente</th>
                    <th className="th text-right">Total</th>
                    <th className="th text-right">Pagado</th>
                    <th className="th text-right">Saldo</th>
                    <th className="th text-right">Dias abierto</th>
                    <th className="th" />
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.saleId} className="hover:bg-background/60">
                      <td className="td font-mono text-xs font-semibold">{r.number}</td>
                      <td className="td whitespace-nowrap">{date(r.saleDate)}</td>
                      <td className="td">
                        <p className="font-medium">{r.customerName ?? 'Consumidor final'}</p>
                        <p className="text-xs text-muted font-mono">{r.customerIdentification ?? ''}</p>
                      </td>
                      <td className="td text-right tabular-nums">${money(r.total)}</td>
                      <td className="td text-right tabular-nums">${money(r.amountPaid)}</td>
                      <td className="td text-right tabular-nums font-semibold text-red-600">${money(r.balance)}</td>
                      <td className="td text-right tabular-nums">{r.daysOpen}</td>
                      <td className="td text-right">
                        <Link to={`/ventas/${r.saleId}`} className="text-brand text-xs hover:underline">
                          Ver / cobrar
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </Card>
      <Button
        variant="ghost"
        className="mt-3"
        onClick={() => {
          api
            .get<ReceivableRow[]>('/api/reports/receivables')
            .then(setRows)
            .catch((e) => toast.error(messageOf(e)))
        }}
      >
        Actualizar
      </Button>
    </div>
  )
}
