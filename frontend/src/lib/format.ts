import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function money(value: number | string | null | undefined): string {
  const n = Number(value ?? 0)
  return n.toLocaleString('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function qty(value: number | string | null | undefined): string {
  const n = Number(value ?? 0)
  return n.toLocaleString('es-EC', { maximumFractionDigits: 3 })
}

export function date(value: string | null | undefined): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleDateString('es-EC', { timeZone: 'America/Guayaquil' })
}

export function dateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('es-EC', { timeZone: 'America/Guayaquil', dateStyle: 'short', timeStyle: 'short' })
}

export function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

export function monthStartIso(): string {
  const d = new Date()
  return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10)
}

export function daysAgoIso(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 10)
}

export function saleStatusLabel(status: string): string {
  return { DRAFT: 'Borrador', ISSUED: 'Emitido', VOIDED: 'Anulado' }[status] ?? status
}

export function paymentStatusLabel(status: string): string {
  return { PENDING: 'Pendiente', PARTIAL: 'Parcial', PAID: 'Pagado' }[status] ?? status
}

export function fieldErrorsOf(error: unknown): Record<string, string> {
  if (error && typeof error === 'object' && 'fieldErrors' in error) {
    return (error as { fieldErrors?: Record<string, string> }).fieldErrors ?? {}
  }
  return {}
}

export function messageOf(error: unknown): string {
  if (error instanceof Error) return error.message
  return 'Ocurrio un error inesperado.'
}
