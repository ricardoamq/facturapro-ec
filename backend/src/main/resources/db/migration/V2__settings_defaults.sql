-- Valores por defecto de configuracion.
-- Tasas de IVA basadas en la normativa ecuatoriana vigente a enero de 2026:
--   * Ley de Regimen Tributario Interno art. 65: tarifa general 13%.
--   * Decreto Ejecutivo 198 (2024) elevo la tarifa general al 15% desde el 01-abr-2024.
--   * Decreto Ejecutivo 470 (dic-2024) la mantuvo para 2025.
--   * SRI, Circular NAC-DGECCGC25-00000006 (26-dic-2025): sin nuevo decreto, se mantiene el 15%.
--   * Existe ademas tarifa 0% y tarifa 5% para materiales de construccion (mismo art. 65).
-- La tasa es configurable en Configuracion > Impuestos y se congela en cada documento emitido.

INSERT INTO settings (setting_key, setting_value) VALUES
('business.name', 'FacturaPro EC (demostracion)'),
('business.legalName', 'Mi Negocio Cia. Ltda.'),
('business.ruc', ''),
('business.address', ''),
('business.phone', ''),
('business.email', ''),
('business.logoBase64', ''),
('currency.code', 'USD'),
('currency.symbol', '$'),
('locale.datePattern', 'dd/MM/yyyy'),
('locale.dateTimePattern', 'dd/MM/yyyy HH:mm'),
('tax.categories', '[{"code":"EXEMPT","name":"Exento","rate":0},{"code":"GENERAL","name":"General","rate":15},{"code":"REDUCED","name":"Reducido (materiales de construccion)","rate":5}]'),
('tax.defaultCategory', 'GENERAL'),
('numbering.saleSeries', 'FAC'),
('numbering.purchaseSeries', 'COM'),
('numbering.startAt', '1'),
('sales.allowDrafts', 'true'),
('sales.allowPartialPayments', 'true'),
('inventory.allowNegativeStock', 'false'),
('inventory.lowStockEnabled', 'true');
