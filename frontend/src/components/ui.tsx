import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { cn, money } from '@/lib/format'

export function Button({
  variant = 'primary',
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger' | 'ghost' }) {
  const variants = {
    primary: 'btn-primary',
    secondary: 'btn-secondary',
    danger: 'btn-danger',
    ghost: 'btn text-muted hover:bg-background',
  }
  return <button className={cn(variants[variant], className)} {...props} />
}

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn('input', className)} {...props} />
}

export function Textarea({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cn('input min-h-20', className)} {...props} />
}

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={cn('input', className)} {...props}>
      {children}
    </select>
  )
}

export function Field({
  label,
  error,
  children,
  required,
}: {
  label: string
  error?: string
  children: ReactNode
  required?: boolean
}) {
  return (
    <div>
      <label className="label">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      {children}
      {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
    </div>
  )
}

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('card', className)}>{children}</div>
}

export function Badge({ tone = 'default', children }: { tone?: 'default' | 'success' | 'warning' | 'danger' | 'info'; children: ReactNode }) {
  const tones = {
    default: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
    success: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300',
    warning: 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300',
    danger: 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300',
    info: 'bg-teal-100 text-teal-800 dark:bg-teal-900/40 dark:text-teal-300',
  }
  return (
    <span className={cn('inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold', tones[tone])}>
      {children}
    </span>
  )
}

export function StatCard({
  title,
  value,
  hint,
  icon,
  tone = 'default',
}: {
  title: string
  value: string
  hint?: string
  icon?: ReactNode
  tone?: 'default' | 'success' | 'warning' | 'danger'
}) {
  const tones = {
    default: 'text-foreground',
    success: 'text-emerald-600',
    warning: 'text-amber-600',
    danger: 'text-red-600',
  }
  return (
    <Card className="p-4 flex items-start justify-between gap-3">
      <div>
        <p className="text-[11px] font-bold uppercase tracking-wide text-muted">{title}</p>
        <p className={cn('text-2xl font-bold mt-1 tabular-nums', tones[tone])}>{value}</p>
        {hint && <p className="text-xs text-muted mt-1">{hint}</p>}
      </div>
      {icon && <div className="text-brand/70">{icon}</div>}
    </Card>
  )
}

export function EmptyState({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="text-center py-14 px-6">
      <p className="font-semibold">{title}</p>
      {description && <p className="text-sm text-muted mt-1 max-w-md mx-auto">{description}</p>}
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  )
}

export function Spinner({ label = 'Cargando...' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-10 text-sm text-muted">
      <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-brand border-t-transparent" />
      {label}
    </div>
  )
}

export function Modal({
  open,
  title,
  onClose,
  children,
  wide,
}: {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  wide?: boolean
}) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />
      <div className={cn('relative card w-full max-h-[90vh] overflow-y-auto p-5', wide ? 'max-w-3xl' : 'max-w-lg')}>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button onClick={onClose} className="text-muted hover:text-foreground text-xl leading-none" aria-label="Cerrar">
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirmar',
  onConfirm,
  onClose,
  danger,
}: {
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  onConfirm: () => void
  onClose: () => void
  danger?: boolean
}) {
  return (
    <Modal open={open} title={title} onClose={onClose}>
      <p className="text-sm text-muted">{message}</p>
      <div className="flex justify-end gap-2 mt-6">
        <Button variant="secondary" onClick={onClose}>
          Cancelar
        </Button>
        <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  )
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onPage,
}: {
  page: number
  totalPages: number
  totalElements: number
  onPage: (p: number) => void
}) {
  if (totalElements === 0) return null
  return (
    <div className="flex items-center justify-between px-3 py-2 border-t border-border text-sm">
      <span className="text-muted">
        {totalElements} registro{totalElements === 1 ? '' : 's'} · pagina {page + 1} de {Math.max(totalPages, 1)}
      </span>
      <div className="flex gap-2">
        <Button variant="secondary" disabled={page <= 0} onClick={() => onPage(page - 1)}>
          Anterior
        </Button>
        <Button variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
          Siguiente
        </Button>
      </div>
    </div>
  )
}

export function Money({ value, className }: { value: number | string | null | undefined; className?: string }) {
  return <span className={cn('tabular-nums', className)}>${money(value)}</span>
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
      <div>
        <h1 className="text-xl font-bold">{title}</h1>
        {subtitle && <p className="text-sm text-muted mt-0.5">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

export function Tabs({
  tabs,
  active,
  onChange,
}: {
  tabs: { id: string; label: string }[]
  active: string
  onChange: (id: string) => void
}) {
  return (
    <div className="flex gap-1 border-b border-border mb-4 overflow-x-auto">
      {tabs.map((t) => (
        <button
          key={t.id}
          onClick={() => onChange(t.id)}
          className={cn(
            'px-3 py-2 text-sm font-medium border-b-2 -mb-px whitespace-nowrap cursor-pointer',
            active === t.id ? 'border-brand text-brand' : 'border-transparent text-muted hover:text-foreground',
          )}
        >
          {t.label}
        </button>
      ))}
    </div>
  )
}
