import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { useAuth } from '@/auth/AuthContext'
import { api } from '@/lib/api'
import { Button, Field, Input } from '@/components/ui'
import { messageOf } from '@/lib/format'

export default function ChangePasswordPage() {
  const { session, refresh, logout } = useAuth()
  const navigate = useNavigate()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (next !== confirm) {
      setError('Las contrasenas no coinciden.')
      return
    }
    setLoading(true)
    try {
      await api.post('/api/users/me/change-password', { currentPassword: current, newPassword: next })
      toast.success('Contrasena actualizada.')
      if (session?.mustChangePassword) {
        await refresh()
        navigate('/', { replace: true })
      } else {
        setCurrent('')
        setNext('')
        setConfirm('')
      }
    } catch (err) {
      setError(messageOf(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <form onSubmit={onSubmit} className="w-full max-w-md card p-6 space-y-4">
        <div>
          <h1 className="text-lg font-semibold">Cambiar contrasena</h1>
          {session?.mustChangePassword && (
            <p className="text-sm text-amber-600 mt-1">
              Debe cambiar su contrasena temporal antes de continuar usando el sistema.
            </p>
          )}
        </div>
        {error && <div className="rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm px-3 py-2">{error}</div>}
        <Field label="Contrasena actual" required>
          <Input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} required autoComplete="current-password" />
        </Field>
        <Field label="Nueva contrasena" required>
          <Input type="password" value={next} onChange={(e) => setNext(e.target.value)} required minLength={8} autoComplete="new-password" />
        </Field>
        <Field label="Confirmar nueva contrasena" required>
          <Input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required minLength={8} autoComplete="new-password" />
        </Field>
        <Button type="submit" disabled={loading} className="w-full">
          {loading ? 'Guardando...' : 'Guardar contrasena'}
        </Button>
        {!session?.mustChangePassword && (
          <button
            type="button"
            className="w-full text-xs text-muted hover:underline"
            onClick={async () => {
              await logout()
              navigate('/login')
            }}
          >
            Salir sin cambios
          </button>
        )}
      </form>
    </div>
  )
}
