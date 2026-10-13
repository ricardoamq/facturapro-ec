export class ApiError extends Error {
  status: number
  code: string
  fieldErrors?: Record<string, string>

  constructor(status: number, code: string, message: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

export function getCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1]) : null
}

export async function ensureCsrf(): Promise<string> {
  const existing = getCookie('XSRF-TOKEN')
  if (existing) return existing
  const res = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
  const data = await res.json()
  return data.token as string
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface RequestOptions {
  method?: Method
  body?: unknown
  headers?: Record<string, string>
  signal?: AbortSignal
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET'
  const headers: Record<string, string> = { Accept: 'application/json', ...options.headers }

  if (method !== 'GET') {
    const token = await ensureCsrf()
    headers['X-XSRF-TOKEN'] = token
  }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    signal: options.signal,
  })

  if (res.status === 204) return undefined as T

  const text = await res.text()
  let data: unknown = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = text
    }
  }

  if (!res.ok) {
    if (res.status === 401) {
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
    }
    const err = data as { code?: string; message?: string; fieldErrors?: Record<string, string> }
    throw new ApiError(
      res.status,
      err?.code ?? 'ERROR',
      err?.message ?? `Error ${res.status}`,
      err?.fieldErrors,
    )
  }
  return data as T
}

export const api = {
  get: <T>(path: string, signal?: AbortSignal) => request<T>(path, { signal }),
  post: <T>(path: string, body?: unknown, headers?: Record<string, string>) =>
    request<T>(path, { method: 'POST', body, headers }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  del: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}

export async function download(path: string, filename: string): Promise<void> {
  const res = await fetch(path, { credentials: 'same-origin' })
  if (!res.ok) {
    const text = await res.text()
    let message = `Error ${res.status}`
    try {
      message = JSON.parse(text).message ?? message
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, 'DOWNLOAD_ERROR', message)
  }
  const blob = await res.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}
