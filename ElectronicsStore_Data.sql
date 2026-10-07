-- =====================================================================
-- DỮ LIỆU MẪU - ElectronicsStore (SQL Server) - package com.example.store.entity
-- !!! Script XÓA SẠCH dữ liệu cũ của các bảng bên dưới rồi nạp lại !!!
-- Giả định: bảng đã được Hibernate tạo, naming snake_case (Spring Boot).
-- Ảnh (logo_url, image, avatar, product_variant_images) = NULL / không có dòng.
-- Mật khẩu của mọi user: 123456 (BCrypt)
-- =====================================================================
SET NOCOUNT ON;
SET XACT_ABORT ON;   -- lỗi ở bất kỳ câu nào thì rollback toàn bộ
BEGIN TRAN;

-- ---------- dọn dữ liệu cũ (thứ tự theo khóa ngoại) ----------
DELETE FROM reviews;
DELETE FROM user_follow_products;
DELETE FROM transactions;
DELETE FROM order_promotions;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM cart_items;
DELETE FROM carts;
DELETE FROM product_variant_images;
DELETE FROM product_variant_style_values;
DELETE FROM product_variants;
DELETE FROM products;
DELETE FROM style_values;
DELETE FROM style_categories;
DELETE FROM styles;
UPDATE categories SET parent_id = NULL;
DELETE FROM categories;
DELETE FROM brands;
DELETE FROM promotions;
DELETE FROM deliveries;
DELETE FROM user_levels;
DELETE FROM otp_tokens;
DELETE FROM users;
DELETE FROM roles;
DELETE FROM order_status;
DELETE FROM payment_methods;

DECLARE @Pwd NVARCHAR(255) = N'$2b$10$Xk3dSampleSaltForStoretim0bQAAEvqfHQIxFtnmU2.0PL/OeoK';

-- ---------- roles ----------
SET IDENTITY_INSERT roles ON;
INSERT INTO roles (id, name) VALUES (1, N'ADMIN');
INSERT INTO roles (id, name) VALUES (2, N'MANAGER');
INSERT INTO roles (id, name) VALUES (3, N'EMPLOYEE');
INSERT INTO roles (id, name) VALUES (4, N'SHIPPER');
INSERT INTO roles (id, name) VALUES (5, N'CUSTOMER');
SET IDENTITY_INSERT roles OFF;

-- ---------- order_status ----------
SET IDENTITY_INSERT order_status ON;
INSERT INTO order_status (id, name) VALUES (1, N'not processed');
INSERT INTO order_status (id, name) VALUES (2, N'confirmed');
INSERT INTO order_status (id, name) VALUES (3, N'picked up');
INSERT INTO order_status (id, name) VALUES (4, N'shipping');
INSERT INTO order_status (id, name) VALUES (5, N'delivered');
INSERT INTO order_status (id, name) VALUES (6, N'cancelled');
INSERT INTO order_status (id, name) VALUES (7, N'returned');
SET IDENTITY_INSERT order_status OFF;

-- ---------- payment_methods ----------
SET IDENTITY_INSERT payment_methods ON;
INSERT INTO payment_methods (id, name) VALUES (1, N'COD');
INSERT INTO payment_methods (id, name) VALUES (2, N'BANK_QR');
INSERT INTO payment_methods (id, name) VALUES (3, N'E_WALLET');
SET IDENTITY_INSERT payment_methods OFF;

