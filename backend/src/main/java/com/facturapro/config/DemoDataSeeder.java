package com.facturapro.config;

import com.facturapro.customers.Customer;
import com.facturapro.customers.CustomerRepository;
import com.facturapro.customers.IdType;
import com.facturapro.products.Category;
import com.facturapro.products.CategoryRepository;
import com.facturapro.products.Product;
import com.facturapro.products.ProductRepository;
import com.facturapro.products.TaxCategory;
import com.facturapro.inventory.StockService;
import com.facturapro.sales.PaymentMethod;
import com.facturapro.sales.SaleDtos;
import com.facturapro.sales.SaleService;
import com.facturapro.security.AuthUser;
import com.facturapro.suppliers.Supplier;
import com.facturapro.suppliers.SupplierRepository;
import com.facturapro.users.User;
import com.facturapro.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos de DEMOSTRACION claramente identificados como ficticios.
 * Solo se ejecuta si app.demo.enabled=true (perfil/variable de entorno).
 * Nunca se activa automaticamente en produccion.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final AppProperties appProperties;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final SaleService saleService;
    private final StockService stockService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!appProperties.demo().enabled()) {
            return;
        }
        if (productRepository.count() > 0) {
            log.info("Datos de demostracion: la base ya contiene datos, no se carga nada.");
            return;
        }
        log.info("Cargando datos de DEMOSTRACION (ficticios)...");

        User actor = userRepository.findByUsername("vendedor").orElseGet(() -> {
            User u = new User();
            u.setUsername("vendedor");
            u.setFullName("Vendedor Demo");
            u.setEmail("vendedor@demo.example");
            u.setRole(com.facturapro.users.Role.VENDEDOR);
            u.setPasswordHash(com.facturapro.config.SecurityConfig.ENCODER.encode("VendedorDemo123!"));
            u.setActive(true);
            u.setMustChangePassword(true);
            return userRepository.save(u);
        });
        // Contexto de seguridad simulado para que los movimientos de inventario
        // y las ventas queden registrados con un usuario responsable.
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(actor.getId(), actor.getUsername(), "N/A", actor.getFullName(), actor.getRole(),
                        true, false, false),
                null, actor.getRole().getPermissions().stream()
                        .map(p -> new org.springframework.security.core.authority.SimpleGrantedAuthority(p.name()))
                        .toList()));

        Category alimentos = category("Alimentos", "Productos de consumo general");
        Category bebidas = category("Bebidas", "Bebidas y refrescos");
        Category limpieza = category("Limpieza", "Aseo y limpieza");

        Product p1 = product("PROD-001", "Arroz blanco 1Kg", alimentos, "1.20", "1.85", "60", "20", TaxCategory.GENERAL);
        Product p2 = product("PROD-002", "Aceite vegetal 1L", alimentos, "2.50", "3.60", "40", "10", TaxCategory.GENERAL);
        Product p3 = product("PROD-003", "Refresco cola 1.5L", bebidas, "0.90", "1.50", "96", "24", TaxCategory.GENERAL);
        Product p4 = product("PROD-004", "Agua mineral 1L", bebidas, "0.30", "0.60", "120", "30", TaxCategory.GENERAL);
        Product p5 = product("PROD-005", "Detergente liquido 1L", limpieza, "1.80", "2.95", "8", "15", TaxCategory.GENERAL);
        Product p6 = product("PROD-006", "Papel higienico x4", limpieza, "1.10", "1.90", "54", "12", TaxCategory.GENERAL);
        Supplier supplier = new Supplier();
        supplier.setIdType(IdType.RUC);
        supplier.setIdentification("1790012345001");
        supplier.setBusinessName("Distribuidora Demo Andina Cia. Ltda.");
        supplier.setAddress("Av. Siempre Viva 742, Quito");
        supplier.setPhone("022000000");
        supplier.setEmail("ventas@demo-andina.example");
        supplier = supplierRepository.save(supplier);

        Customer c1 = customer("1712345678", "Maria Fernanda Lopez", IdType.CEDULA, "Quito", "maria.lopez@example.com");
        Customer c2 = customer("1798765432001", "Comercial El Progreso S.A.", IdType.RUC, "Cuenca", "compras@elprogreso.example");
        Customer c3 = customer("0987654321", "Carlos Mendoza", IdType.CEDULA, "Guayaquil", "carlos.mendoza@example.com");

        // Ventas ficticias de los ultimos 14 dias para poblar dashboard y reportes.
        for (int day = 13; day >= 0; day--) {
            LocalDate date = LocalDate.now().minusDays(day);
            int salesToday = (day % 3) + 1;
            for (int i = 0; i < salesToday; i++) {
                Customer customer = switch ((day + i) % 3) {
                    case 0 -> c1;
                    case 1 -> c2;
                    default -> c3;
                };
                Product product = switch ((day + i) % 3) {
                    case 0 -> p1;
                    case 1 -> p3;
                    default -> p6;
                };
                Product product2 = switch ((day + i) % 2) {
                    case 0 -> p2;
                    default -> p4;
                };
                try {
                    SaleDtos.CreateRequest request = new SaleDtos.CreateRequest(
                            customer.getId(),
                            date,
                            "Venta de demostracion",
                            true,
                            List.of(
                                    new SaleDtos.ItemRequest(product.getId(), new BigDecimal("2"), null, null),
                                    new SaleDtos.ItemRequest(product2.getId(), new BigDecimal("1"), null, null)),
                            i % 2 == 0 ? null : new BigDecimal("1.00"),
                            PaymentMethod.CASH);
                    saleService.create(request, "demo-" + date + "-" + i + "-" + actor.getId());
                } catch (Exception ex) {
                    log.warn("No se pudo crear venta de demostracion: {}", ex.getMessage());
                }
            }
        }
        log.info("Datos de demostracion cargados: {} productos, {} clientes, 1 proveedor.",
                productRepository.count(), customerRepository.count());
    }

    private Category category(String name, String description) {
        return categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Category c = new Category();
            c.setName(name);
            c.setDescription(description);
            return categoryRepository.save(c);
        });
    }

    private Product product(String sku, String name, Category category, String cost, String price,
                            String stock, String minStock, TaxCategory taxCategory) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setCategory(category);
        p.setPurchasePrice(new BigDecimal(cost));
        p.setSalePrice(new BigDecimal(price));
        p.setStock(BigDecimal.ZERO);
        p.setMinStock(new BigDecimal(minStock));
        p.setTaxCategory(taxCategory);
        p.setUnit("UND");
        p.setActive(true);
        Product saved = productRepository.save(p);
        stockService.adjust(saved.getId(), new BigDecimal(stock),
                "Carga inicial de datos de demostracion (ficticia)");
        return productRepository.findById(saved.getId()).orElseThrow();
    }

    private Customer customer(String identification, String name, IdType type, String address, String email) {
        Customer c = new Customer();
        c.setIdentification(identification);
        c.setBusinessName(name);
        c.setIdType(type);
        c.setAddress(address);
        c.setEmail(email);
        return customerRepository.save(c);
    }
}
