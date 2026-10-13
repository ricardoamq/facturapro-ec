import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { useAuth } from '@/auth/AuthContext'
import { Button, Field, Input } from '@/components/ui'
import { messageOf } from '@/lib/format'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const session = await login(username.trim(), password)
      toast.success(`Bienvenido, ${session.fullName}`)
      navigate(session.mustChangePassword ? '/cambiar-password' : '/', { replace: true })
    } catch (err) {
      setError(messageOf(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="flex items-center gap-3 mb-6 justify-center">
          <div className="h-11 w-11 rounded-xl bg-brand text-white grid place-items-center font-bold">FP</div>
          <div>
            <h1 className="text-xl font-bold leading-none">FacturaPro EC</h1>
            <p className="text-xs text-muted mt-1">Facturacion e inventario</p>
          </div>
        </div>

        <form onSubmit={onSubmit} className="card p-6 space-y-4">
          <h2 className="text-lg font-semibold">Iniciar sesion</h2>
          {error && (
            <div className="rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm px-3 py-2" role="alert">
              {error}
            </div>
          )}
          <Field label="Usuario" required>
            <Input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              autoFocus
              required
            />
          </Field>
          <Field label="Contrasena" required>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </Field>
          <Button type="submit" disabled={loading} className="w-full">
            {loading ? 'Verificando...' : 'Entrar'}
          </Button>
          <div className="flex justify-between text-xs">
            <Link to="/recuperar" className="text-brand hover:underline">
              Olvide mi contrasena
            </Link>
          </div>
        </form>
        <p className="text-center text-xs text-muted mt-4">
          Sistema interno de comprobantes · no autorizado electronicamente por el SRI
        </p>
      </div>
    </div>
  )
}
