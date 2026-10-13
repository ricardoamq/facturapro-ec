import PartyPage from './PartyPage'

export default function CustomersPage() {
  return <PartyPage base="/api/customers" singular="Cliente" plural="Clientes" writePerm="CUSTOMERS_WRITE" />
}
