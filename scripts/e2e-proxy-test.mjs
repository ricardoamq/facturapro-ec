// Prueba end-to-end del frontend contra el backend usando el proxy de Vite (http://localhost:5173)
const BASE = process.env.BASE_URL || 'http://localhost:5173'
const results = []
let cookie = ''
let xsrf = ''

function ok(name, cond, extra = '') {
  results.push({ name, pass: !!cond, extra })
  console.log(`${cond ? '  [OK]  ' : '  [FAIL]'} ${name}${extra ? ' -> ' + extra : ''}`)
}

function storeCookies(res) {
  const setCookies = res.headers.getSetCookie ? res.headers.getSetCookie() : []
  for (const sc of setCookies) {
    const pair = sc.split(';')[0]
    const [k, ...v] = pair.split('=')
    const key = k.trim()
    if (key === 'XSRF-TOKEN') xsrf = decodeURIComponent(v.join('='))
    if (key === 'JSESSIONID' || key === 'XSRF-TOKEN') {
      cookie = cookie
        .split('; ')
        .filter(Boolean)
        .filter((c) => !c.startsWith(key + '='))
        .concat([pair])
        .join('; ')
    }
  }
}

async function call(method, path, body, headers = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      Accept: 'application/json',
      ...(cookie ? { Cookie: cookie } : {}),
      ...(method !== 'GET' && xsrf ? { 'X-XSRF-TOKEN': xsrf } : {}),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  storeCookies(res)
  const text = await res.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = text
  }
  return { status: res.status, data, headers: res.headers }
}

const PASSWORDS = ['AdminDemo12345!', 'AdminDev12345!']

