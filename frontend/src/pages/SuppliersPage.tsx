import PartyPage from './PartyPage'

export default function SuppliersPage() {
  return <PartyPage base="/api/suppliers" singular="Proveedor" plural="Proveedores" writePerm="SUPPLIERS_WRITE" />
}
