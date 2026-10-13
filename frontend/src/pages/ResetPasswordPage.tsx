import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { Button, Field, Input } from '@/components/ui'
import { fieldErrorsOf, messageOf } from '@/lib/format'

export default function ResetPasswordPage() {
  const navigate = useNavigate()
  const [token, setToken] = useState(() => sessionStorage.getItem('resetToken') ?? '')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (password !== confirm) {
      setError('Las contrasenas no coinciden.')
      return
    }
    setLoading(true)
    try {
      await api.post('/api/auth/reset-password', { token: token.trim(), newPassword: password })
      sessionStorage.removeItem('resetToken')
      toast.success('Contrasena restablecida. Inicie sesion.')
      navigate('/login', { replace: true })
    } catch (err) {
      setError(messageOf(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <form onSubmit={onSubmit} className="w-full max-w-md card p-6 space-y-4">
        <h1 className="text-lg font-semibold">Restablecer contrasena</h1>
        {error && <div className="rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm px-3 py-2">{error}</div>}
        <Field label="Token" required>
          <Input value={token} onChange={(e) => setToken(e.target.value)} required />
        </Field>
        <Field label="Nueva contrasena" required error={fieldErrorsOf(null).newPassword}>
          <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
        </Field>
        <Field label="Confirmar contrasena" required>
          <Input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required minLength={8} />
        </Field>
        <Button type="submit" disabled={loading} className="w-full">
          {loading ? 'Guardando...' : 'Restablecer'}
        </Button>
        <Link to="/login" className="block text-center text-xs text-brand hover:underline">
          Volver al inicio de sesion
        </Link>
      </form>
    </div>
  )
}
