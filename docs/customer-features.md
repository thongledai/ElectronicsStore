# Các Tính năng Khách hàng (Customer Features)

## 1. Giỏ hàng (Shopping Cart)

- **Lưu trữ giỏ hàng**: Giỏ hàng của khách hàng được liên kết trực tiếp với tài khoản và lưu trong cơ sở dữ liệu (Database-backed cart), đảm bảo không bị mất khi đổi thiết bị hoặc đăng xuất.
- **Thao tác**:
  - Thêm sản phẩm vào giỏ kèm biến thể (màu sắc, cấu hình dung lượng, RAM, SSD).
  - Tăng/giảm số lượng sản phẩm (tự động kiểm tra số lượng tồn kho khả dụng).
  - Xóa sản phẩm khỏi giỏ hàng.
  - Tự động tính toán tổng giá trị tạm tính.
- **Endpoint**:
  - `GET /cart` hoặc `/api/cart`: Lấy danh sách sản phẩm trong giỏ.
  - `POST /api/cart/add`: Thêm sản phẩm vào giỏ.
  - `PUT /api/cart/update`: Cập nhật số lượng.
  - `DELETE /api/cart/remove/{itemId}`: Xóa sản phẩm.

---

## 2. Danh sách yêu thích (Wishlist)

- Giúp khách hàng lưu lại các sản phẩm quan tâm để dễ dàng theo dõi biến động giá hoặc mua lại sau này.
- **Endpoint**:
  - `GET /customer/wishlist`: Màn hình danh sách sản phẩm yêu thích.
  - `GET /api/wishlist`: Lấy danh sách ID sản phẩm yêu thích của người dùng hiện tại.
  - `POST /api/wishlist/toggle/{productId}`: Thêm hoặc gỡ sản phẩm khỏi danh sách yêu thích.

---

## 3. Đánh giá & Bình luận sản phẩm (Product Reviews & Ratings)

- **Xem đánh giá (Công khai)**: Bất kỳ ai cũng có thể xem điểm đánh giá trung bình (1 đến 5 sao) và các bình luận của khách hàng khác tại trang chi tiết sản phẩm `/customer/product-details`.
- **Gửi đánh giá (Khách hàng đã mua hàng)**:
  - Khách hàng đã mua và nhận sản phẩm thành công có quyền viết đánh giá.
  - Hỗ trợ chọn số sao (1-5 sao), tiêu đề, nội dung nhận xét và đính kèm hình ảnh thực tế.
- **Endpoint**:
  - `GET /api/customer/reviews/product/{productId}`: Lấy danh sách đánh giá công khai kèm phân trang.
  - `POST /api/customer/reviews`: Gửi đánh giá mới (yêu cầu quyền `CUSTOMER`).
  - `PUT /api/customer/reviews/{id}`: Chỉnh sửa đánh giá của chính mình.
  - `DELETE /api/customer/reviews/{id}`: Xóa đánh giá của chính mình.

---

## 4. Quản lý Hồ sơ cá nhân (User Profile)

- Xem và cập nhật thông tin cá nhân: Họ tên, số điện thoại, ngày sinh, địa chỉ mặc định.
- Tải lên ảnh đại diện (Avatar) tích hợp lưu trữ đám mây qua **Cloudinary**.
- Đổi mật khẩu tài khoản (dành cho tài khoản đăng ký truyền thống).
- Thiết lập mật khẩu mới (dành cho tài khoản ban đầu đăng nhập qua Google OAuth2 chưa có mật khẩu).
- **Endpoint**:
  - `GET /customer/profile` hoặc `/profile`: Giao diện thông tin tài khoản.
  - `GET /api/profile`: Lấy thông tin JSON hồ sơ cá nhân.
  - `PUT /api/profile`: Cập nhật thông tin cá nhân.
  - `POST /api/profile/avatar`: Tải lên ảnh đại diện (Multipart file lên Cloudinary).
  - `POST /auth/reset-password`: Đổi mật khẩu.

---

## 5. Hỗ trợ khách hàng trực tuyến (Live Chat via WebSocket)

- Khách hàng có thể mở khung chat trên web để trao đổi trực tiếp với nhân viên hỗ trợ.
- Tích hợp **Spring WebSocket (`/ws` và `/ws-direct`)** để truyền tải tin nhắn hai chiều theo thời gian thực (Real-time).
- Lưu trữ lịch sử tin nhắn để người dùng xem lại khi cần thiết qua `/api/customer/chat/**`.

---

## 6. Mã nguồn liên quan

- Giao diện giỏ hàng: `src/main/resources/templates/customer/cart.html`
- Giao diện danh sách yêu thích: `src/main/resources/templates/customer/wishlist.html`
- Giao diện chi tiết sản phẩm & đánh giá: `src/main/resources/templates/customer/product-details.html`
- Giao diện hồ sơ: `src/main/resources/templates/customer/profile.html`
- Controller giỏ hàng & wishlist: `CartControllerAPI.java`, `WishlistControllerAPI.java`
- Controller đánh giá: `ReviewControllerAPI.java`
- Cấu hình WebSocket: `src/main/java/com/example/store/config/WebSocketConfig.java` (nếu có)
