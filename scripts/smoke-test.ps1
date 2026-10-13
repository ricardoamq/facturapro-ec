# Smoke test de la API de FacturaPro EC (PowerShell 5.1)
# Requiere: backend en http://localhost:8085 y base de datos inicializada.
# Uso: .\scripts\smoke-test.ps1 [-BaseUrl http://localhost:8085] [-User admin] [-Password 'AdminDev12345!']
param(
    [string]$BaseUrl = "http://localhost:8085",
    [string]$User = "admin",
    [string]$Password = "AdminDev12345!"
)

$ErrorActionPreference = "Stop"
$script:passed = 0
$script:failed = 0

function Ok($name) { $script:passed++; Write-Host "  [OK]   $name" -ForegroundColor Green }
function Fail($name, $detail) { $script:failed++; Write-Host "  [FAIL] $name - $detail" -ForegroundColor Red }

$jar = Join-Path $env:TEMP "fp-smoke-cookies.txt"
Remove-Item $jar -ErrorAction SilentlyContinue

function Api($Method, $Path, $Body = $null, $Headers = @{}, $Raw = $false) {
    $curlArgs = @("-s", "-b", $jar, "-c", $jar, "-X", $Method, "$BaseUrl$Path",
        "-H", "Accept: application/json", "-w", "\n%{http_code}")
    foreach ($k in $Headers.Keys) { $curlArgs += @("-H", "$k`:$($Headers[$k])") }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 10 -Compress
        $tmp = [System.IO.Path]::GetTempFileName()
        [System.IO.File]::WriteAllText($tmp, $json)
        $curlArgs += @("-H", "Content-Type: application/json", "-d", "@$tmp")
        $out = & curl.exe @curlArgs
        Remove-Item $tmp -ErrorAction SilentlyContinue
    } else {
        $out = & curl.exe @curlArgs
    }
    $code = [int]($out[-1])
    $content = ($out[0..($out.Count - 2)] -join "`n")
    if ($Raw) { return @{ Code = $code; Content = $content } }
    $parsed = $null
    if ($content) { try { $parsed = $content | ConvertFrom-Json } catch { $parsed = $content } }
    return @{ Code = $code; Data = $parsed; Content = $content }
}

Write-Host "`n=== FacturaPro EC - Smoke test ===" -ForegroundColor Cyan

# 1. CSRF + login
$csrf = Api GET "/api/auth/csrf"
if ($csrf.Code -ne 200) { Write-Host "No se pudo obtener CSRF"; exit 1 }
$xsrf = $csrf.Data.token
$login = Api POST "/api/auth/login" @{ username = $User; password = $Password } @{ "X-XSRF-TOKEN" = $xsrf }
if ($login.Code -eq 200) { Ok "Login ($User)" } else { Fail "Login" $login.Content; exit 1 }
$xsrfCookie = (Get-Content $jar | Select-String "XSRF-TOKEN").ToString().Split("`t")[-1]
$H = @{ "X-XSRF-TOKEN" = $xsrfCookie }

# 2. /me y dashboard
$me = Api GET "/api/auth/me"
if ($me.Code -eq 200 -and $me.Data.username -eq $User) { Ok "/api/auth/me" } else { Fail "/api/auth/me" $me.Content }
$dash = Api GET "/api/dashboard/summary"
if ($dash.Code -eq 200) { Ok "/api/dashboard/summary" } else { Fail "Dashboard" $dash.Content }

# 3. Producto
$sku = "SMOKE-" + (Get-Random -Maximum 99999)
$prod = Api POST "/api/products" @{ sku = $sku; name = "Producto smoke"; purchasePrice = 10.00; salePrice = 15.50; unit = "UND"; initialStock = 100; minStock = 5; taxCategory = "GENERAL"; active = $true } $H
if ($prod.Code -eq 201) { Ok "Crear producto ($sku)" } else { Fail "Crear producto" $prod.Content }
$productId = $prod.Data.id

$dup = Api POST "/api/products" @{ sku = $sku; name = "Duplicado"; purchasePrice = 1; salePrice = 2; unit = "UND"; initialStock = 0; minStock = 0 } $H
if ($dup.Code -eq 409) { Ok "SKU duplicado rechazado (409)" } else { Fail "SKU duplicado" "esperaba 409, obtuve $($dup.Code)" }

# 4. Cliente
$cust = Api POST "/api/customers" @{ idType = "CEDULA"; identification = "1718250535"; businessName = "Cliente Smoke"; phone = "0990000000" } $H
if ($cust.Code -eq 201) { Ok "Crear cliente" } else { Fail "Crear cliente" $cust.Content }
$customerId = $cust.Data.id

$badCust = Api POST "/api/customers" @{ idType = "CEDULA"; identification = "1234567890"; businessName = "Invalida" } $H
if ($badCust.Code -eq 400) { Ok "Cedula invalida rechazada (400)" } else { Fail "Validacion cedula" "esperaba 400, obtuve $($badCust.Code)" }

