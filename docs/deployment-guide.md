# Hướng dẫn Triển khai (Deployment Guide)

## 1. Môi trường triển khai thực tế

Dự án ElectronicsStore (UTE Shop) đã được đóng gói container hóa và triển khai trực tuyến trên nền tảng Cloud:

- **Live URL**: [https://electronicstore-19nw.onrender.com](https://electronicstore-19nw.onrender.com)
- **Nền tảng Hosting**: [Render.com](https://render.com) (Docker Web Service)
- **Cơ sở dữ liệu Cloud**: [Aiven Cloud MySQL](https://aiven.io) (Managed MySQL with SSL)
- **Kho lưu trữ mã nguồn**: [GitHub - thongledai/ElectronicsStore](https://github.com/thongledai/ElectronicsStore)

---

## 2. Kiến trúc Đóng gói Docker (Multi-stage Build)

Dự án sử dụng file `Dockerfile` với kỹ thuật **Multi-stage build** nhằm tối ưu dung lượng hình ảnh và hạn chế rủi ro bảo mật:

```dockerfile
# Stage 1: Build JAR với Maven & Eclipse Temurin JDK 22
FROM maven:3.9.9-eclipse-temurin-22 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Runtime Environment với JRE 22
FROM eclipse-temurin:22-jre
WORKDIR /app
# Tạo người dùng non-root bảo mật
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080
# Cấu hình bộ nhớ JVM tương thích container (512MB RAM free tier)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 3. Cấu hình Biến môi trường trên Production (Environment Variables)

Khi triển khai trên Render (hoặc bất kỳ PaaS/VPS nào), cần cung cấp các biến môi trường sau trong bảng **Environment Variables**:

| Tên biến | Ví dụ giá trị | Mục đích |
|---|---|---|
| `SERVER_PORT` | `10000` (hoặc `8080`) | Cổng lắng nghe của web server |
| `DB_URL` | `jdbc:mysql://host:port/dbname?sslMode=REQUIRED` | Kết nối MySQL đám mây |
| `DB_USERNAME` | `avnadmin` | Tên đăng nhập database |
| `DB_PASSWORD` | `******` | Mật khẩu database |
| `DDL_AUTO` | `update` (hoặc `validate`) | Chế độ schema Hibernate |
| `SHOW_SQL` | `false` | Tắt in SQL log trên production để tăng tốc |
| `DB_POOL_MAX` | `5` | Tối ưu connection pool cho RAM 512MB |
| `BREVO_API_KEY` | `xkeysib-******` | API Key gửi email OTP |
| `BREVO_SENDER_EMAIL` | `no-reply@technova.com` | Email người gửi OTP |
| `BREVO_SENDER_NAME` | `TechNova UTE` | Tên người gửi hiển thị |
| `GOOGLE_CLIENT_ID` | `****.apps.googleusercontent.com` | Google OAuth2 Client ID |
| `CLOUDINARY_CLOUD_NAME` | `******` | Cloud name Cloudinary |
| `CLOUDINARY_API_KEY` | `******` | API Key Cloudinary |
| `CLOUDINARY_API_SECRET` | `******` | Secret Cloudinary |
| `GHN_BASE_URL` | `https://dev-online-gateway.ghn.vn` | Base URL API GHN |
| `GHN_TOKEN` | `******` | Token xác thực GHN |
| `GHN_SHOP_ID` | `230501` | ID cửa hàng GHN |
| `GHN_SHOP_DISTRICT_ID` | `3695` | ID Quận/Huyện lấy hàng |
| `GHN_SHOP_WARD_ID` | `90742` | ID Phường/Xã lấy hàng |
| `PAYMENT_BANK_BIN` | `970418` | BIN ngân hàng nhận tiền (BIDV) |
| `PAYMENT_BANK_NAME` | `BIDV` | Tên ngân hàng nhận |
| `PAYMENT_ACCOUNT_NUMBER` | `96247SVOF9` | Số tài khoản ngân hàng nhận |
| `PAYMENT_ACCOUNT_NAME` | `LE DAI THONG` | Tên chủ tài khoản in lên QR |
| `PAYMENT_REFERENCE_PREFIX` | `DH` | Tiền tố mã đơn thanh toán |
| `SEPAY_API_KEY` | `******` | Khóa API xác thực webhook SePay |

---

## 4. Cấu hình Webhook SePay cho môi trường Live

Để cơ chế xác nhận thanh toán tự động VietQR và nạp Ví điện tử hoạt động trên môi trường thật:

1. Đăng nhập vào [my.sepay.vn](https://my.sepay.vn).
2. Vào mục **WebHooks** → **Thêm mới Webhook**:
   - **URL Webhook**: `https://electronicstore-19nw.onrender.com/api/webhooks/sepay`
   - **Kiểu chứng thực**: Chọn `API Key`.
   - **API Key**: Điền đúng giá trị đã cấu hình trong `SEPAY_API_KEY`.
   - **Sự kiện**: Chọn "Tiền vào (Transaction In)".
   - **Cấu trúc nhận diện mã**: Tiền tố `DH`, độ dài 8 ký tự.

---

## 5. Khởi chạy bằng Docker Compose (Môi trường Local)

Nếu muốn chạy toàn bộ hệ thống trên máy local qua Docker:

```powershell
# 1. Sao chép và cấu hình file .env
cp .env.example .env

# 2. Khởi chạy container
docker compose up -d --build

# 3. Xem log chạy
docker compose logs -f app
```
Ứng dụng sẽ khả dụng tại `http://localhost:8080`.
