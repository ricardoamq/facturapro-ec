import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { useAuth } from '@/auth/AuthContext'
import { api, ApiError } from '@/lib/api'
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  Pagination,
  Select,
  Spinner,
  Textarea,
} from '@/components/ui'
import { messageOf, money, qty } from '@/lib/format'
import type { Category, Page as PageT, Product } from '@/types'

interface FormState {
  sku: string
  name: string
  description: string
  categoryId: string
  purchasePrice: string
  salePrice: string
  unit: string
  barcode: string
  initialStock: string
  minStock: string
  taxCategory: string
  active: boolean
}

const emptyForm: FormState = {
  sku: '',
  name: '',
  description: '',
  categoryId: '',
  purchasePrice: '0',
  salePrice: '0',
  unit: 'UND',
  barcode: '',
  initialStock: '0',
  minStock: '0',
  taxCategory: 'GENERAL',
  active: true,
}

export default function ProductsPage() {
  const { can } = useAuth()
  const [data, setData] = useState<PageT<Product> | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [editing, setEditing] = useState<Product | null>(null)
  const [open, setOpen] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [toDelete, setToDelete] = useState<Product | null>(null)
  const [catName, setCatName] = useState('')
  const [catOpen, setCatOpen] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api.get<PageT<Product>>(
        `/api/products?search=${encodeURIComponent(search)}&page=${page}&size=20`,
      )
      setData(res)
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

  useEffect(() => {
    api.get<Category[]>('/api/categories').then(setCategories).catch(() => setCategories([]))
  }, [])

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setErrors({})
    setOpen(true)
  }

  function openEdit(p: Product) {
    setEditing(p)
    setForm({
      sku: p.sku,
      name: p.name,
      description: p.description ?? '',
      categoryId: p.categoryId ? String(p.categoryId) : '',
      purchasePrice: String(p.purchasePrice),
      salePrice: String(p.salePrice),
      unit: p.unit,
      barcode: p.barcode ?? '',
      initialStock: String(p.stock),
      minStock: String(p.minStock),
      taxCategory: p.taxCategory,
      active: p.active,
    })
    setErrors({})
    setOpen(true)
  }

  async function save() {
    setErrors({})
    const body = {
      sku: form.sku.trim(),
      name: form.name.trim(),
      description: form.description.trim() || null,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      purchasePrice: Number(form.purchasePrice),
      salePrice: Number(form.salePrice),
      unit: form.unit.trim(),
      barcode: form.barcode.trim() || null,
      minStock: Number(form.minStock),
      taxCategory: form.taxCategory,
      active: form.active,
      ...(editing ? {} : { initialStock: Number(form.initialStock) }),
    }
    try {
      if (editing) {
        await api.put(`/api/products/${editing.id}`, body)
        toast.success('Producto actualizado.')
      } else {
        await api.post('/api/products', body)
        toast.success('Producto creado.')
      }
      setOpen(false)
      await load()
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors) setErrors(e.fieldErrors)
      toast.error(messageOf(e))
    }
  }

  async function doDelete() {
    if (!toDelete) return
    try {
      await api.del(`/api/products/${toDelete.id}`)
      toast.success('Producto eliminado.')
      setToDelete(null)
      await load()
    } catch (e) {
      toast.error(messageOf(e))
    }
  }

  async function saveCategory() {
    try {
      await api.post('/api/categories', { name: catName.trim(), active: true })
      setCategories(await api.get<Category[]>('/api/categories'))
      setCatName('')
      setCatOpen(false)
      toast.success('Categoria creada.')
    } catch (e) {
      toast.error(messageOf(e))
    }
  }

  return (
    <div>
      <PageHeader
        title="Productos"
        subtitle="Catalogo, precios y existencias"
        actions={
          <>
            {can('PRODUCTS_WRITE') && (
              <Button variant="secondary" onClick={() => setCatOpen(true)}>
                Nueva categoria
              </Button>
            )}
            {can('PRODUCTS_WRITE') && <Button onClick={openCreate}>Nuevo producto</Button>}
          </>
        }
      />

      <Card>
        <div className="p-3 flex flex-wrap gap-2 items-center border-b border-border">
          <Input
            placeholder="Buscar por SKU, nombre o codigo de barras..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setPage(0)
            }}
            className="max-w-sm"
          />
          {data && <span className="text-xs text-muted">{data.totalElements} resultado(s)</span>}
        </div>

        {loading && !data ? (
          <Spinner />
        ) : !data || data.content.length === 0 ? (
          <EmptyState
            title="No se encontraron productos"
            description="Cree su primer producto para comenzar a vender."
            action={can('PRODUCTS_WRITE') ? <Button onClick={openCreate}>Nuevo producto</Button> : undefined}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr>
                  <th className="th">SKU</th>
                  <th className="th">Nombre</th>
                  <th className="th">Categoria</th>
                  <th className="th text-right">Costo</th>
                  <th className="th text-right">Precio</th>
                  <th className="th text-right">Margen</th>
                  <th className="th text-right">Stock</th>
                  <th className="th">Estado</th>
                  <th className="th text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id} className="hover:bg-background/60">
                    <td className="td font-mono text-xs">{p.sku}</td>
                    <td className="td font-medium">{p.name}</td>
                    <td className="td text-muted">{p.categoryName ?? '—'}</td>
                    <td className="td text-right tabular-nums">${money(p.purchasePrice)}</td>
                    <td className="td text-right tabular-nums font-semibold">${money(p.salePrice)}</td>
                    <td className="td text-right tabular-nums text-muted">
                      {money(p.marginPercent)}%
                    </td>
                    <td className="td text-right tabular-nums">
                      <span className={p.lowStock ? 'text-red-600 font-semibold' : ''}>{qty(p.stock)}</span>
                      {p.lowStock && <span className="text-[10px] block text-red-500">bajo minimo</span>}
                    </td>
                    <td className="td">
                      {p.active ? <Badge tone="success">Activo</Badge> : <Badge tone="danger">Inactivo</Badge>}
                    </td>
                    <td className="td text-right whitespace-nowrap">
                      {can('PRODUCTS_WRITE') && (
                        <Button variant="ghost" onClick={() => openEdit(p)}>
                          Editar
                        </Button>
                      )}
                      {can('PRODUCTS_MANAGE') && (
                        <Button variant="ghost" className="!text-red-600" onClick={() => setToDelete(p)}>
                          Eliminar
                        </Button>
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

      <Modal open={open} title={editing ? 'Editar producto' : 'Nuevo producto'} onClose={() => setOpen(false)} wide>
        <div className="grid sm:grid-cols-2 gap-3">
          <Field label="SKU" required error={errors.sku}>
            <Input value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} disabled={!!editing} />
          </Field>
          <Field label="Nombre" required error={errors.name}>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Field>
          <Field label="Categoria">
            <Select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
              <option value="">Sin categoria</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Unidad" required error={errors.unit}>
            <Select value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })}>
              {['UND', 'CAJA', 'PAQ', 'KG', 'LT', 'MT', 'DOC'].map((u) => (
                <option key={u}>{u}</option>
              ))}
            </Select>
          </Field>
          <Field label="Precio de compra" required error={errors.purchasePrice}>
            <Input
              type="number"
              step="0.01"
              min="0"
              value={form.purchasePrice}
              onChange={(e) => setForm({ ...form, purchasePrice: e.target.value })}
            />
          </Field>
          <Field label="Precio de venta" required error={errors.salePrice}>
            <Input
              type="number"
              step="0.01"
              min="0"
              value={form.salePrice}
              onChange={(e) => setForm({ ...form, salePrice: e.target.value })}
            />
          </Field>
          {!editing && (
            <Field label="Existencia inicial" required error={errors.initialStock}>
              <Input
                type="number"
                step="0.001"
                min="0"
                value={form.initialStock}
                onChange={(e) => setForm({ ...form, initialStock: e.target.value })}
              />
            </Field>
          )}
          <Field label="Stock minimo" required error={errors.minStock}>
            <Input
              type="number"
              step="0.001"
              min="0"
              value={form.minStock}
              onChange={(e) => setForm({ ...form, minStock: e.target.value })}
            />
          </Field>
          <Field label="Categoria fiscal">
            <Select value={form.taxCategory} onChange={(e) => setForm({ ...form, taxCategory: e.target.value })}>
              <option value="GENERAL">General (15%)</option>
              <option value="REDUCED">Reducida (5%)</option>
              <option value="EXEMPT">Exenta (0%)</option>
            </Select>
          </Field>
          <Field label="Codigo de barras">
            <Input value={form.barcode} onChange={(e) => setForm({ ...form, barcode: e.target.value })} />
          </Field>
          <div className="sm:col-span-2">
            <Field label="Descripcion">
              <Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </Field>
          </div>
          <label className="flex items-center gap-2 text-sm sm:col-span-2">
            <input
              type="checkbox"
              checked={form.active}
              onChange={(e) => setForm({ ...form, active: e.target.checked })}
            />
            Producto activo (disponible para venta)
          </label>
        </div>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setOpen(false)}>
            Cancelar
          </Button>
          <Button onClick={save}>Guardar</Button>
        </div>
      </Modal>

      <Modal open={catOpen} title="Nueva categoria" onClose={() => setCatOpen(false)}>
        <Field label="Nombre" required>
          <Input value={catName} onChange={(e) => setCatName(e.target.value)} autoFocus />
        </Field>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setCatOpen(false)}>
            Cancelar
          </Button>
          <Button onClick={saveCategory} disabled={!catName.trim()}>
            Crear
          </Button>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!toDelete}
        title="Eliminar producto"
        message={`Se eliminara el producto "${toDelete?.name}" (${toDelete?.sku}). Esta accion no se puede deshacer y fallara si el producto tiene movimientos de inventario.`}
        confirmLabel="Eliminar"
        danger
        onConfirm={doDelete}
        onClose={() => setToDelete(null)}
      />
    </div>
  )
}