# 5. Venta emitida con 2 unidades
$sale = Api POST "/api/sales" @{ customerId = $customerId; issue = $true; items = @(@{ productId = $productId; quantity = 2 }) } $H
if ($sale.Code -eq 201) { Ok "Crear venta emitida ($($sale.Data.number))" } else { Fail "Crear venta" $sale.Content }
$saleId = $sale.Data.id
$expectedTotal = [Math]::Round(2 * 15.50 * 1.15, 2)
if ([decimal]$sale.Data.total -eq [decimal]$expectedTotal) { Ok "Totales calculados en backend ($expectedTotal)" } else { Fail "Totales" "esperaba $expectedTotal, obtuve $($sale.Data.total)" }
if ($sale.Data.status -eq "ISSUED") { Ok "Estado ISSUED" } else { Fail "Estado" $sale.Data.status }

# 6. Stock descontado
$prodAfter = Api GET "/api/products/$productId"
if ([decimal]$prodAfter.Data.stock -eq [decimal]98) { Ok "Stock descontado (100 -> 98)" } else { Fail "Stock" "esperaba 98, obtuve $($prodAfter.Data.stock)" }

# 7. Movimiento de inventario registrado
$moves = Api GET "/api/inventory/movements?productId=$productId"
if ($moves.Code -eq 200 -and $moves.Data.content.Count -ge 2) { Ok "Movimientos de inventario trazables" } else { Fail "Movimientos" $moves.Content }

# 8. Venta con stock insuficiente
$insuf = Api POST "/api/sales" @{ issue = $true; items = @(@{ productId = $productId; quantity = 9999 }) } $H
if ($insuf.Code -eq 400 -and $insuf.Data.code -eq "INSUFFICIENT_STOCK") { Ok "Stock insuficiente rechazado" } else { Fail "Stock insuficiente" $insuf.Content }

# 9. Idempotencia
$key = "smoke-idem-" + [Guid]::NewGuid().ToString("N")
$s1 = Api POST "/api/sales" @{ issue = $true; items = @(@{ productId = $productId; quantity = 1 }) } @{ "X-XSRF-TOKEN" = $xsrfCookie; "Idempotency-Key" = $key }
$s2 = Api POST "/api/sales" @{ issue = $true; items = @(@{ productId = $productId; quantity = 1 }) } @{ "X-XSRF-TOKEN" = $xsrfCookie; "Idempotency-Key" = $key }
if ($s1.Code -eq 201 -and $s2.Code -eq 201 -and $s1.Data.id -eq $s2.Data.id) { Ok "Idempotencia de venta (misma venta en el reintento)" } else { Fail "Idempotencia" "ids $($s1.Data.id) / $($s2.Data.id)" }

# 10. Pagos parciales y completos
$pay1 = Api POST "/api/sales/$saleId/payments" @{ amount = 10.00; method = "CASH" } $H
if ($pay1.Code -eq 201 -and $pay1.Data.voided -eq $false) { Ok "Pago parcial registrado" } else { Fail "Pago parcial" $pay1.Content }
$salePay = Api GET "/api/sales/$saleId"
if ($salePay.Data.paymentStatus -eq "PARTIAL") { Ok "Estado de pago PARTIAL" } else { Fail "Payment status" $salePay.Data.paymentStatus }
$pay2 = Api POST "/api/sales/$saleId/payments" @{ amount = [decimal]$salePay.Data.balance; method = "TRANSFER" } $H
$salePaid = Api GET "/api/sales/$saleId"
if ($salePaid.Data.paymentStatus -eq "PAID" -and [decimal]$salePaid.Data.balance -eq 0) { Ok "Venta pagada completa (PAID, saldo 0)" } else { Fail "Pago completo" $salePaid.Data.paymentStatus }

$overpay = Api POST "/api/sales/$saleId/payments" @{ amount = 1.00; method = "CASH" } $H
if ($overpay.Code -eq 400) { Ok "Pago sobre saldo 0 rechazado" } else { Fail "Sobrepago" "esperaba 400, obtuve $($overpay.Code)" }

# 11. PDF de la factura
$pdfOut = Join-Path $env:TEMP "fp-invoice.pdf"
& curl.exe -s -b $jar -o $pdfOut "$BaseUrl/api/invoices/$($sale.Data.number)/pdf"
$pdfSize = (Get-Item $pdfOut).Length
if ($pdfSize -gt 1000) { Ok "PDF generado ($pdfSize bytes)" } else { Fail "PDF" "archivo de $pdfSize bytes" }

# 12. Anulacion de venta con pagos debe fallar
$voidBlocked = Api POST "/api/sales/$saleId/void" @{ reason = "Prueba" } $H
if ($voidBlocked.Code -eq 400) { Ok "Anulacion bloqueada mientras hay pagos" } else { Fail "Anulacion bloqueada" "esperaba 400, obtuve $($voidBlocked.Code)" }

