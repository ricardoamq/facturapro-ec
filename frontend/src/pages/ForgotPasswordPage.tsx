import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '@/lib/api'
import { Button, Field, Input } from '@/components/ui'
import { messageOf } from '@/lib/format'

export default function ForgotPasswordPage() {
  const [username, setUsername] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const res = await api.post<{ token: string | null; message: string }>('/api/auth/forgot-password', {
        username: username.trim(),
      })
      setSent(true)
      if (res?.token) {
        sessionStorage.setItem('resetToken', res.token)
      }
    } catch (err) {
      setError(messageOf(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <form onSubmit={onSubmit} className="card p-6 space-y-4">
          <h1 className="text-lg font-semibold">Recuperar contrasena</h1>
          {error && <div className="rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm px-3 py-2">{error}</div>}
          {sent ? (
            <div className="rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm px-3 py-2 space-y-2">
              <p>
                Si el usuario existe, se genero un token de restablecimiento. En este entorno de desarrollo el token
                se guarda automaticamente.
              </p>
              <Link to="/restablecer" className="font-semibold underline">
                Continuar al restablecimiento
              </Link>
            </div>
          ) : (
            <>
              <Field label="Usuario" required>
                <Input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
              </Field>
              <Button type="submit" disabled={loading} className="w-full">
                {loading ? 'Enviando...' : 'Generar token'}
              </Button>
            </>
          )}
          <Link to="/login" className="block text-center text-xs text-brand hover:underline">
            Volver al inicio de sesion
          </Link>
        </form>
      </div>
    </div>
  )
}
