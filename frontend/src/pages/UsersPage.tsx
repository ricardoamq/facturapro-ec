import { useCallback, useEffect, useState } from 'react'
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
import { dateTime, messageOf } from '@/lib/format'
import type { AppUser, Page as PageT } from '@/types'

const ROLES = ['ADMINISTRADOR', 'GERENTE', 'VENDEDOR', 'BODEGA']

export default function UsersPage() {
  const { can } = useAuth()
  const [data, setData] = useState<PageT<AppUser> | null>(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<AppUser | null>(null)
  const [form, setForm] = useState({
    username: '',
    fullName: '',
    email: '',
    phone: '',
    role: 'VENDEDOR',
    password: '',
    active: true,
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [resetUser, setResetUser] = useState<AppUser | null>(null)
  const [newPassword, setNewPassword] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setData(await api.get<PageT<AppUser>>(`/api/users?search=${encodeURIComponent(search)}&page=${page}&size=20`))
    } catch (e) {
      toast.error(messageOf(e))
    } finally {
      setLoading(false)
    }
  }, [search, page])

  useEffect(() => {
    const t = setTimeout(() => void load(), 250)
    return () => clearTimeout(t)
  }, [load])

  function openCreate() {
    setEditing(null)
    setForm({ username: '', fullName: '', email: '', phone: '', role: 'VENDEDOR', password: '', active: true })
    setErrors({})
    setOpen(true)
  }

  function openEdit(u: AppUser) {
    setEditing(u)
    setForm({
      username: u.username,
      fullName: u.fullName,
      email: u.email ?? '',
      phone: u.phone ?? '',
      role: u.role,
      password: '',
      active: u.active,
    })
    setErrors({})
    setOpen(true)
  }

  async function save() {
    setErrors({})
    try {
      if (editing) {
        await api.put(`/api/users/${editing.id}`, {
          fullName: form.fullName.trim(),
          email: form.email.trim() || null,
          phone: form.phone.trim() || null,
          role: form.role,
          active: form.active,
        })
        toast.success('Usuario actualizado.')
      } else {
        await api.post('/api/users', {
          username: form.username.trim(),
          fullName: form.fullName.trim(),
          email: form.email.trim() || null,
          phone: form.phone.trim() || null,
          role: form.role,
          password: form.password,
        })
        toast.success('Usuario creado.')
      }
      setOpen(false)
      await load()
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors) setErrors(e.fieldErrors)
      toast.error(messageOf(e))
    }
  }

  async function doReset() {
    if (!resetUser || newPassword.length < 8) return
    try {
      await api.post(`/api/users/${resetUser.id}/reset-password`, {
        newPassword,
        mustChangePassword: true,
      })
      toast.success('Contrasena reiniciada.')
      setResetUser(null)
      setNewPassword('')
    } catch (e) {
      toast.error(messageOf(e))
    }
  }

  return (
    <div>
      <PageHeader
        title="Usuarios"
        subtitle="Cuentas, roles y permisos"
        actions={can('USERS_WRITE') ? <Button onClick={openCreate}>Nuevo usuario</Button> : undefined}
      />

      <Card>
        <div className="p-3 border-b border-border">
          <Input
            placeholder="Buscar usuario o nombre..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(0)
            }}
            className="max-w-sm"
          />
        </div>
        {loading && !data ? (
          <Spinner />
        ) : !data || data.content.length === 0 ? (
          <EmptyState title="Sin usuarios" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr>
                  <th className="th">Usuario</th>
                  <th className="th">Nombre</th>
                  <th className="th">Rol</th>
                  <th className="th">Estado</th>
                  <th className="th">Ultimo acceso</th>
                  <th className="th text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((u) => (
                  <tr key={u.id} className="hover:bg-background/60">
                    <td className="td font-mono text-xs font-semibold">{u.username}</td>
                    <td className="td font-medium">{u.fullName}</td>
                    <td className="td">
                      <Badge tone="info">{u.role}</Badge>
                    </td>
                    <td className="td">
                      {u.active ? <Badge tone="success">Activo</Badge> : <Badge tone="danger">Inactivo</Badge>}
                      {u.mustChangePassword && (
                        <Badge tone="warning"> debe cambiar clave</Badge>
                      )}
                    </td>
                    <td className="td text-muted text-xs">{dateTime(u.lastLoginAt)}</td>
                    <td className="td text-right whitespace-nowrap">
                      {can('USERS_WRITE') && (
                        <>
                          <Button variant="ghost" onClick={() => openEdit(u)}>
                            Editar
                          </Button>
                          <Button variant="ghost" onClick={() => setResetUser(u)}>
                            Reset clave
                          </Button>
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

      <Modal open={open} title={editing ? 'Editar usuario' : 'Nuevo usuario'} onClose={() => setOpen(false)}>
        <div className="space-y-3">
          {!editing && (
            <Field label="Usuario" required error={errors.username}>
              <Input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </Field>
          )}
          <Field label="Nombre completo" required error={errors.fullName}>
            <Input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
          </Field>
          <Field label="Correo" error={errors.email}>
            <Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </Field>
          <Field label="Telefono">
            <Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </Field>
          <Field label="Rol" required error={errors.role}>
            <Select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </Select>
          </Field>
          {!editing && (
            <Field label="Contrasena temporal" required error={errors.password}>
              <Input
                type="text"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                minLength={8}
              />
            </Field>
          )}
          {editing && (
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={form.active}
                onChange={(e) => setForm({ ...form, active: e.target.checked })}
              />
              Usuario activo
            </label>
          )}
        </div>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setOpen(false)}>
            Cancelar
          </Button>
          <Button onClick={save}>Guardar</Button>
        </div>
      </Modal>

      <Modal open={!!resetUser} title={`Resetear contrasena de ${resetUser?.username ?? ''}`} onClose={() => setResetUser(null)}>
        <Field label="Nueva contrasena" required>
          <Input type="text" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} minLength={8} autoFocus />
        </Field>
        <p className="text-xs text-muted mt-2">El usuario debera cambiarla en su proximo inicio de sesion.</p>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setResetUser(null)}>
            Cancelar
          </Button>
          <Button disabled={newPassword.length < 8} onClick={doReset}>
            Resetear
          </Button>
        </div>
      </Modal>
    </div>
  )
}