-- ---------- user_levels ----------
INSERT INTO user_levels (id, name, min_point, discount, is_deleted, created_at, updated_at) VALUES ('14D0601C-E1A3-5950-B57D-A7E8882BBB8F', N'Đồng', 0, 0, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO user_levels (id, name, min_point, discount, is_deleted, created_at, updated_at) VALUES ('66E0E0CE-9B55-5C0C-ADA1-5B2195D5ED1D', N'Bạc', 1000, 2, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO user_levels (id, name, min_point, discount, is_deleted, created_at, updated_at) VALUES ('2D207D3A-E74E-5008-8C59-BF1AFDFB20E1', N'Vàng', 5000, 5, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO user_levels (id, name, min_point, discount, is_deleted, created_at, updated_at) VALUES ('B8FF1BE6-3C62-50F7-9D32-80FF4CF22417', N'Kim cương', 10000, 8, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');

-- ---------- deliveries ----------
INSERT INTO deliveries (id, name, description, price, is_deleted, created_at, updated_at) VALUES ('FD5F9F32-D80D-5D17-A001-50E22526AB11', N'Giao hàng tiêu chuẩn', N'Giao trong 3-5 ngày làm việc', 30000, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO deliveries (id, name, description, price, is_deleted, created_at, updated_at) VALUES ('262D685E-E350-5EFE-A900-6BF3B9C1A487', N'Giao hàng nhanh', N'Giao trong 1-2 ngày làm việc', 50000, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO deliveries (id, name, description, price, is_deleted, created_at, updated_at) VALUES ('D7598F6C-D4B9-5C5E-BAE0-42814D6832CF', N'Giao hỏa tốc', N'Giao trong vòng 4 giờ nội thành', 80000, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');

-- ---------- brands ----------
SET IDENTITY_INSERT brands ON;
INSERT INTO brands (id, name, slug, logo_url, description, is_active, created_at, updated_at) VALUES (1, N'Apple', N'apple', NULL, N'Thương hiệu công nghệ đến từ Mỹ', 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO brands (id, name, slug, logo_url, description, is_active, created_at, updated_at) VALUES (2, N'Samsung', N'samsung', NULL, N'Tập đoàn điện tử hàng đầu Hàn Quốc', 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO brands (id, name, slug, logo_url, description, is_active, created_at, updated_at) VALUES (3, N'Xiaomi', N'xiaomi', NULL, N'Thiết bị thông minh giá tốt', 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO brands (id, name, slug, logo_url, description, is_active, created_at, updated_at) VALUES (4, N'Sony', N'sony', NULL, N'Thương hiệu điện tử và âm thanh Nhật Bản', 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO brands (id, name, slug, logo_url, description, is_active, created_at, updated_at) VALUES (5, N'Logitech', N'logitech', NULL, N'Phụ kiện máy tính chuyên nghiệp', 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
SET IDENTITY_INSERT brands OFF;

-- ---------- categories (cha trước, con sau) ----------
INSERT INTO categories (id, name, slug, parent_id, image, is_deleted, created_at, updated_at) VALUES ('8BCB53F5-A8A9-58EF-A06A-6F1DFD172897', N'Điện thoại', N'dien-thoai', NULL, NULL, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO categories (id, name, slug, parent_id, image, is_deleted, created_at, updated_at) VALUES ('9AA4CF84-AFB2-5DB5-ADB9-5A2BD31EABA6', N'Laptop', N'laptop', NULL, NULL, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO categories (id, name, slug, parent_id, image, is_deleted, created_at, updated_at) VALUES ('51B2F515-BC29-5B01-AE36-3599969404C7', N'Phụ kiện', N'phu-kien', NULL, NULL, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO categories (id, name, slug, parent_id, image, is_deleted, created_at, updated_at) VALUES ('3A29F5A0-7A82-5EC2-9885-CBBEF80ABA76', N'Tai nghe', N'tai-nghe', '51B2F515-BC29-5B01-AE36-3599969404C7', NULL, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO categories (id, name, slug, parent_id, image, is_deleted, created_at, updated_at) VALUES ('C3F609D8-2FF8-5C05-A2D1-2858CDAFC158', N'Chuột & bàn phím', N'chuot-ban-phim', '51B2F515-BC29-5B01-AE36-3599969404C7', NULL, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');

-- ---------- styles & style_categories ----------
INSERT INTO styles (id, name, is_deleted, created_at, updated_at) VALUES ('DD6B949E-AB83-5BA3-8820-A2D6968996C1', N'Màu sắc', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO styles (id, name, is_deleted, created_at, updated_at) VALUES ('79636413-CA39-5009-A33A-C5D7F2ECA097', N'Dung lượng', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO styles (id, name, is_deleted, created_at, updated_at) VALUES ('FE0D7F95-E34A-548A-99E9-28D784177B81', N'RAM', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO style_categories (style_id, category_id) VALUES ('DD6B949E-AB83-5BA3-8820-A2D6968996C1', '8BCB53F5-A8A9-58EF-A06A-6F1DFD172897');
INSERT INTO style_categories (style_id, category_id) VALUES ('DD6B949E-AB83-5BA3-8820-A2D6968996C1', '9AA4CF84-AFB2-5DB5-ADB9-5A2BD31EABA6');
INSERT INTO style_categories (style_id, category_id) VALUES ('DD6B949E-AB83-5BA3-8820-A2D6968996C1', '3A29F5A0-7A82-5EC2-9885-CBBEF80ABA76');
INSERT INTO style_categories (style_id, category_id) VALUES ('DD6B949E-AB83-5BA3-8820-A2D6968996C1', 'C3F609D8-2FF8-5C05-A2D1-2858CDAFC158');
INSERT INTO style_categories (style_id, category_id) VALUES ('79636413-CA39-5009-A33A-C5D7F2ECA097', '8BCB53F5-A8A9-58EF-A06A-6F1DFD172897');
INSERT INTO style_categories (style_id, category_id) VALUES ('79636413-CA39-5009-A33A-C5D7F2ECA097', '9AA4CF84-AFB2-5DB5-ADB9-5A2BD31EABA6');
INSERT INTO style_categories (style_id, category_id) VALUES ('FE0D7F95-E34A-548A-99E9-28D784177B81', '9AA4CF84-AFB2-5DB5-ADB9-5A2BD31EABA6');

-- ---------- style_values (name UNIQUE toàn bảng) ----------
INSERT INTO style_values (id, name, style_id, is_deleted, created_at, updated_at) VALUES ('59A66403-2776-5ECA-95F6-70FB66DD7A20', N'Đen', 'DD6B949E-AB83-5BA3-8820-A2D6968996C1', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO style_values (id, name, style_id, is_deleted, created_at, updated_at) VALUES ('1E17D8E2-1208-5394-8326-72AF4FD4180E', N'Bạc', 'DD6B949E-AB83-5BA3-8820-A2D6968996C1', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO style_values (id, name, style_id, is_deleted, created_at, updated_at) VALUES ('ADF61633-05C3-58AF-8020-BD0CCD28E7C3', N'Xanh dương', 'DD6B949E-AB83-5BA3-8820-A2D6968996C1', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO style_values (id, name, style_id, is_deleted, created_at, updated_at) VALUES ('79375C32-EF78-50AA-B763-F6BF1DC4E7C5', N'128GB', '79636413-CA39-5009-A33A-C5D7F2ECA097', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO style_values (id, name, style_id, is_deleted, created_at, updated_at) VALUES ('A3F79B87-80F0-52D7-A2E3-83F2EFE45641', N'256GB', '79636413-CA39-5009-A33A-C5D7F2ECA097', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO style_values (id, name, style_id, is_deleted, created_at, updated_at) VALUES ('CCAEB9E7-03E5-5C1E-A512-ACC49C2059B1', N'512GB', '79636413-CA39-5009-A33A-C5D7F2ECA097', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO style_values (id, name, style_id, is_deleted, created_at, updated_at) VALUES ('8294ADCD-0270-561C-81AB-8BF4C38D6C72', N'8GB RAM', 'FE0D7F95-E34A-548A-99E9-28D784177B81', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO style_values (id, name, style_id, is_deleted, created_at, updated_at) VALUES ('780A90C3-7613-52F9-A6FF-4EBFCD019F0F', N'16GB RAM', 'FE0D7F95-E34A-548A-99E9-28D784177B81', 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');

-- ---------- products ----------
INSERT INTO products (id, name, slug, description, is_active, is_selling, rating, created_at, updated_at, category_id, brand_id) VALUES ('904E50E9-3DE2-5703-85CA-3CBF871B76B4', N'iPhone 15', N'iphone-15', N'iPhone 15 màn hình Dynamic Island, chip A16 Bionic, camera chính 48MP, cổng USB-C.', 1, 1, 5.0, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '8BCB53F5-A8A9-58EF-A06A-6F1DFD172897', 1);
INSERT INTO products (id, name, slug, description, is_active, is_selling, rating, created_at, updated_at, category_id, brand_id) VALUES ('3849A50B-17EE-536B-98F2-25B1B4BFEA70', N'Samsung Galaxy S24', N'samsung-galaxy-s24', N'Galaxy S24 trang bị Galaxy AI, màn hình Dynamic AMOLED 2X 120Hz, chip Exynos 2400.', 1, 1, 5.0, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '8BCB53F5-A8A9-58EF-A06A-6F1DFD172897', 2);
INSERT INTO products (id, name, slug, description, is_active, is_selling, rating, created_at, updated_at, category_id, brand_id) VALUES ('E57A6252-4199-584B-9950-DC8BB0737DCA', N'MacBook Air M2 13 inch', N'macbook-air-m2-13', N'MacBook Air chip M2 mỏng nhẹ, màn hình Liquid Retina 13.6 inch, pin đến 18 giờ.', 1, 1, 5.0, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '9AA4CF84-AFB2-5DB5-ADB9-5A2BD31EABA6', 1);
INSERT INTO products (id, name, slug, description, is_active, is_selling, rating, created_at, updated_at, category_id, brand_id) VALUES ('5F86D16E-6E22-50FC-AAB7-22ADA3628D77', N'Tai nghe Sony WH-1000XM5', N'tai-nghe-sony-wh-1000xm5', N'Tai nghe chống ồn chủ động hàng đầu, pin 30 giờ, kết nối đa điểm.', 1, 1, 4.0, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '3A29F5A0-7A82-5EC2-9885-CBBEF80ABA76', 4);
INSERT INTO products (id, name, slug, description, is_active, is_selling, rating, created_at, updated_at, category_id, brand_id) VALUES ('5FAAB401-7537-5119-AE50-C88CD45ACCCB', N'Chuột Logitech MX Master 3S', N'chuot-logitech-mx-master-3s', N'Chuột không dây cao cấp, cảm biến 8000 DPI, cuộn MagSpeed siêu êm.', 1, 1, 5.0, '2026-09-01T08:00:00', '2026-09-01T08:00:00', 'C3F609D8-2FF8-5C05-A2D1-2858CDAFC158', 5);
INSERT INTO products (id, name, slug, description, is_active, is_selling, rating, created_at, updated_at, category_id, brand_id) VALUES ('AB8A473D-7BAD-5485-82FB-545E5B9460D4', N'Xiaomi Redmi Note 13', N'xiaomi-redmi-note-13', N'Redmi Note 13 màn hình AMOLED 120Hz, camera 108MP, sạc nhanh 33W.', 1, 1, 4.0, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '8BCB53F5-A8A9-58EF-A06A-6F1DFD172897', 3);

-- ---------- product_variants (ảnh rỗng: không có dòng product_variant_images) ----------
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('4490ACFE-BB93-5569-99F9-4B7C06463FD8', N'APL-IP15-BLK-128', 19990000, 18490000, 30, 85, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '904E50E9-3DE2-5703-85CA-3CBF871B76B4');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('27AA6BD0-B14E-5363-B767-71F4B37405D3', N'APL-IP15-BLK-256', 22990000, NULL, 20, 52, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '904E50E9-3DE2-5703-85CA-3CBF871B76B4');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('BDB0FF83-A39E-55BF-B87A-D0EE27C9E086', N'APL-IP15-BLU-128', 19990000, NULL, 25, 41, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '904E50E9-3DE2-5703-85CA-3CBF871B76B4');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('B9743B82-2BB2-5021-800C-AD8528A98169', N'SS-S24-BLK-256', 21990000, 19990000, 18, 44, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '3849A50B-17EE-536B-98F2-25B1B4BFEA70');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('0155DF74-441B-58C1-BEF6-515BD3E4DFB6', N'SS-S24-SIL-256', 21990000, NULL, 15, 29, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '3849A50B-17EE-536B-98F2-25B1B4BFEA70');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('1609E221-5C0A-571C-AE2D-BD53F06CB9D2', N'APL-MBA-M2-8-256', 24990000, 22990000, 10, 33, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', 'E57A6252-4199-584B-9950-DC8BB0737DCA');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('348B82B8-BA05-557F-BB4D-FB1B30C37928', N'APL-MBA-M2-16-512', 32990000, NULL, 6, 14, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', 'E57A6252-4199-584B-9950-DC8BB0737DCA');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('35FE481F-C755-5FD9-BDEC-E72B337EB156', N'SN-XM5-BLK', 8490000, 7490000, 20, 61, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '5F86D16E-6E22-50FC-AAB7-22ADA3628D77');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('9E18047D-6905-5FE8-B9E1-3A0A5B8FE1B6', N'SN-XM5-SIL', 8490000, NULL, 12, 27, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '5F86D16E-6E22-50FC-AAB7-22ADA3628D77');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('78C03913-905A-5864-9F96-B56E244841A8', N'LG-MX3S-BLK', 2490000, NULL, 40, 96, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '5FAAB401-7537-5119-AE50-C88CD45ACCCB');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('605C07AB-FEF9-55FD-8673-8C2FBB21037B', N'LG-MX3S-SIL', 2490000, 2190000, 35, 58, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', '5FAAB401-7537-5119-AE50-C88CD45ACCCB');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('D8EABEA9-08C7-5106-9DEC-64500009B0DD', N'XM-RN13-BLK-128', 4990000, NULL, 50, 130, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', 'AB8A473D-7BAD-5485-82FB-545E5B9460D4');
INSERT INTO product_variants (id, sku, price, promotional_price, quantity, sold, is_active, is_selling, created_at, updated_at, product_id) VALUES ('FA743BE9-2F1C-541F-B99F-54998CD38B78', N'XM-RN13-BLU-256', 5990000, 5490000, 40, 77, 1, 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00', 'AB8A473D-7BAD-5485-82FB-545E5B9460D4');

-- ---------- product_variant_style_values ----------
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('4490ACFE-BB93-5569-99F9-4B7C06463FD8', '59A66403-2776-5ECA-95F6-70FB66DD7A20');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('4490ACFE-BB93-5569-99F9-4B7C06463FD8', '79375C32-EF78-50AA-B763-F6BF1DC4E7C5');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('27AA6BD0-B14E-5363-B767-71F4B37405D3', '59A66403-2776-5ECA-95F6-70FB66DD7A20');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('27AA6BD0-B14E-5363-B767-71F4B37405D3', 'A3F79B87-80F0-52D7-A2E3-83F2EFE45641');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('BDB0FF83-A39E-55BF-B87A-D0EE27C9E086', 'ADF61633-05C3-58AF-8020-BD0CCD28E7C3');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('BDB0FF83-A39E-55BF-B87A-D0EE27C9E086', '79375C32-EF78-50AA-B763-F6BF1DC4E7C5');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('B9743B82-2BB2-5021-800C-AD8528A98169', '59A66403-2776-5ECA-95F6-70FB66DD7A20');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('B9743B82-2BB2-5021-800C-AD8528A98169', 'A3F79B87-80F0-52D7-A2E3-83F2EFE45641');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('0155DF74-441B-58C1-BEF6-515BD3E4DFB6', '1E17D8E2-1208-5394-8326-72AF4FD4180E');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('0155DF74-441B-58C1-BEF6-515BD3E4DFB6', 'A3F79B87-80F0-52D7-A2E3-83F2EFE45641');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('1609E221-5C0A-571C-AE2D-BD53F06CB9D2', '1E17D8E2-1208-5394-8326-72AF4FD4180E');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('1609E221-5C0A-571C-AE2D-BD53F06CB9D2', '8294ADCD-0270-561C-81AB-8BF4C38D6C72');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('1609E221-5C0A-571C-AE2D-BD53F06CB9D2', 'A3F79B87-80F0-52D7-A2E3-83F2EFE45641');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('348B82B8-BA05-557F-BB4D-FB1B30C37928', '1E17D8E2-1208-5394-8326-72AF4FD4180E');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('348B82B8-BA05-557F-BB4D-FB1B30C37928', '780A90C3-7613-52F9-A6FF-4EBFCD019F0F');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('348B82B8-BA05-557F-BB4D-FB1B30C37928', 'CCAEB9E7-03E5-5C1E-A512-ACC49C2059B1');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('35FE481F-C755-5FD9-BDEC-E72B337EB156', '59A66403-2776-5ECA-95F6-70FB66DD7A20');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('9E18047D-6905-5FE8-B9E1-3A0A5B8FE1B6', '1E17D8E2-1208-5394-8326-72AF4FD4180E');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('78C03913-905A-5864-9F96-B56E244841A8', '59A66403-2776-5ECA-95F6-70FB66DD7A20');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('605C07AB-FEF9-55FD-8673-8C2FBB21037B', '1E17D8E2-1208-5394-8326-72AF4FD4180E');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('D8EABEA9-08C7-5106-9DEC-64500009B0DD', '59A66403-2776-5ECA-95F6-70FB66DD7A20');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('D8EABEA9-08C7-5106-9DEC-64500009B0DD', '79375C32-EF78-50AA-B763-F6BF1DC4E7C5');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('FA743BE9-2F1C-541F-B99F-54998CD38B78', 'ADF61633-05C3-58AF-8020-BD0CCD28E7C3');
INSERT INTO product_variant_style_values (variant_id, style_value_id) VALUES ('FA743BE9-2F1C-541F-B99F-54998CD38B78', 'A3F79B87-80F0-52D7-A2E3-83F2EFE45641');

-- ---------- users ----------
INSERT INTO users (id, full_name, slug, id_card, email, phone, is_email_active, is_phone_active, hashed_password, role_id, addresses, avatar, point, e_wallet, created_at, updated_at) VALUES ('56361B35-5C97-594A-9C16-7F5CD7F0C20B', N'Quản Trị Viên', N'quan-tri-vien', N'079200000001', N'admin@store.vn', N'0900000001', 1, 1, @Pwd, 1, NULL, NULL, 0, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO users (id, full_name, slug, id_card, email, phone, is_email_active, is_phone_active, hashed_password, role_id, addresses, avatar, point, e_wallet, created_at, updated_at) VALUES ('5E6BB7FB-ED8E-5086-98B8-62804E4B9A77', N'Trần Quản Lý', N'tran-quan-ly', N'079200000002', N'manager@store.vn', N'0900000002', 1, 1, @Pwd, 2, NULL, NULL, 0, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO users (id, full_name, slug, id_card, email, phone, is_email_active, is_phone_active, hashed_password, role_id, addresses, avatar, point, e_wallet, created_at, updated_at) VALUES ('C044B47C-2459-5BD0-98FB-466414832EE8', N'Lê Nhân Viên', N'le-nhan-vien', N'079200000003', N'employee@store.vn', N'0900000003', 1, 1, @Pwd, 3, NULL, NULL, 0, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO users (id, full_name, slug, id_card, email, phone, is_email_active, is_phone_active, hashed_password, role_id, addresses, avatar, point, e_wallet, created_at, updated_at) VALUES ('788222C8-C218-535B-AE2B-9BCDFBAE76D0', N'Phạm Giao Hàng', N'pham-giao-hang', N'079200000004', N'shipper@store.vn', N'0900000004', 1, 1, @Pwd, 4, NULL, NULL, 0, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO users (id, full_name, slug, id_card, email, phone, is_email_active, is_phone_active, hashed_password, role_id, addresses, avatar, point, e_wallet, created_at, updated_at) VALUES ('A18A2651-B894-543E-B7F8-D4D000D76534', N'Nguyễn Văn An', N'nguyen-van-an', NULL, N'an.nguyen@gmail.com', N'0911111111', 1, 1, @Pwd, 5, N'12 Nguyễn Huệ, Quận 1, TP.HCM|45 Lê Lợi, Quận 3, TP.HCM', NULL, 1200, 4000000, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO users (id, full_name, slug, id_card, email, phone, is_email_active, is_phone_active, hashed_password, role_id, addresses, avatar, point, e_wallet, created_at, updated_at) VALUES ('94A4CFE0-64E8-58AB-8371-7C471B78343B', N'Trần Thị Bình', N'tran-thi-binh', NULL, N'binh.tran@gmail.com', N'0922222222', 1, 1, @Pwd, 5, N'78 Trần Hưng Đạo, Quận 5, TP.HCM', NULL, 5500, 7460000, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO users (id, full_name, slug, id_card, email, phone, is_email_active, is_phone_active, hashed_password, role_id, addresses, avatar, point, e_wallet, created_at, updated_at) VALUES ('A2CAA840-49A6-534B-A834-B72AE1BF4C45', N'Lê Hoàng Chi', N'le-hoang-chi', NULL, N'chi.le@gmail.com', N'0933333333', 1, 1, @Pwd, 5, N'101 Cách Mạng Tháng 8, Quận 10, TP.HCM|9 Hai Bà Trưng, Quận 1, TP.HCM', NULL, 300, 2000000, '2026-09-01T08:00:00', '2026-09-01T08:00:00');

-- ---------- promotions ----------
INSERT INTO promotions (id, code, description, type, discount_percent, max_discount_amount, min_order_value, quantity, start_date, end_date, is_active, is_deleted, created_at, updated_at) VALUES ('B6BE12AB-7EAA-5788-AF65-083DD4C45E7A', N'WELCOME10', N'Giảm 10% giá trị sản phẩm, tối đa 500.000đ', N'PRODUCT_PERCENT', 10, 500000, 1000000, 100, '2026-01-01T00:00:00', '2026-12-31T23:59:59', 1, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO promotions (id, code, description, type, discount_percent, max_discount_amount, min_order_value, quantity, start_date, end_date, is_active, is_deleted, created_at, updated_at) VALUES ('8210B464-D4C2-579E-B831-CE79160191A0', N'FREESHIP', N'Giảm 100% phí vận chuyển (tối đa 50.000đ) cho đơn từ 500.000đ', N'SHIPPING_PERCENT', 100, 50000, 500000, 200, '2026-01-01T00:00:00', '2026-12-31T23:59:59', 1, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO promotions (id, code, description, type, discount_percent, max_discount_amount, min_order_value, quantity, start_date, end_date, is_active, is_deleted, created_at, updated_at) VALUES ('EDC01822-AA9A-5FF4-8AA5-A10FD0B00860', N'TET2026', N'Giảm 5% sản phẩm - đã hết hạn (dữ liệu test)', N'PRODUCT_PERCENT', 5, NULL, 0, 0, '2026-01-01T00:00:00', '2026-02-28T23:59:59', 0, 0, '2026-09-01T08:00:00', '2026-09-01T08:00:00');

-- ---------- carts & cart_items ----------
INSERT INTO carts (id, user_id, created_at, updated_at) VALUES ('FD70AB30-FC58-5516-8C24-2342D53C6564', 'A18A2651-B894-543E-B7F8-D4D000D76534', '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO cart_items (id, cart_id, variant_id, count, created_at, updated_at) VALUES ('70C2866B-68EF-5E2A-93B4-020DDBA87018', 'FD70AB30-FC58-5516-8C24-2342D53C6564', '0155DF74-441B-58C1-BEF6-515BD3E4DFB6', 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO cart_items (id, cart_id, variant_id, count, created_at, updated_at) VALUES ('5F991BF6-54E6-5338-B362-D21C2224876E', 'FD70AB30-FC58-5516-8C24-2342D53C6564', '78C03913-905A-5864-9F96-B56E244841A8', 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO carts (id, user_id, created_at, updated_at) VALUES ('B057EAE0-6770-581F-B565-4E8031B9C487', '94A4CFE0-64E8-58AB-8371-7C471B78343B', '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO cart_items (id, cart_id, variant_id, count, created_at, updated_at) VALUES ('34113B5D-0D22-501D-B794-FB0FB7B51C60', 'B057EAE0-6770-581F-B565-4E8031B9C487', 'BDB0FF83-A39E-55BF-B87A-D0EE27C9E086', 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO carts (id, user_id, created_at, updated_at) VALUES ('203D9537-A0A1-5FE0-90AE-1D30A32BD6BC', 'A2CAA840-49A6-534B-A834-B72AE1BF4C45', '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO cart_items (id, cart_id, variant_id, count, created_at, updated_at) VALUES ('0C4EC296-04C5-57DC-A175-7A74FBAED450', '203D9537-A0A1-5FE0-90AE-1D30A32BD6BC', '348B82B8-BA05-557F-BB4D-FB1B30C37928', 1, '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO cart_items (id, cart_id, variant_id, count, created_at, updated_at) VALUES ('8BFA70A3-A702-5C61-BB66-F5B2939BAA1A', '203D9537-A0A1-5FE0-90AE-1D30A32BD6BC', 'D8EABEA9-08C7-5106-9DEC-64500009B0DD', 2, '2026-09-01T08:00:00', '2026-09-01T08:00:00');

-- ---------- orders / order_items / order_promotions ----------
INSERT INTO orders (id, user_id, delivery_id, address, phone, status_id, is_paid_before, amount_from_user, payment_id, created_at, updated_at) VALUES ('568FE826-2C03-5DFA-AEA1-935B1BDC1305', 'A18A2651-B894-543E-B7F8-D4D000D76534', 'FD5F9F32-D80D-5D17-A001-50E22526AB11', N'12 Nguyễn Huệ, Quận 1, TP.HCM', N'0911111111', 5, 0, 20180000, 1, '2026-09-05T10:15:00', '2026-09-05T10:15:00');
INSERT INTO order_items (id, order_id, variant_id, count, created_at, updated_at) VALUES ('C1BE294F-003C-5C2A-A543-322A6814AA90', '568FE826-2C03-5DFA-AEA1-935B1BDC1305', '4490ACFE-BB93-5569-99F9-4B7C06463FD8', 1, '2026-09-05T10:15:00', '2026-09-05T10:15:00');
INSERT INTO order_items (id, order_id, variant_id, count, created_at, updated_at) VALUES ('60562756-AA6F-568D-8B83-25081199F725', '568FE826-2C03-5DFA-AEA1-935B1BDC1305', '605C07AB-FEF9-55FD-8673-8C2FBB21037B', 1, '2026-09-05T10:15:00', '2026-09-05T10:15:00');
INSERT INTO order_promotions (order_id, promotion_id) VALUES ('568FE826-2C03-5DFA-AEA1-935B1BDC1305', 'B6BE12AB-7EAA-5788-AF65-083DD4C45E7A');
INSERT INTO order_promotions (order_id, promotion_id) VALUES ('568FE826-2C03-5DFA-AEA1-935B1BDC1305', '8210B464-D4C2-579E-B831-CE79160191A0');
INSERT INTO orders (id, user_id, delivery_id, address, phone, status_id, is_paid_before, amount_from_user, payment_id, created_at, updated_at) VALUES ('0C32A0CB-F5C2-5CC0-8F8B-E83648940EF6', 'A18A2651-B894-543E-B7F8-D4D000D76534', 'D7598F6C-D4B9-5C5E-BAE0-42814D6832CF', N'45 Lê Lợi, Quận 3, TP.HCM', N'0911111111', 4, 1, 7570000, 2, '2026-10-05T14:20:00', '2026-10-05T14:20:00');
INSERT INTO order_items (id, order_id, variant_id, count, created_at, updated_at) VALUES ('F428F8B0-BD8F-5D6D-804C-3EE6EEA2D84D', '0C32A0CB-F5C2-5CC0-8F8B-E83648940EF6', '35FE481F-C755-5FD9-BDEC-E72B337EB156', 1, '2026-10-05T14:20:00', '2026-10-05T14:20:00');
INSERT INTO orders (id, user_id, delivery_id, address, phone, status_id, is_paid_before, amount_from_user, payment_id, created_at, updated_at) VALUES ('BC1D2199-B0C5-5F80-8108-AC89C81F8294', '94A4CFE0-64E8-58AB-8371-7C471B78343B', '262D685E-E350-5EFE-A900-6BF3B9C1A487', N'78 Trần Hưng Đạo, Quận 5, TP.HCM', N'0922222222', 5, 1, 22540000, 3, '2026-09-12T09:00:00', '2026-09-12T09:00:00');
INSERT INTO order_items (id, order_id, variant_id, count, created_at, updated_at) VALUES ('D3D38DCA-4DF6-5DC5-B255-BA5BEC977F11', 'BC1D2199-B0C5-5F80-8108-AC89C81F8294', '1609E221-5C0A-571C-AE2D-BD53F06CB9D2', 1, '2026-09-12T09:00:00', '2026-09-12T09:00:00');
INSERT INTO order_promotions (order_id, promotion_id) VALUES ('BC1D2199-B0C5-5F80-8108-AC89C81F8294', 'B6BE12AB-7EAA-5788-AF65-083DD4C45E7A');
INSERT INTO orders (id, user_id, delivery_id, address, phone, status_id, is_paid_before, amount_from_user, payment_id, created_at, updated_at) VALUES ('7F658108-54CF-5DEF-B16E-65BA5F9E7848', '94A4CFE0-64E8-58AB-8371-7C471B78343B', 'FD5F9F32-D80D-5D17-A001-50E22526AB11', N'78 Trần Hưng Đạo, Quận 5, TP.HCM', N'0922222222', 1, 0, 10010000, 1, '2026-10-07T08:30:00', '2026-10-07T08:30:00');
INSERT INTO order_items (id, order_id, variant_id, count, created_at, updated_at) VALUES ('FD656075-9BF8-59DF-AC7B-D46667CEA926', '7F658108-54CF-5DEF-B16E-65BA5F9E7848', 'D8EABEA9-08C7-5106-9DEC-64500009B0DD', 2, '2026-10-07T08:30:00', '2026-10-07T08:30:00');
INSERT INTO orders (id, user_id, delivery_id, address, phone, status_id, is_paid_before, amount_from_user, payment_id, created_at, updated_at) VALUES ('936C50C7-A670-5121-97CD-27C081C6A1D9', 'A2CAA840-49A6-534B-A834-B72AE1BF4C45', 'FD5F9F32-D80D-5D17-A001-50E22526AB11', N'9 Hai Bà Trưng, Quận 1, TP.HCM', N'0933333333', 6, 0, 20020000, 1, '2026-09-20T19:45:00', '2026-09-20T19:45:00');
INSERT INTO order_items (id, order_id, variant_id, count, created_at, updated_at) VALUES ('936A8DBB-B43A-54FF-878B-FBC092A1B8B7', '936C50C7-A670-5121-97CD-27C081C6A1D9', 'B9743B82-2BB2-5021-800C-AD8528A98169', 1, '2026-09-20T19:45:00', '2026-09-20T19:45:00');
INSERT INTO orders (id, user_id, delivery_id, address, phone, status_id, is_paid_before, amount_from_user, payment_id, created_at, updated_at) VALUES ('9248CAE1-9BA1-5499-9AAC-03564FC591F6', 'A2CAA840-49A6-534B-A834-B72AE1BF4C45', '262D685E-E350-5EFE-A900-6BF3B9C1A487', N'101 Cách Mạng Tháng 8, Quận 10, TP.HCM', N'0933333333', 5, 1, 13980000, 2, '2026-09-25T16:10:00', '2026-09-25T16:10:00');
INSERT INTO order_items (id, order_id, variant_id, count, created_at, updated_at) VALUES ('F1A5A5EB-F05E-54E1-A8B2-76A4FBB9CC17', '9248CAE1-9BA1-5499-9AAC-03564FC591F6', '9E18047D-6905-5FE8-B9E1-3A0A5B8FE1B6', 1, '2026-09-25T16:10:00', '2026-09-25T16:10:00');
INSERT INTO order_items (id, order_id, variant_id, count, created_at, updated_at) VALUES ('B73F6C49-0A31-517B-AF90-A525C9F55CF2', '9248CAE1-9BA1-5499-9AAC-03564FC591F6', 'FA743BE9-2F1C-541F-B99F-54998CD38B78', 1, '2026-09-25T16:10:00', '2026-09-25T16:10:00');
INSERT INTO order_promotions (order_id, promotion_id) VALUES ('9248CAE1-9BA1-5499-9AAC-03564FC591F6', '8210B464-D4C2-579E-B831-CE79160191A0');

-- ---------- reviews ----------
INSERT INTO reviews (id, user_id, product_id, order_id, content, stars, created_at, updated_at) VALUES ('8369CFE5-1845-5A78-8CD5-DCF654E482F4', 'A18A2651-B894-543E-B7F8-D4D000D76534', '904E50E9-3DE2-5703-85CA-3CBF871B76B4', '568FE826-2C03-5DFA-AEA1-935B1BDC1305', N'Máy mượt, camera đẹp, pin dùng cả ngày.', 5, '2026-10-01T12:00:00', '2026-10-01T12:00:00');
INSERT INTO reviews (id, user_id, product_id, order_id, content, stars, created_at, updated_at) VALUES ('E9E91FE0-EDC1-5612-AC66-89CA6E0B8D91', 'A18A2651-B894-543E-B7F8-D4D000D76534', '5FAAB401-7537-5119-AE50-C88CD45ACCCB', '568FE826-2C03-5DFA-AEA1-935B1BDC1305', N'Chuột êm, cuộn nhanh, rất hợp làm việc văn phòng.', 5, '2026-10-01T12:00:00', '2026-10-01T12:00:00');
INSERT INTO reviews (id, user_id, product_id, order_id, content, stars, created_at, updated_at) VALUES ('150E3CA7-92AE-596D-88DB-1F38D930198E', '94A4CFE0-64E8-58AB-8371-7C471B78343B', 'E57A6252-4199-584B-9950-DC8BB0737DCA', 'BC1D2199-B0C5-5F80-8108-AC89C81F8294', N'MacBook mỏng nhẹ, pin trâu, chạy rất mượt.', 5, '2026-10-01T12:00:00', '2026-10-01T12:00:00');
INSERT INTO reviews (id, user_id, product_id, order_id, content, stars, created_at, updated_at) VALUES ('69CF9DD6-26C3-51BF-9315-368954157EBB', 'A2CAA840-49A6-534B-A834-B72AE1BF4C45', '5F86D16E-6E22-50FC-AAB7-22ADA3628D77', '9248CAE1-9BA1-5499-9AAC-03564FC591F6', N'Chống ồn tốt, âm thanh hay nhưng hơi đắt.', 4, '2026-10-01T12:00:00', '2026-10-01T12:00:00');
INSERT INTO reviews (id, user_id, product_id, order_id, content, stars, created_at, updated_at) VALUES ('640989ED-DAF2-52C2-9EAE-CBA1F0FDD963', 'A2CAA840-49A6-534B-A834-B72AE1BF4C45', 'AB8A473D-7BAD-5485-82FB-545E5B9460D4', '9248CAE1-9BA1-5499-9AAC-03564FC591F6', N'Giá rẻ mà màn hình đẹp, dùng ổn.', 4, '2026-10-01T12:00:00', '2026-10-01T12:00:00');

-- ---------- user_follow_products ----------
INSERT INTO user_follow_products (id, user_id, product_id, created_at, updated_at) VALUES ('64DCBFC6-9BC1-54CA-8714-37013C427FA0', 'A18A2651-B894-543E-B7F8-D4D000D76534', '3849A50B-17EE-536B-98F2-25B1B4BFEA70', '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO user_follow_products (id, user_id, product_id, created_at, updated_at) VALUES ('FC1D0CE3-3138-5799-B5C5-4B7F3E70750F', 'A18A2651-B894-543E-B7F8-D4D000D76534', 'E57A6252-4199-584B-9950-DC8BB0737DCA', '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO user_follow_products (id, user_id, product_id, created_at, updated_at) VALUES ('D8536CE0-8E05-5786-9742-842D77AF75A5', '94A4CFE0-64E8-58AB-8371-7C471B78343B', '904E50E9-3DE2-5703-85CA-3CBF871B76B4', '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO user_follow_products (id, user_id, product_id, created_at, updated_at) VALUES ('25907041-40F6-5549-9E1A-10811A66F1AA', 'A2CAA840-49A6-534B-A834-B72AE1BF4C45', '5F86D16E-6E22-50FC-AAB7-22ADA3628D77', '2026-09-01T08:00:00', '2026-09-01T08:00:00');
INSERT INTO user_follow_products (id, user_id, product_id, created_at, updated_at) VALUES ('13A3861A-5E40-5139-A27C-8AE20D5169E7', 'A2CAA840-49A6-534B-A834-B72AE1BF4C45', '5FAAB401-7537-5119-AE50-C88CD45ACCCB', '2026-09-01T08:00:00', '2026-09-01T08:00:00');

-- ---------- transactions ----------
INSERT INTO transactions (id, user_id, is_up, amount, created_at, updated_at) VALUES ('2BA538BC-1C3A-5F19-843E-0224AB5CF06F', 'A18A2651-B894-543E-B7F8-D4D000D76534', 1, 5000000, '2026-09-02T09:00:00', '2026-09-02T09:00:00');
INSERT INTO transactions (id, user_id, is_up, amount, created_at, updated_at) VALUES ('2B75836A-AD1E-5C25-9532-C1E266450526', 'A18A2651-B894-543E-B7F8-D4D000D76534', 0, 1000000, '2026-09-03T09:00:00', '2026-09-03T09:00:00');
INSERT INTO transactions (id, user_id, is_up, amount, created_at, updated_at) VALUES ('F8E7DC6B-D517-50F8-ABDB-EA8CAD6D7817', '94A4CFE0-64E8-58AB-8371-7C471B78343B', 1, 30000000, '2026-09-10T08:00:00', '2026-09-10T08:00:00');
INSERT INTO transactions (id, user_id, is_up, amount, created_at, updated_at) VALUES ('812C394F-A365-5772-A9B8-809D1E94E6D9', '94A4CFE0-64E8-58AB-8371-7C471B78343B', 0, 22540000, '2026-09-12T09:00:00', '2026-09-12T09:00:00');
INSERT INTO transactions (id, user_id, is_up, amount, created_at, updated_at) VALUES ('C444856A-3D8E-5262-A56D-AB826CE867D2', 'A2CAA840-49A6-534B-A834-B72AE1BF4C45', 1, 2000000, '2026-09-18T11:00:00', '2026-09-18T11:00:00');

-- ---------- otp_tokens (otp_hash là giá trị giả, chỉ để có dữ liệu mẫu) ----------
INSERT INTO otp_tokens (id, email, otp_hash, type, expires_at, attempts, used, created_at) VALUES ('C22D2777-2F80-50C2-8195-03099253A7E8', N'newuser@gmail.com', N'$2b$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', N'REGISTER', '2026-10-07T09:10:00', 0, 0, '2026-10-07T09:00:00');
INSERT INTO otp_tokens (id, email, otp_hash, type, expires_at, attempts, used, created_at) VALUES ('EA17503A-C202-5545-BAC1-E11D13F67470', N'an.nguyen@gmail.com', N'$2b$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', N'FORGOT_PASSWORD', '2026-10-06T10:10:00', 1, 1, '2026-10-06T10:00:00');

COMMIT;
PRINT N'Đã nạp xong dữ liệu mẫu.';