async function main() {
  console.log(`=== E2E via proxy ${BASE} ===`)

  // 1. CSRF endpoint
  let r = await call('GET', '/api/auth/csrf')
  ok('GET /api/auth/csrf', r.status === 200 && r.data?.token, `status=${r.status}`)
  xsrf = r.data?.token || xsrf

  // 2. Login
  let session = null
  for (const pw of PASSWORDS) {
    const res = await call('POST', '/api/auth/login', { username: 'admin', password: pw })
    if (res.status === 200) {
      session = res.data
      ok('POST /api/auth/login (admin)', true, `password=${pw}`)
      break
    }
  }
  if (!session) ok('POST /api/auth/login (admin)', false, 'ninguna contrasena funciono')

  if (!session) {
    // Usuario demo alternativo
    const res = await call('POST', '/api/auth/login', { username: 'vendedor', password: 'VendedorDemo123!' })
    ok('POST /api/auth/login (vendedor fallback)', res.status === 200, `status=${res.status}`)
    if (res.status === 200) session = res.data
  }

  if (!session) {
    console.log('No se pudo iniciar sesion. Abortando.')
    process.exit(1)
  }

  ok('Session tiene permisos', Array.isArray(session.permissions) && session.permissions.length > 0, `${session.permissions?.length} permisos`)

  // 3. Endpoint protegido con cookie de sesion
  r = await call('GET', '/api/auth/me')
  ok('GET /api/auth/me', r.status === 200 && r.data?.username, `user=${r.data?.username}`)

  // 4. Dashboard
  r = await call('GET', '/api/dashboard/summary')
  ok('GET /api/dashboard/summary', r.status === 200 && r.data?.today !== undefined, `status=${r.status}`)

  // 5. CSRF efectivo: POST sin token debe fallar
  const noCsrf = await fetch(BASE + '/api/products', {
    method: 'POST',
    headers: { Cookie: cookie, 'Content-Type': 'application/json' },
    body: JSON.stringify({ sku: 'X', name: 'X', purchasePrice: 0, salePrice: 0, unit: 'UND', initialStock: 0, minStock: 0, taxCategory: 'GENERAL' }),
  })
  ok('POST sin X-XSRF-TOKEN rechazado', noCsrf.status === 400 || noCsrf.status === 403, `status=${noCsrf.status}`)

  // 6. Producto
  const sku = 'E2E-' + Date.now()
  r = await call('POST', '/api/products', {
    sku,
    name: 'Producto E2E',
    description: 'Creado por prueba end-to-end',
    categoryId: null,
    purchasePrice: 10,
    salePrice: 15,
    unit: 'UND',
    barcode: null,
    initialStock: 100,
    minStock: 5,
    taxCategory: 'GENERAL',
    active: true,
  })
  ok('POST /api/products crea producto', r.status === 201 && r.data?.id, `id=${r.data?.id} status=${r.status}`)
  const productId = r.data?.id
  const stockBefore = Number(r.data?.stock ?? 0)

  // 7. Venta emitida con pago inicial
  r = await call(
    'POST',
    '/api/sales',
    {
      customerId: null,
      saleDate: new Date().toISOString().slice(0, 10),
      notes: 'Venta E2E',
      issue: true,
      items: [{ productId, quantity: 2, unitPrice: 15, discountPercent: null }],
      initialPaymentAmount: null,
      initialPaymentMethod: null,
    },
    { 'Idempotency-Key': crypto.randomUUID() },
  )
  ok('POST /api/sales emite venta', r.status === 201 && r.data?.number, `numero=${r.data?.number} status=${r.status}`)
  const saleId = r.data?.id
  const saleNumber = r.data?.number

  // 8. El backend calculo los totales
  if (r.data) {
    const total = Number(r.data.total)
    ok('Totales calculados por backend', total > 0 && r.data.items?.length === 1, `total=${total}`)
  }

  // 9. Stock descontado
  r = await call('GET', `/api/products/${productId}`)
  const stockAfter = Number(r.data?.stock ?? 0)
  ok('Stock descontado tras venta', stockAfter === stockBefore - 2, `${stockBefore} -> ${stockAfter}`)

  // 10. Idempotencia
  const key = crypto.randomUUID()
  const body = {
    customerId: null,
    saleDate: new Date().toISOString().slice(0, 10),
    notes: 'Idem E2E',
    issue: true,
    items: [{ productId, quantity: 1, unitPrice: 15, discountPercent: null }],
    initialPaymentAmount: null,
    initialPaymentMethod: null,
  }
  const a = await call('POST', '/api/sales', body, { 'Idempotency-Key': key })
  const b = await call('POST', '/api/sales', body, { 'Idempotency-Key': key })
  ok('Idempotencia de creacion de venta', a.data?.id === b.data?.id && a.status === 201 && b.status === 201, `ids=${a.data?.id}/${b.data?.id}`)

  // 11. Pago parcial
  if (saleId) {
    r = await call('POST', `/api/sales/${saleId}/payments`, { amount: 5, method: 'CASH', reference: 'E2E', notes: null }, { 'Idempotency-Key': crypto.randomUUID() })
    ok('POST /api/sales/{id}/payments', r.status === 201, `status=${r.status}`)
    r = await call('GET', `/api/sales/${saleId}`)
    ok('Saldo recalculado', r.data && Number(r.data.amountPaid) === 5, `pagado=${r.data?.amountPaid} saldo=${r.data?.balance}`)
  }

  // 12. PDF del comprobante
  if (saleNumber) {
    const res = await fetch(`${BASE}/api/invoices/${saleNumber}/pdf`, { headers: { Cookie: cookie } })
    const buf = Buffer.from(await res.arrayBuffer())
    ok('GET /api/invoices/{number}/pdf', res.status === 200 && buf.slice(0, 5).toString() === '%PDF-', `status=${res.status} bytes=${buf.length}`)
  }

  // 13. RBAC: un endpoint que el rol no deberia permitir se prueba con usuario vendedor
  // (verificacion principal de permisos: 401 sin sesion)
  const anon = await fetch(BASE + '/api/products')
  ok('GET /api/products sin sesion = 401', anon.status === 401, `status=${anon.status}`)

  // 14. Reportes
  const today = new Date().toISOString().slice(0, 10)
  const from = new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10)
  r = await call('GET', `/api/reports/sales?from=${from}&to=${today}`)
  ok('GET /api/reports/sales', r.status === 200 && r.data?.salesCount !== undefined, `ventas=${r.data?.salesCount}`)

  r = await call('GET', '/api/reports/receivables')
  ok('GET /api/reports/receivables', r.status === 200 && Array.isArray(r.data), `filas=${r.data?.length}`)

  r = await call('GET', '/api/inventory/summary')
  ok('GET /api/inventory/summary', r.status === 200 && r.data?.activeProducts !== undefined, `activos=${r.data?.activeProducts}`)

  r = await call('GET', '/api/inventory/movements?size=5')
  ok('GET /api/inventory/movements', r.status === 200 && Array.isArray(r.data?.content), `movs=${r.data?.totalElements}`)

  r = await call('GET', '/api/settings')
  ok('GET /api/settings', r.status === 200 && Object.keys(r.data || {}).length > 0, `keys=${Object.keys(r.data || {}).length}`)

  r = await call('GET', '/api/users?size=5')
  ok('GET /api/users', r.status === 200 && Array.isArray(r.data?.content), `usuarios=${r.data?.totalElements}`)

  r = await call('GET', '/api/audit?size=5')
  ok('GET /api/audit', r.status === 200 && Array.isArray(r.data?.content), `eventos=${r.data?.totalElements}`)

  r = await call('GET', '/api/sales?size=5')
  ok('GET /api/sales', r.status === 200 && Array.isArray(r.data?.content), `ventas=${r.data?.totalElements}`)

  r = await call('GET', '/api/purchases?size=5')
  ok('GET /api/purchases', r.status === 200 && Array.isArray(r.data?.content), `compras=${r.data?.totalElements}`)

  r = await call('GET', '/api/customers?size=5')
  ok('GET /api/customers', r.status === 200 && Array.isArray(r.data?.content), `clientes=${r.data?.totalElements}`)

  r = await call('GET', '/api/suppliers?size=5')
  ok('GET /api/suppliers', r.status === 200 && Array.isArray(r.data?.content), `proveedores=${r.data?.totalElements}`)

  // 15. Logout
  r = await call('POST', '/api/auth/logout')
  ok('POST /api/auth/logout', r.status === 200 || r.status === 204, `status=${r.status}`)

  const failed = results.filter((x) => !x.pass)
  console.log(`\n=== Resultado: ${results.length - failed.length}/${results.length} pruebas OK ===`)
  if (failed.length) {
    console.log('Fallas:')
    for (const f of failed) console.log(` - ${f.name} ${f.extra}`)
    process.exit(1)
  }
}

main().catch((e) => {
  console.error('ERROR FATAL:', e)
  process.exit(1)
})