# 13. Compra y confirmacion actualiza inventario
$sups = Api GET "/api/suppliers?size=1"
if ($sups.Code -ne 200 -or $sups.Data.content.Count -eq 0) {
    $sup = Api POST "/api/suppliers" @{ idType = "RUC"; identification = "1791100888001"; businessName = "Proveedor Smoke" } $H
    $supplierId = $sup.Data.id
} else { $supplierId = $sups.Data.content[0].id }
$stockBefore = [decimal](Api GET "/api/products/$productId").Data.stock
$purchase = Api POST "/api/purchases" @{ supplierId = $supplierId; items = @(@{ productId = $productId; quantity = 10; unitCost = 9.00 }) } $H
if ($purchase.Code -eq 201 -and $purchase.Data.status -eq "DRAFT") { Ok "Compra en borrador creada" } else { Fail "Compra" $purchase.Content }
$stockDraft = [decimal](Api GET "/api/products/$productId").Data.stock
if ($stockDraft -eq $stockBefore) { Ok "Borrador no modifica existencias" } else { Fail "Borrador" "stock cambio a $stockDraft" }
$confirm = Api POST "/api/purchases/$($purchase.Data.id)/confirm" $null $H
$stockConfirmed = [decimal](Api GET "/api/products/$productId").Data.stock
if ($confirm.Code -eq 200 -and $stockConfirmed -eq ($stockBefore + 10)) { Ok "Confirmar compra incrementa inventario" } else { Fail "Confirmar compra" "stock $stockBefore -> $stockConfirmed" }

# 14. Ajuste de inventario exige motivo
$adjNoReason = Api POST "/api/inventory/adjustments" @{ productId = $productId; quantity = -1 } $H
if ($adjNoReason.Code -eq 400) { Ok "Ajuste sin motivo rechazado" } else { Fail "Ajuste sin motivo" "esperaba 400, obtuve $($adjNoReason.Code)" }
$adj = Api POST "/api/inventory/adjustments" @{ productId = $productId; quantity = -1; reason = "Merma por rotura (smoke)" } $H
if ($adj.Code -eq 200) { Ok "Ajuste con motivo registrado" } else { Fail "Ajuste" $adj.Content }

# 15. Reportes reales
$from = (Get-Date).AddDays(-30).ToString("yyyy-MM-dd")
$to = (Get-Date).ToString("yyyy-MM-dd")
$rep = Api GET "/api/reports/sales?from=$from&to=$to"
if ($rep.Code -eq 200 -and [decimal]$rep.Data.totalSales -gt 0) { Ok "Reporte de ventas con datos reales (total $($rep.Data.totalSales))" } else { Fail "Reporte ventas" $rep.Content }
$rec = Api GET "/api/reports/receivables"
if ($rec.Code -eq 200) { Ok "Cuentas por cobrar" } else { Fail "Receivables" $rec.Content }

# 16. Autorizacion: sin sesion => 401; rol sin permiso => 403
$noAuth = & curl.exe -s -o "$env:TEMP\fp-noauth.json" -w "%{http_code}" "$BaseUrl/api/products"
if ($noAuth -eq "401") { Ok "Endpoint protegido sin sesion (401)" } else { Fail "401" "obtuve $noAuth" }

# 17. Usuario demo VENDEDOR (si existe) no puede gestionar usuarios
$venJar = Join-Path $env:TEMP "fp-ven-cookies.txt"
Remove-Item $venJar -ErrorAction SilentlyContinue
$venCsrf = (& curl.exe -s -c $venJar "$BaseUrl/api/auth/csrf" | ConvertFrom-Json).token
$venCookie = (Get-Content $venJar | Select-String "XSRF-TOKEN").ToString().Split("`t")[-1]
& curl.exe -s -b $venJar -c $venJar -X POST "$BaseUrl/api/auth/login" -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $venCookie" -d '{\"username\":\"vendedor\",\"password\":\"VendedorDemo123!\"}' -o "$env:TEMP\fp-ven-login.json"
$venLogin = Get-Content "$env:TEMP\fp-ven-login.json" -Raw | ConvertFrom-Json
if ($venLogin.username -eq "vendedor") {
    $venUsers = & curl.exe -s -b $venJar -o "$env:TEMP\fp-ven-users.json" -w "%{http_code}" "$BaseUrl/api/users"
    if ($venUsers -eq "403") { Ok "VENDEDOR sin permiso USERS_VIEW (403)" } else { Fail "RBAC vendedor" "obtuve $venUsers" }
    $venSale = & curl.exe -s -b $venJar -o "$env:TEMP\fp-ven-sale.json" -w "%{http_code}" "$BaseUrl/api/sales"
    if ($venSale -eq "200") { Ok "VENDEDOR si puede consultar ventas" } else { Fail "RBAC ventas" "obtuve $venSale" }
} else {
    Write-Host "  [SKIP] Usuario demo 'vendedor' no existe (ejecute el backend con perfil demo)" -ForegroundColor Yellow
}

Write-Host "`n=== Resultado: $($script:passed) correctas, $($script:failed) fallidas ===" -ForegroundColor Cyan
if ($script:failed -gt 0) { exit 1 }
