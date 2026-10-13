import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import { useAuth } from '@/auth/AuthContext'
import { api } from '@/lib/api'
import { Button, Card, Field, Input, PageHeader, Spinner, Tabs } from '@/components/ui'
import { messageOf } from '@/lib/format'
import type { TaxCategory } from '@/types'

const SETTING_GROUPS: { id: string; label: string; keys: string[] }[] = [
  {
    id: 'company',
    label: 'Empresa',
    keys: ['company.name', 'company.ruc', 'company.address', 'company.phone', 'company.email', 'company.logo'],
  },
  {
    id: 'invoice',
    label: 'Comprobantes',
    keys: [
      'invoice.series',
      'invoice.nextNumber',
      'invoice.numberFormat',
      'invoice.footerText',
      'invoice.electronicEnabled',
      'invoice.sriEnvironment',
    ],
  },
  {
    id: 'tax',
    label: 'Impuestos',
    keys: ['tax.generalRate', 'tax.reducedRate', 'tax.exemptRate', 'tax.decimals'],
  },
  { id: 'inventory', label: 'Inventario', keys: ['inventory.allowNegativeStock', 'inventory.lowStockAlert'] },
  { id: 'sales', label: 'Ventas', keys: ['sales.defaultPaymentMethod', 'sales.allowDraftSales'] },
  { id: 'session', label: 'Sesion', keys: ['security.sessionTimeoutMinutes', 'security.passwordMinLength'] },
]

const LABELS: Record<string, string> = {
  'company.name': 'Nombre de la empresa',
  'company.ruc': 'RUC',
  'company.address': 'Direccion',
  'company.phone': 'Telefono',
  'company.email': 'Correo',
  'company.logo': 'URL del logo',
  'invoice.series': 'Serie',
  'invoice.nextNumber': 'Siguiente numero',
  'invoice.numberFormat': 'Formato de numero (ej. FAC-{YYYY}-{SEQ})',
  'invoice.footerText': 'Texto del pie de pagina',
  'invoice.electronicEnabled': 'Facturacion electronica habilitada',
  'invoice.sriEnvironment': 'Entorno SRI (1=pruebas, 2=produccion)',
  'tax.generalRate': 'Tarifa general (%)',
  'tax.reducedRate': 'Tarifa reducida (%)',
  'tax.exemptRate': 'Tarifa exenta (%)',
  'tax.decimals': 'Decimales de dinero',
  'inventory.allowNegativeStock': 'Permitir stock negativo',
  'inventory.lowStockAlert': 'Alerta de stock bajo (dias)',
  'sales.defaultPaymentMethod': 'Metodo de pago por defecto',
  'sales.allowDraftSales': 'Permitir ventas en borrador',
  'security.sessionTimeoutMinutes': 'Minutos de expiracion de sesion',
  'security.passwordMinLength': 'Longitud minima de contrasena',
}

export default function SettingsPage() {
  const { can } = useAuth()
  const [tab, setTab] = useState('company')
  const [settings, setSettings] = useState<Record<string, string> | null>(null)
  const [taxCategories, setTaxCategories] = useState<TaxCategory[]>([])
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.get<Record<string, string>>('/api/settings').then(setSettings).catch((e) => toast.error(messageOf(e)))
    api
      .get<TaxCategory[]>('/api/settings/tax-categories')
      .then(setTaxCategories)
      .catch(() => setTaxCategories([]))
  }, [])

  function update(key: string, value: string) {
    setSettings((prev) => (prev ? { ...prev, [key]: value } : prev))
  }

  async function save() {
    if (!settings) return
    setBusy(true)
    try {
      const updated = await api.put<Record<string, string>>('/api/settings', { values: settings })
      setSettings(updated)
      toast.success('Configuracion guardada.')
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  if (!settings) return <Spinner label="Cargando configuracion..." />

  const group = SETTING_GROUPS.find((g) => g.id === tab) ?? SETTING_GROUPS[0]

  return (
    <div className="max-w-3xl">
      <PageHeader
        title="Configuracion"
        subtitle="Parametros generales del sistema"
        actions={
          can('SETTINGS_WRITE') ? (
            <Button onClick={save} disabled={busy}>
              {busy ? 'Guardando...' : 'Guardar cambios'}
            </Button>
          ) : undefined
        }
      />

      <Tabs active={tab} onChange={setTab} tabs={SETTING_GROUPS.map((g) => ({ id: g.id, label: g.label }))} />

      <Card className="p-5 space-y-4">
        {group.keys.map((key) => (
          <Field key={key} label={LABELS[key] ?? key}>
            <Input
              value={settings[key] ?? ''}
              onChange={(e) => update(key, e.target.value)}
              disabled={!can('SETTINGS_WRITE')}
              type={
                key.includes('Rate') || key.includes('Number') || key.includes('Length') || key.includes('Timeout') || key.includes('decimals')
                  ? 'number'
                  : 'text'
              }
              step={key.includes('Rate') ? '0.01' : undefined}
            />
          </Field>
        ))}

        {tab === 'tax' && (
          <div className="border-t border-border pt-4">
            <p className="label">Categorias fiscales configuradas</p>
            <table className="w-full text-sm">
              <thead>
                <tr>
                  <th className="th">Codigo</th>
                  <th className="th">Nombre</th>
                  <th className="th text-right">Tarifa</th>
                </tr>
              </thead>
              <tbody>
                {taxCategories.map((t) => (
                  <tr key={t.code}>
                    <td className="td font-mono text-xs">{t.code}</td>
                    <td className="td">{t.name}</td>
                    <td className="td text-right tabular-nums">{t.rate}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="text-xs text-muted mt-2">
              Tarifas documentadas: 15% general, 5% construccion, 0% exenta (ver ficha de implementacion).
            </p>
          </div>
        )}

        {tab === 'invoice' && (
          <div className="border-t border-border pt-4 text-xs text-muted space-y-1">
            <p>Los comprobantes generados por este sistema son internos.</p>
            <p>Solo se marcara como autorizado cuando exista firma electronica habilitada.</p>
          </div>
        )}
      </Card>

      {!can('SETTINGS_WRITE') && (
        <p className="text-xs text-muted mt-3">Su rol solo puede consultar la configuracion.</p>
      )}
    </div>
  )
}
