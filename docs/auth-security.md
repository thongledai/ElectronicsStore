# Xác thực & Phân quyền (Authentication & Authorization)

## 1. Tổng quan kiến trúc bảo mật

Hệ thống bảo mật của UTE Shop được xây dựng trên nền tảng **Spring Security 6+**, sử dụng kiến trúc **Stateless Authentication** kết hợp **JSON Web Token (JWT)** được lưu trữ an toàn trong **HttpOnly Cookies** (ngăn chặn tấn công XSS) và cơ chế phân quyền theo vai trò (**Role-Based Access Control - RBAC**).

Hệ thống cũng tích hợp **Google OAuth2** để đăng nhập bằng tài khoản Google, và dịch vụ gửi mã xác thực **OTP qua Brevo REST API** khi đăng ký tài khoản hoặc quên mật khẩu.

---

## 2. Các vai trò trong hệ thống (Roles & Authorities)

| Vai trò | Authority | Mô tả & Phạm vi quyền hạn |
|---|---|---|
| **Khách hàng** | `CUSTOMER`, `ROLE_CUSTOMER` | Xem catalog, giỏ hàng, đặt hàng, quản lý đơn cá nhân, ví điện tử, áp dụng voucher, đánh giá sản phẩm, quản lý wishlist, chat hỗ trợ. |
| **Nhân viên** | `EMPLOYEE`, `ROLE_EMPLOYEE` | Quản lý đơn hàng, xác nhận đơn, tạo vận đơn GHN, quản lý khuyến mãi/voucher. |
| **Quản lý** | `MANAGER`, `ROLE_MANAGER` | Toàn quyền quản lý catalog (Sản phẩm, biến thể, hình ảnh, danh mục, thương hiệu, thuộc tính), xem báo cáo doanh thu, quản trị người dùng. |
| **Giao hàng** | `SHIPPER`, `ROLE_SHIPPER` | Truy cập bảng điều khiển giao vận, cập nhật trạng thái chuyến giao hàng. |
| **Quản trị viên** | `ADMIN`, `ROLE_ADMIN` | Quyền tối cao hệ thống, cấu hình toàn bộ phân hệ và tài khoản nhân sự. |

---

## 3. Cơ chế xác thực JWT & Cookie

1. **Access Token**:
   - Thời gian sống: 45 phút (`jwt.expiration=2700000`).
   - Lưu trữ: Cookie `JWT_TOKEN` (HttpOnly, Path `/`).
   - Được kiểm tra tại mỗi request bởi `JwtAuthenticationFilter`.
2. **Refresh Token**:
   - Thời gian sống mặc định: 45 phút sliding window (`jwt.refresh.expiration=2700000`).
   - Thời gian sống khi chọn "Remember Me": 30 ngày (`jwt.refresh.remember-me-expiration=2592000000`).
   - Lưu trữ: Cookie `REFRESH_TOKEN` (HttpOnly, Path `/`).
3. **Đăng xuất (Logout)**:
   - Endpoint: `/logout`
   - Xóa bỏ các cookie `JWT_TOKEN`, `REFRESH_TOKEN`, `JSESSIONID`, vô hiệu hóa SecurityContext và chuyển hướng về `/login?logout=true`.

---

## 4. Luồng xác thực chi tiết

### 4.1. Đăng ký tài khoản & Xác thực OTP
1. Khách hàng gửi thông tin đăng ký (Họ tên, email, số điện thoại, mật khẩu).
2. Backend kiểm tra tính hợp lệ và sự tồn tại của email/SĐT trong DB.
3. Sinh mã OTP ngẫu nhiên 6 chữ số, thời gian hết hạn 10 phút.
4. Gọi **Brevo API v3** (`POST https://api.brevo.com/v3/smtp/email`) gửi email giao diện HTML chuyên nghiệp chứa mã OTP.
5. Khách hàng nhập mã OTP tại màn hình `/verify-otp`.
6. Sau khi mã OTP được xác minh chính xác, tài khoản được kích hoạt trạng thái hoạt động (`isActive = true`).

### 4.2. Đăng nhập bằng tài khoản Google (OAuth2)
1. Khách hàng bấm nút **"Đăng nhập với Google"** trên giao diện.
2. Trình duyệt lấy Google ID Token qua Google Identity Services.
3. Gửi token về `POST /auth/google`.
4. Backend xác thực token với Google API Client (`google.client.id`).
5. Nếu người dùng chưa tồn tại, hệ thống tự động tạo mới tài khoản với vai trò `CUSTOMER` và email từ Google.
6. Cấp phát cặp JWT Token vào HttpOnly Cookies và hoàn tất đăng nhập.
7. Người dùng có thể thiết lập mật khẩu riêng qua trang `/customer/set-password` nếu muốn đăng nhập bằng mật khẩu sau này.

### 4.3. Quên mật khẩu & Đổi mật khẩu
1. Gửi yêu cầu quên mật khẩu đến `POST /auth/forgot-password` kèm email.
2. Hệ thống gửi mã OTP xác nhận đặt lại mật khẩu qua email.
3. Xác minh OTP qua `POST /auth/verify-otp`.
4. Người dùng cập nhật mật khẩu mới qua `POST /auth/reset-password`. Mật khẩu được mã hóa an toàn bằng **BCrypt**.

---

## 5. Danh sách Endpoint xác thực chính

| Method | Endpoint | Quyền | Mục đích |
|---|---|---|---|
| `POST` | `/auth/login` | Công khai | Đăng nhập bằng email/password, nhận JWT cookie |
| `POST` | `/auth/register` | Công khai | Đăng ký tài khoản mới và gửi OTP |
| `POST` | `/auth/google` | Công khai | Đăng nhập bằng Google ID Token |
| `POST` | `/auth/verify-otp` | Công khai | Xác thực mã OTP 6 số |
| `POST` | `/auth/resend-otp` | Công khai | Gửi lại mã OTP mới |
| `POST` | `/auth/forgot-password` | Công khai | Yêu cầu khôi phục mật khẩu |
| `POST` | `/auth/reset-password` | Đã xác thực OTP | Cập nhật mật khẩu mới |
| `POST` | `/auth/refresh-token` | Công khai | Cấp mới Access Token từ Refresh Token |
| `POST` | `/auth/set-password` | Đã đăng nhập (Google) | Đặt mật khẩu lần đầu cho user Google |
| `GET` | `/logout` | Mọi người | Đăng xuất, xóa toàn bộ authentication cookies |

---

## 6. Cấu hình môi trường liên quan (`.env`)

```ini
# Brevo Email API (Gửi OTP)
BREVO_API_KEY=xkeysib-...
BREVO_SENDER_EMAIL=support@technova.com
BREVO_SENDER_NAME=TechNova Store

# Google Client ID (OAuth2)
GOOGLE_CLIENT_ID=xxxxxxxxxxxx-xxxxxxxxxxxxxxxx.apps.googleusercontent.com
```

---

## 7. Mã nguồn liên quan

- Cấu hình bảo mật: `src/main/java/com/example/store/config/SecurityConfig.java`
- Bộ lọc JWT: `src/main/java/com/example/store/filter/JwtAuthenticationFilter.java`
- Dịch vụ xác thực: `src/main/java/com/example/store/service/common/impl/AuthServiceImpl.java`
- Dịch vụ gửi email Brevo: `src/main/java/com/example/store/service/common/impl/EmailServiceImpl.java`
- Controller xác thực: `src/main/java/com/example/store/controller/api/common/AuthControllerAPI.java`
