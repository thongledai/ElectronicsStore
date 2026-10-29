# Quản lý Khuyến mãi & Mã giảm giá (Promotions & Vouchers)

## 1. Tổng quan

Module Khuyến mãi cung cấp khả năng tạo, quản lý và áp dụng mã giảm giá (Vouchers / Coupon codes) trong toàn bộ hệ thống UTE Shop. Mã giảm giá có thể được áp dụng trực tiếp tại màn hình thanh toán (Checkout) để giảm trừ tiền hàng trước khi xác nhận đơn.

---

## 2. Các loại khuyến mãi & Quy tắc nghiệp vụ

Hệ thống hỗ trợ 2 loại hình giảm giá chính:

1. **Giảm theo phần trăm (`PERCENTAGE`)**:
   - Giảm một tỉ lệ phần trăm nhất định trên tổng giá trị hàng (ví dụ: giảm 10%, 20%).
   - Hỗ trợ thiết lập **Mức giảm tối đa (`maxDiscountAmount`)** để kiểm soát chi phí (ví dụ: giảm 10% tối đa 100.000 VNĐ).

2. **Giảm theo số tiền cố định (`FIXED_AMOUNT`)**:
   - Giảm một số tiền xác định trực tiếp trên đơn hàng (ví dụ: giảm 50.000 VNĐ cho đơn từ 500.000 VNĐ).

### Các điều kiện ràng buộc:
- **Giá trị đơn hàng tối thiểu (`minOrderValue`)**: Đơn hàng cần đạt tổng tiền hàng tối thiểu để được áp dụng voucher.
- **Giới hạn số lượt dùng (`usageLimit`)**: Tổng số lần voucher có thể được áp dụng trên toàn hệ thống. Hệ thống tự động theo dõi `usedCount`.
- **Thời gian hiệu lực**: Ngày bắt đầu (`startDate`) và Ngày kết thúc (`endDate`).
- **Trạng thái kích hoạt (`isActive`)**: Có thể bật/tắt thủ công voucher bất cứ lúc nào.
- **Ràng buộc địa chỉ trước khi áp dụng voucher**: Để tính toán chính xác tổng thanh toán và tránh xung đột với phí ship, hệ thống yêu cầu khách hàng nhập thông tin địa chỉ giao hàng trước khi áp dụng voucher.

---

## 3. Quản lý Khuyến mãi phía Quản trị (Employee / Manager / Admin)

Nhân viên hoặc quản trị viên có thể truy cập giao diện quản lý khuyến mãi để:
- Xem danh sách khuyến mãi kèm phân trang, tìm kiếm theo mã/tên, lọc theo loại và trạng thái kích hoạt.
- Thêm mới chương trình khuyến mãi/voucher.
- Chỉnh sửa thông tin chi tiết voucher.
- Bật/tắt trạng thái hoạt động nhanh chóng.
- Xóa voucher không còn sử dụng.

### API Quản lý (`/api/employee/promotions` & `/api/manager/promotions`):

| Method | Endpoint | Quyền | Mục đích |
|---|---|---|---|
| `GET` | `/api/employee/promotions` | EMPLOYEE, MANAGER, ADMIN | Lấy danh sách voucher phân trang, hỗ trợ tìm kiếm và lọc |
| `GET` | `/api/employee/promotions/{id}` | EMPLOYEE, MANAGER, ADMIN | Lấy chi tiết thông tin một voucher |
| `POST` | `/api/employee/promotions` | EMPLOYEE, MANAGER, ADMIN | Tạo mới voucher |
| `PUT` | `/api/employee/promotions/{id}` | EMPLOYEE, MANAGER, ADMIN | Cập nhật thông tin voucher |
| `PATCH` | `/api/employee/promotions/{id}/status` | EMPLOYEE, MANAGER, ADMIN | Bật/tắt trạng thái `isActive` của voucher |
| `DELETE` | `/api/employee/promotions/{id}` | EMPLOYEE, MANAGER, ADMIN | Xóa voucher khỏi hệ thống |

---

## 4. Áp dụng Khuyến mãi phía Khách hàng (Customer Checkout)

Tại màn hình `/customer/checkout`:
1. Khách hàng nhập đầy đủ thông tin giao hàng (Tỉnh/Thành, Phường/Xã, Địa chỉ cụ thể).
2. Ô nhập mã giảm giá được kích hoạt.
3. Khách hàng nhập mã code (ví dụ: `SUMMERSALE10`) và nhấn **Áp dụng**.
4. Trình duyệt gửi request kiểm tra voucher:
   ```http
   POST /api/promotions/check
   Content-Type: application/json

   {
     "code": "SUMMERSALE10",
     "orderValue": 2500000
   }
   ```
5. Backend xác thực:
   - Voucher có tồn tại và đang hoạt động (`isActive = true`) hay không.
   - Thời gian hiện tại có nằm trong khoảng `startDate` - `endDate` không.
   - Đơn hàng có đạt `minOrderValue` không.
   - Voucher đã vượt quá `usageLimit` chưa.
6. Trả về số tiền giảm thực tế (`discountAmount`) và tổng tiền thanh toán mới.
7. Khi đặt hàng thành công, hệ thống tăng `usedCount` của voucher tương ứng.

---

## 5. Mã nguồn liên quan

- Controller phía khách hàng: `src/main/java/com/example/store/controller/api/customer/PromotionControllerAPI.java`
- Controller phía nhân viên/quản lý: `src/main/java/com/example/store/controller/api/employee/PromotionEmployeeControllerAPI.java`
- Service quản lý khuyến mãi: `src/main/java/com/example/store/service/promotion/impl/PromotionManagementServiceImpl.java`
- Service áp dụng & kiểm tra khuyến mãi: `src/main/java/com/example/store/service/promotion/impl/PromotionServiceImpl.java`
- Entity khuyến mãi: `src/main/java/com/example/store/entity/Promotion.java`
- Giao diện Checkout: `src/main/resources/templates/customer/checkout.html` và `src/main/resources/static/js/customer/checkout.js`
