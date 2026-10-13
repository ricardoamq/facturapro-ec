export interface Session {
  id: number
  username: string
  fullName: string
  role: string
  mustChangePassword: boolean
  permissions: string[]
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface Category {
  id: number
  name: string
  description: string | null
  active: boolean
}

export interface Product {
  id: number
  sku: string
  name: string
  description: string | null
  categoryId: number | null
  categoryName: string | null
  purchasePrice: number
  salePrice: number
  margin: number
  marginPercent: number
  unit: string
  barcode: string | null
  stock: number
  minStock: number
  taxCategory: 'EXEMPT' | 'GENERAL' | 'REDUCED'
  taxRate: number
  lowStock: boolean
  active: boolean
  createdAt: string
}

export type IdType = 'RUC' | 'CEDULA' | 'PASAPORTE' | 'IDENTIFICACION_EXTRANJERA'

export interface Customer {
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

export interface Supplier extends Customer {}

export interface SaleItem {
  id: number
  productId: number | null
  sku: string
  productName: string
  quantity: number
  unitPrice: number
  discountPercent: number
  taxRate: number
  lineSubtotal: number
  lineDiscount: number
  lineTax: number
  lineTotal: number
  unitCost: number
  lineProfit: number
}

export interface Payment {
  id: number
  saleId: number
  saleNumber: string | null
  amount: number
  paymentMethod: string
  reference: string | null
  notes: string | null
  paymentDate: string
  voided: boolean
  voidReason: string | null
  voidedAt: string | null
  createdByName: string | null
}

export interface Sale {
  id: number
  number: string | null
  customerId: number | null
  customerIdentification: string | null
  customerName: string | null
  customerAddress: string | null
  customerEmail: string | null
  status: 'DRAFT' | 'ISSUED' | 'VOIDED'
  paymentStatus: 'PENDING' | 'PARTIAL' | 'PAID'
  saleDate: string
  issuedAt: string | null
  voidedAt: string | null
  voidReason: string | null
  subtotal: number
  discountTotal: number
  taxTotal: number
  total: number
  amountPaid: number
  balance: number
  notes: string | null
  createdBy: number
  createdByName: string | null
  createdAt: string
  items: SaleItem[]
  payments: Payment[]
}

export interface PurchaseItem {
  id: number
  productId: number
  sku: string
  productName: string
  quantity: number
  unitCost: number
  discountPercent: number
  taxRate: number
  lineSubtotal: number
  lineDiscount: number
  lineTax: number
  lineTotal: number
}

export interface Purchase {
  id: number
  number: string
  supplierId: number
  supplierName: string
  supplierIdentification: string | null
  purchaseDate: string
  status: 'DRAFT' | 'CONFIRMED' | 'CANCELLED'
  subtotal: number
  discountTotal: number
  taxTotal: number
  total: number
  notes: string | null
  confirmedAt: string | null
  createdBy: number
  createdByName: string | null
  createdAt: string
  items: PurchaseItem[]
}

export interface StockMovement {
  id: number
  productId: number
  productSku: string
  productName: string
  movementType: string
  quantity: number
  balanceAfter: number
  unitCost: number | null
  referenceType: string | null
  referenceId: number | null
  reason: string | null
  username: string | null
  createdAt: string
}

export interface AppUser {
  id: number
  username: string
  fullName: string
  email: string | null
  phone: string | null
  role: string
  active: boolean
  mustChangePassword: boolean
  lastLoginAt: string | null
  createdAt: string
}

export interface DashboardSummary {
  today: { date: string; count: number; total: number }
  week: { date: string; count: number; total: number }
  month: { date: string; count: number; total: number }
  lowStockCount: number
  pendingInvoicesCount: number
  receivablesTotal: number
  stockValue: number
  topProducts: { sku: string; name: string; quantity: number; total: number }[]
  evolution: { date: string; count: number; total: number }[]
  recentActivity: {
    id: number
    username: string | null
    action: string
    entityType: string | null
    entityId: string | null
    details: string | null
    createdAt: string
  }[]
}

export interface TaxCategory {
  code: string
  name: string
  rate: number
}

export interface SalesSummary {
  from: string
  to: string
  salesCount: number
  totalSales: number
  subtotal: number
  discountTotal: number
  taxTotal: number
  paymentsReceived: number
  estimatedCost: number
  estimatedProfit: number
  byDay: { date: string; salesCount: number; subtotal: number; discount: number; tax: number; total: number }[]
}

export interface ReceivableRow {
  saleId: number
  number: string
  customerIdentification: string | null
  customerName: string | null
  saleDate: string
  total: number
  amountPaid: number
  balance: number
  daysOpen: number
}

export interface InventoryRow {
  productId: number
  sku: string
  name: string
  categoryName: string | null
  stock: number
  minStock: number
  unitCost: number
  stockValue: number
  lowStock: boolean
}
