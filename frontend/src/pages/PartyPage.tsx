import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { useAuth } from '@/auth/AuthContext'
import { api, ApiError } from '@/lib/api'
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  Pagination,
  Select,
  Spinner,
  Textarea,
} from '@/components/ui'
import { messageOf } from '@/lib/format'
import type { IdType, Page as PageT } from '@/types'

interface Party {
  id: number
  idType: IdType
  identification: string
  businessName: string
  address: string | null
  phone: string | null
  email: string | null
  notes: string | null
  active: boolean
  createdAt: string
}

interface FormState {
  idType: IdType
  identification: string
  businessName: string
  address: string
  phone: string
  email: string
  notes: string
  active: boolean
}

const empty: FormState = {
  idType: 'CEDULA',
  identification: '',
  businessName: '',
  address: '',
  phone: '',
  email: '',
  notes: '',
  active: true,
}

export default function PartyPage({
  base,
  singular,
  plural,
  writePerm,
}: {
  base: string
  singular: string
  plural: string
  writePerm: string
}) {
  const { can } = useAuth()
  const [data, setData] = useState<PageT<Party> | null>(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState<FormState>(empty)
  const [editing, setEditing] = useState<Party | null>(null)
  const [open, setOpen] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [toDelete, setToDelete] = useState<Party | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setData(await api.get<PageT<Party>>(`${base}?search=${encodeURIComponent(search)}&page=${page}&size=20`))
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setLoading(false)
    }
  }, [base, search, page])

  useEffect(() => {
    const t = setTimeout(() => void load(), 250)
    return () => clearTimeout(t)
  }, [load])

  function openCreate() {
    setEditing(null)
    setForm(empty)
    setErrors({})
    setOpen(true)
  }

  function openEdit(p: Party) {
    setEditing(p)
    setForm({
      idType: p.idType,
      identification: p.identification,
      businessName: p.businessName,
      address: p.address ?? '',
      phone: p.phone ?? '',
      email: p.email ?? '',
      notes: p.notes ?? '',
      active: p.active,
    })
    setErrors({})
    setOpen(true)
  }

  async function save() {
    setErrors({})
    const body = {
      idType: form.idType,
      identification: form.identification.trim(),
      businessName: form.businessName.trim(),
      address: form.address.trim() || null,
      phone: form.phone.trim() || null,
      email: form.email.trim() || null,
      notes: form.notes.trim() || null,
      active: form.active,
    }
    try {
      if (editing) {
        await api.put(`${base}/${editing.id}`, body)
        toast.success(`${singular} actualizado.`)
      } else {
        await api.post(base, body)
        toast.success(`${singular} creado.`)
      }
      setOpen(false)
      await load()
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors) setErrors(e.fieldErrors)
      toast.error(messageOf(e))
    }
  }

  async function doDelete() {
    if (!toDelete) return
    try {
      await api.put(`${base}/${toDelete.id}`, { ...toDelete, active: false })
      toast.success(`${singular} desactivado.`)
      setToDelete(null)
      await load()
    } catch (e) {
      toast.error(messageOf(e))
    }
  }

  const writable = can(writePerm)

  return (
    <div>
      <PageHeader
        title={plural}
        subtitle={`Gestion de ${plural.toLowerCase()}`}
        actions={writable ? <Button onClick={openCreate}>Nuevo {singular.toLowerCase()}</Button> : undefined}
      />

      <Card>
        <div className="p-3 flex flex-wrap gap-2 items-center border-b border-border">
          <Input
            placeholder="Buscar por identificacion o nombre..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(0)
            }}
            className="max-w-sm"
          />
          {data && <span className="text-xs text-muted">{data.totalElements} resultado(s)</span>}
        </div>

        {loading && !data ? (
          <Spinner />
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            title={`No se encontraron ${plural.toLowerCase()}`}
            action={writable ? <Button onClick={openCreate}>Nuevo {singular.toLowerCase()}</Button> : undefined}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr>
                  <th className="th">Identificacion</th>
                  <th className="th">Nombre / Razon social</th>
                  <th className="th">Telefono</th>
                  <th className="th">Correo</th>
                  <th className="th">Estado</th>
                  <th className="th text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id} className="hover:bg-background/60">
                    <td className="td whitespace-nowrap">
                      <span className="text-[10px] text-muted block">{p.idType}</span>
                      <span className="font-mono text-xs">{p.identification}</span>
                    </td>
                    <td className="td font-medium">{p.businessName}</td>
                    <td className="td">{p.phone ?? '—'}</td>
                    <td className="td text-muted">{p.email ?? '—'}</td>
                    <td className="td">
                      {p.active ? <Badge tone="success">Activo</Badge> : <Badge tone="danger">Inactivo</Badge>}
                    </td>
                    <td className="td text-right whitespace-nowrap">
                      {writable && (
                        <>
                          <Button variant="ghost" onClick={() => openEdit(p)}>
                            Editar
                          </Button>
                          {p.active && (
                            <Button variant="ghost" className="!text-red-600" onClick={() => setToDelete(p)}>
                              Desactivar
                            </Button>
                          )}
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onPage={setPage} />
          </div>
        )}
      </Card>

      <Modal
        open={open}
        title={editing ? `Editar ${singular.toLowerCase()}` : `Nuevo ${singular.toLowerCase()}`}
        onClose={() => setOpen(false)}
        wide
      >
        <div className="grid sm:grid-cols-2 gap-3">
          <Field label="Tipo de identificacion" required error={errors.idType}>
            <Select value={form.idType} onChange={(e) => setForm({ ...form, idType: e.target.value as IdType })}>
              <option value="RUC">RUC</option>
              <option value="CEDULA">Cedula</option>
              <option value="PASAPORTE">Pasaporte</option>
              <option value="IDENTIFICACION_EXTRANJERA">Identificacion extranjera</option>
            </Select>
          </Field>
          <Field label="Identificacion" required error={errors.identification}>
            <Input value={form.identification} onChange={(e) => setForm({ ...form, identification: e.target.value })} />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Nombre o razon social" required error={errors.businessName}>
              <Input value={form.businessName} onChange={(e) => setForm({ ...form, businessName: e.target.value })} />
            </Field>
          </div>
          <Field label="Telefono">
            <Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </Field>
          <Field label="Correo">
            <Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Direccion">
              <Input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
            </Field>
          </div>
          <div className="sm:col-span-2">
            <Field label="Notas">
              <Textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
            </Field>
          </div>
          <label className="flex items-center gap-2 text-sm sm:col-span-2">
            <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
            Activo
          </label>
        </div>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setOpen(false)}>
            Cancelar
          </Button>
          <Button onClick={save}>Guardar</Button>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!toDelete}
        title={`Desactivar ${singular.toLowerCase()}`}
        message={`Se desactivara "${toDelete?.businessName}". El registro se conserva por trazabilidad.`}
        confirmLabel="Desactivar"
        danger
        onConfirm={doDelete}
        onClose={() => setToDelete(null)}
      />
    </div>
  )
}
