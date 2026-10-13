import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '@/lib/api'
import type { Session } from '@/types'

interface AuthContextValue {
  session: Session | null
  loading: boolean
  login: (username: string, password: string) => Promise<Session>
  logout: () => Promise<void>
  refresh: () => Promise<void>
  can: (permission: string) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    try {
      const me = await api.get<Session>('/api/auth/me')
      setSession(me)
    } catch {
      setSession(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    const handler = () => setSession(null)
    window.addEventListener('auth:unauthorized', handler)
    return () => window.removeEventListener('auth:unauthorized', handler)
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const data = await api.post<Session>('/api/auth/login', { username, password })
    setSession(data)
    return data
  }, [])

  const logout = useCallback(async () => {
    try {
      await api.post('/api/auth/logout')
    } finally {
      setSession(null)
    }
  }, [])

  const can = useCallback(
    (permission: string) => !!session?.permissions.includes(permission),
    [session],
  )

  const value = useMemo(
    () => ({ session, loading, login, logout, refresh, can }),
    [session, loading, login, logout, refresh, can],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
