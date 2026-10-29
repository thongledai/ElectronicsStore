# Tài liệu Kỹ thuật UTE Shop (ElectronicsStore)

Hệ thống tài liệu chi tiết về toàn bộ kiến trúc, chức năng nghiệp vụ và các cổng tích hợp bên thứ ba của dự án **ElectronicsStore (UTE Shop)**.

---

## 📑 Danh mục Tài liệu

### 1. Phân hệ Khách hàng & Mua sắm (Customer Experience)
| Tài liệu | Nội dung chi tiết |
|---|---|
| [Tìm kiếm & Lọc sản phẩm](./product-search-filter.md) | Tìm kiếm từ khóa, bộ lọc đa thuộc tính (giá, thương hiệu, danh mục), sắp xếp và phân trang danh sách sản phẩm. |
| [Tính năng Khách hàng](./customer-features.md) | Quản lý Giỏ hàng (Cart), Danh sách yêu thích (Wishlist), Đánh giá & bình luận sản phẩm (Reviews), Quản lý hồ sơ (Profile) và Live Chat qua WebSocket. |
| [Mã giảm giá & Khuyến mãi](./vouchers-promotions.md) | Cơ chế kiểm tra, tính toán mức giảm và áp dụng Voucher tại màn hình Checkout. |

---

### 2. Thanh toán & Vận chuyển (Payment & Logistics)
| Tài liệu | Nội dung chi tiết |
|---|---|
| [Thanh toán Chuyển khoản VietQR](./online_payment.md) | Tạo mã QR động VietQR QuickLink, xác minh tiền vào tự động qua SePay Webhook và đối soát giao dịch. |
| [Ví điện tử E-Wallet](./e_wallet.md) | Quản lý số dư, tạo mã nạp tiền tự chọn số tiền qua SePay, thanh toán đơn hàng bằng số dư ví an toàn với khóa bi quan (Pessimistic Lock). |
| [Giao hàng GHN Express](./ghn-delivery.md) | Đồng bộ địa chỉ hành chính GHN (Tỉnh/Thành, Phường/Xã), tính phí vận chuyển và quy trình nhân viên tạo vận đơn GHN. |

---

### 3. Quản trị & Nghiệp vụ Nhân viên (Admin & Manager Operations)
| Tài liệu | Nội dung chi tiết |
|---|---|
| [Quản lý Đơn hàng & Vòng đời Đơn](./order-management.md) | Quy trình xử lý đơn hàng từ lúc đặt, xác nhận, đóng gói, bàn giao vận chuyển GHN đến khi hoàn tất hoặc hủy đơn. |
| [CRUD Quản trị Catalog](./manager-crud.md) | Thao tác quản lý Sản phẩm, Biến thể (Variants), Thuộc tính (Styles/Values), Danh mục, Thương hiệu và Hình ảnh Cloudinary dành cho Quản lý. |
| [Quản lý Khuyến mãi Quản trị](./vouchers-promotions.md) | Thiết lập chương trình khuyến mãi, tạo mã giảm giá theo % hoặc tiền mặt, cấu hình hạn mức và thời gian hiệu lực. |

---

### 4. Kiến trúc Hệ thống, Bảo mật & Triển khai
| Tài liệu | Nội dung chi tiết |
|---|---|
| [Xác thực & Bảo mật](./auth-security.md) | Kiến trúc Spring Security 6, Stateless JWT trong HttpOnly Cookie, Google OAuth2, gửi OTP qua Brevo Email API và phân quyền RBAC. |
| [Giao diện & Bootstrap Template](./bootstrap-template.md) | Cấu trúc giao diện Bootstrap 5, kiến trúc layout Thymeleaf và nguồn template tham khảo. |
| [Hướng dẫn Triển khai Cloud](./deployment-guide.md) | Hướng dẫn triển khai lên Render (`https://electronicstore-19nw.onrender.com`), MySQL Aiven Cloud, Multi-stage Dockerfile và biến môi trường. |

---

> [!NOTE]
> Mọi API quản trị và nghiệp vụ trong tài liệu đều tuân thủ cơ chế bảo mật phân quyền. Để kiểm thử hoặc tích hợp, vui lòng đảm bảo bạn có JWT Token hợp lệ với vai trò tương ứng (`CUSTOMER`, `EMPLOYEE`, `MANAGER`, `ADMIN`).
