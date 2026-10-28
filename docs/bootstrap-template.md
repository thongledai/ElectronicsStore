# Giao diện Bootstrap và template tham khảo

## Nguồn tham khảo

Website được đưa ra làm mẫu tham khảo:

- Trang gốc được yêu cầu: <https://krishwa09.github.io/>
- Bản demo TechNova liên quan: <https://krishwa09.github.io/technova-electronics/>

Tại thời điểm viết tài liệu, URL gốc trả về `404`; đường dẫn `/technova-electronics/` vẫn hiển thị demo TechNova. Nội dung demo có phong cách cửa hàng điện tử với danh mục, sản phẩm nổi bật, tìm kiếm/lọc, giỏ hàng và giao diện tương thích thiết bị di động. Đây là nguồn tham khảo giao diện, không phải backend của ứng dụng hiện tại.

## Công nghệ giao diện hiện tại

Ứng dụng hiện tại là ứng dụng Spring Boot sử dụng Thymeleaf cho template phía server, JavaScript cho tương tác trình duyệt và Bootstrap 5 cho lưới, tiện ích, form, modal và thành phần responsive. Bootstrap CSS/JS được nạp từ CDN jsDelivr phiên bản 5.3.3; Font Awesome được nạp từ CDN.

Một số thành phần giao diện:

- Layout dùng chung: `src/main/resources/templates/layouts/customer-layout.html` và `dashboard-layout.html`.
- Phần `<head>` dùng chung và liên kết Bootstrap/CSS: `src/main/resources/templates/fragments/header.html`.
- CSS riêng của dự án: `src/main/resources/static/css/`.
- Các màn hình: `src/main/resources/templates/customer/`, `manager/` và `employee/`.

Các trang và luồng nghiệp vụ trong mã nguồn hiện tại được triển khai riêng bằng Spring Boot/Thymeleaf/API. Không nên coi các chức năng, dữ liệu demo, chính sách bán hàng hoặc thông tin trong website tham khảo là hành vi thật của UTE Shop.

## Lưu ý khi thay đổi hoặc tái sử dụng template

- Giữ Bootstrap và CSS của dự án tách biệt; kiểm tra thứ tự tải CSS nếu điều chỉnh màu sắc, bố cục hoặc responsive để tránh ghi đè không mong muốn.
- Dùng các layout/fragments hiện có thay vì sao chép nguyên trang HTML demo.
- Không sao chép logo, hình ảnh, nội dung, thương hiệu hoặc tài sản bên thứ ba nếu chưa xác nhận quyền sử dụng và giấy phép.
- CDN cần kết nối mạng. Nếu chuyển Bootstrap sang file local, cần cập nhật layout dùng chung và kiểm tra cả customer lẫn dashboard.

## README cũ

README ở thư mục gốc mô tả một bản frontend TechNova tĩnh và còn chứa cấu trúc, tài khoản demo cùng các chi tiết không phản ánh ứng dụng Spring Boot hiện tại. Tài liệu này chỉ ghi nhận liên hệ của URL demo tham khảo; không dùng README cũ làm nguồn chuẩn cho API hoặc nghiệp vụ hiện hành.
