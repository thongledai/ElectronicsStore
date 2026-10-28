# ElectronicsStore

## Giới thiệu

ElectronicsStore là dự án cuối kỳ xây dựng hệ thống cửa hàng điện tử trực tuyến. Dự án mô phỏng các nghiệp vụ chính của một cửa hàng, từ quản lý danh mục và sản phẩm đến đặt hàng, thanh toán và xử lý giao hàng.

Ứng dụng được phát triển theo mô hình web client-server. Backend cung cấp API và xử lý nghiệp vụ; dữ liệu được lưu trữ trong cơ sở dữ liệu. Người dùng được phân quyền theo vai trò để sử dụng các chức năng phù hợp.

## Chức năng chính

- Tài khoản người dùng, đăng nhập và phân quyền.
- Tra cứu, tìm kiếm và lọc sản phẩm.
- Quản lý sản phẩm, danh mục, thương hiệu và các dữ liệu liên quan.
- Giỏ hàng, đặt hàng và theo dõi trạng thái đơn hàng.
- Hỗ trợ thanh toán khi nhận hàng, chuyển khoản VietQR và ví điện tử.
- Tích hợp dịch vụ giao hàng GHN để tạo vận đơn.
- Các chức năng quản lý dành cho nhân viên và quản trị viên.

## Công nghệ sử dụng

- Java và Spring Boot.
- Spring MVC, Spring Data JPA và Spring Security.
- Thymeleaf và JavaScript.
- MySQL.
- Maven.

## Chạy dự án

Yêu cầu Java 22 và cơ sở dữ liệu MySQL. Cấu hình kết nối cơ sở dữ liệu cùng các thông tin dịch vụ cần thiết trong môi trường chạy ứng dụng; không đưa thông tin xác thực hoặc khóa bí mật lên Git.

Trên Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Sau khi ứng dụng khởi động, truy cập <http://localhost:8080>.

Chạy kiểm thử:

```powershell
.\mvnw.cmd test
```

## Tài liệu

Tài liệu chi tiết về một số chức năng và tích hợp được lưu trong thư mục [docs](./docs/README.md).

## Mục đích

Dự án được thực hiện trong khuôn khổ học phần cuối kỳ, nhằm vận dụng kiến thức về phát triển ứng dụng web, cơ sở dữ liệu, bảo mật và thiết kế nghiệp vụ thương mại điện tử.
