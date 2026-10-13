import { Route, Routes } from 'react-router-dom'
import { RequireAuth, RequirePermission } from '@/auth/RequireAuth'
import AppLayout from '@/components/AppLayout'
import LoginPage from '@/pages/LoginPage'
import ForgotPasswordPage from '@/pages/ForgotPasswordPage'
import ResetPasswordPage from '@/pages/ResetPasswordPage'
import DashboardPage from '@/pages/DashboardPage'
import PosPage from '@/pages/PosPage'
import SalesPage from '@/pages/SalesPage'
import SaleDetailPage from '@/pages/SaleDetailPage'
import ProductsPage from '@/pages/ProductsPage'
import InventoryPage from '@/pages/InventoryPage'
import PurchasesPage from '@/pages/PurchasesPage'
import PurchaseDetailPage from '@/pages/PurchaseDetailPage'
import CustomersPage from '@/pages/CustomersPage'
import SuppliersPage from '@/pages/SuppliersPage'
import ReportsPage from '@/pages/ReportsPage'
import UsersPage from '@/pages/UsersPage'
import SettingsPage from '@/pages/SettingsPage'
import AuditPage from '@/pages/AuditPage'
import ReceivablesPage from '@/pages/ReceivablesPage'
import ProfilePage from '@/pages/ProfilePage'
import ChangePasswordPage from '@/pages/ChangePasswordPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/recuperar" element={<ForgotPasswordPage />} />
      <Route path="/restablecer" element={<ResetPasswordPage />} />

      <Route element={<RequireAuth />}>
        <Route path="/cambiar-password" element={<ChangePasswordPage />} />
        <Route element={<AppLayout />}>
          <Route element={<RequirePermission permission="DASHBOARD_VIEW" />}>
            <Route path="/" element={<DashboardPage />} />
          </Route>
          <Route element={<RequirePermission permission="SALES_WRITE" />}>
            <Route path="/pos" element={<PosPage />} />
          </Route>
          <Route element={<RequirePermission permission="SALES_VIEW" />}>
            <Route path="/ventas" element={<SalesPage />} />
            <Route path="/ventas/:id" element={<SaleDetailPage />} />
          </Route>
          <Route element={<RequirePermission permission="PAYMENTS_VIEW" />}>
            <Route path="/cobranzas" element={<ReceivablesPage />} />
          </Route>
          <Route element={<RequirePermission permission="PRODUCTS_VIEW" />}>
            <Route path="/productos" element={<ProductsPage />} />
          </Route>
          <Route element={<RequirePermission permission="INVENTORY_VIEW" />}>
            <Route path="/inventario" element={<InventoryPage />} />
          </Route>
          <Route element={<RequirePermission permission="PURCHASES_VIEW" />}>
            <Route path="/compras" element={<PurchasesPage />} />
            <Route path="/compras/:id" element={<PurchaseDetailPage />} />
          </Route>
          <Route element={<RequirePermission permission="CUSTOMERS_VIEW" />}>
            <Route path="/clientes" element={<CustomersPage />} />
          </Route>
          <Route element={<RequirePermission permission="SUPPLIERS_VIEW" />}>
            <Route path="/proveedores" element={<SuppliersPage />} />
          </Route>
          <Route element={<RequirePermission permission="REPORTS_VIEW" />}>
            <Route path="/reportes" element={<ReportsPage />} />
          </Route>
          <Route element={<RequirePermission permission="USERS_VIEW" />}>
            <Route path="/usuarios" element={<UsersPage />} />
          </Route>
          <Route element={<RequirePermission permission="SETTINGS_VIEW" />}>
            <Route path="/configuracion" element={<SettingsPage />} />
          </Route>
          <Route element={<RequirePermission permission="AUDIT_VIEW" />}>
            <Route path="/auditoria" element={<AuditPage />} />
          </Route>
          <Route path="/perfil" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route path="*" element={<div className="p-10 text-center text-muted">Pagina no encontrada</div>} />
    </Routes>
  )
}
