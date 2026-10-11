# TechNova Electronics

Website thương mại điện tử chuyên bán các sản phẩm điện tử giả tưởng, có giao diện tương thích với nhiều kích thước màn hình — được xây dựng bằng HTML5, CSS3, JavaScript thuần và Bootstrap 5.

Đây là dự án **chỉ có frontend**, không sử dụng PHP, MySQL, Node.js hay bất kỳ backend nào. Dữ liệu giỏ hàng và danh sách yêu thích được lưu trong trình duyệt thông qua `localStorage`, vì vậy dữ liệu vẫn được giữ lại sau khi tải lại trang.

## Bản demo trực tuyến

[https://krishwa09.github.io/technova-electronics/](https://krishwa09.github.io/technova-electronics/)

## Các chức năng

**Các trang chính (8 trang)**

| Trang | Chức năng |
|---|---|
| `index.html` | Banner chính, danh mục sản phẩm, sản phẩm nổi bật, bộ đếm ngược ưu đãi trong ngày, sản phẩm bán chạy, đánh giá khách hàng và đăng ký nhận bản tin. |
| `products.html` | Hiển thị toàn bộ 25 sản phẩm, hỗ trợ tìm kiếm, lọc theo danh mục, thương hiệu, giá và đánh giá; sắp xếp, chuyển đổi chế độ xem dạng lưới/danh sách và phân trang. |
| `product-details.html` | Thư viện hình ảnh có chức năng phóng to, các tab mô tả/thông số kỹ thuật/đánh giá, chọn số lượng, thêm vào giỏ hàng và danh sách yêu thích. |
| `cart.html` | Cập nhật số lượng, xóa sản phẩm, áp dụng mã giảm giá, tính tổng tiền hàng + thuế GST 18% + phí vận chuyển và hiển thị tổng kết đơn hàng. |
| `wishlist.html` | Hiển thị sản phẩm đã yêu thích, chuyển từng sản phẩm hoặc toàn bộ sản phẩm sang giỏ hàng và tính tổng giá trị sản phẩm đã lưu. |
| `about.html` | Giới thiệu câu chuyện công ty, thống kê có hiệu ứng động, giá trị cốt lõi, dòng thời gian phát triển, đội ngũ và câu hỏi thường gặp dạng thu gọn/mở rộng. |
| `contact.html` | Biểu mẫu liên hệ có kiểm tra dữ liệu bằng JavaScript, thông tin công ty và bản đồ Google Maps nhúng. |
| `login.html` | Mô phỏng chức năng đăng nhập và đăng ký, có kiểm tra dữ liệu và thanh đánh giá độ mạnh của mật khẩu. |

**Các chức năng JavaScript**

- Tìm kiếm sản phẩm trực tiếp, hiển thị danh sách gợi ý trong thanh tìm kiếm ở Header.
- Lọc sản phẩm theo danh mục, thương hiệu, khoảng giá, đánh giá và tình trạng còn hàng.
- Hỗ trợ 8 chế độ sắp xếp theo giá, đánh giá, mức độ phổ biến, mức giảm giá và tên sản phẩm.
- Giỏ hàng có giới hạn số lượng, tính thuế GST và phí vận chuyển.
- Danh sách yêu thích có chức năng chuyển sản phẩm sang giỏ hàng.
- Mã giảm giá có kiểm tra tính hợp lệ và điều kiện giá trị đơn hàng tối thiểu.
- Hệ thống thông báo dạng Toast.
- Thư viện hình ảnh sản phẩm có chức năng phóng to theo vị trí con trỏ chuột.
- Chuyển đổi giao diện sáng/tối và ghi nhớ lựa chọn của người dùng.
- Nút quay lại đầu trang, thanh tiến trình cuộn trang và hiệu ứng xuất hiện khi cuộn.
- Thanh điều hướng tương thích với nhiều kích thước màn hình, có menu thu gọn trên thiết bị di động.

## Cách chạy dự án

Không cần biên dịch và không cần cài đặt thêm thư viện phụ thuộc. Bạn có thể chạy theo một trong hai cách:

**Cách 1: Mở trực tiếp**

Nhấp đúp vào file `index.html`.

**Cách 2: Chạy máy chủ cục bộ (khuyến nghị)**

Cách này mô phỏng gần hơn môi trường triển khai website thực tế.

```bash
python3 -m http.server 8000
```

Sau đó, mở trình duyệt và truy cập:

[http://localhost:8000](http://localhost:8000)

## Tài khoản demo

Trang đăng nhập chấp nhận một tài khoản được thiết lập cố định:

- **Email:** `demo@technova.com`
- **Mật khẩu:** `Demo@123`

Dự án không có cơ chế xác thực thực sự. Trạng thái “đã đăng nhập” chỉ được lưu dưới dạng tên người dùng trong `localStorage` để Header có thể hiển thị lời chào.

Không sử dụng phương pháp này trong môi trường triển khai thực tế.

## Mã giảm giá

| Mã | Ưu đãi |
|---|---|
| `TECHNOVA10` | Giảm 10% giá trị đơn hàng. |
| `STUDENT15` | Giảm giá 15% dành cho sinh viên. |
| `FLAT500` | Giảm ₹500 với đơn hàng tối thiểu ₹5.000. |
| `FREESHIP` | Miễn phí vận chuyển. |

## Cấu trúc dự án ban đầu

```text
.
├── index.html              Trang chủ
├── products.html           Danh sách sản phẩm
├── product-details.html    Chi tiết sản phẩm
├── cart.html               Giỏ hàng
├── wishlist.html           Danh sách yêu thích
├── about.html              Giới thiệu
├── contact.html            Liên hệ
├── login.html              Đăng nhập demo
├── css/
│   └── style.css            Biến thiết kế, thành phần giao diện,
│                            chế độ tối và giao diện responsive
├── js/
│   ├── data.js              Danh mục 25 sản phẩm — nguồn dữ liệu duy nhất
│   ├── main.js              Chức năng cốt lõi: lưu trữ, giỏ hàng,
│   │                        yêu thích, thông báo, giao diện và UI dùng chung
│   ├── home.js              Các thành phần trên trang chủ
│   ├── products.js          Lọc, sắp xếp và phân trang
│   ├── product-details.js   Thư viện ảnh, tab và đánh giá
│   ├── cart.js              Các dòng sản phẩm trong giỏ, tổng tiền, mã giảm giá
│   ├── wishlist.js          Sản phẩm yêu thích và thao tác hàng loạt
│   ├── about.js             Bộ đếm thống kê có hiệu ứng
│   ├── contact.js           Kiểm tra dữ liệu biểu mẫu liên hệ
│   └── login.js             Kiểm tra dữ liệu đăng nhập demo
└── images/                  90 hình ảnh SVG độc lập
```

## Ghi chú về tài nguyên hình ảnh

Tất cả hình ảnh đều được tạo thủ công dưới dạng SVG và lưu trực tiếp trong kho mã nguồn. Không có hình ảnh nào được tải trực tiếp từ máy chủ hình ảnh bên ngoài.

Nhờ đó, website có thể hiển thị hình ảnh nhất quán ngay cả khi ngoại tuyến và không bị lỗi khi một URL của bên thứ ba ngừng hoạt động.

Bootstrap 5, Font Awesome 6 và phông chữ Poppins được tải từ các CDN công khai, vì vậy cần kết nối mạng để hiển thị đầy đủ giao diện.

## Cách thêm sản phẩm

Thêm một đối tượng vào mảng `PRODUCTS` trong file `js/data.js`.

Sản phẩm sẽ tự động xuất hiện trong danh sách sản phẩm, bộ lọc, chức năng tìm kiếm, số lượng sản phẩm theo danh mục và các khu vực sản phẩm trên trang chủ. Không cần chỉnh sửa các file khác.

## Công nghệ sử dụng

- HTML5: cấu trúc nội dung theo ngữ nghĩa và các nhãn ARIA hỗ trợ khả năng truy cập.
- CSS3: thuộc tính tùy chỉnh, Flexbox, Grid và hiệu ứng chuyển động.
- JavaScript thuần: ES6 trở lên, không sử dụng framework.
- Bootstrap 5.3.
- Font Awesome 6.5.
- Google Fonts: Poppins.

---

Dự án được xây dựng phục vụ mục đích minh họa trong môi trường đại học. Tên sản phẩm, thương hiệu, giá bán, đánh giá và bản thân công ty đều là giả tưởng.

# TechNova Electronics — Cấu trúc giao diện Spring Boot + Thymeleaf

Giao diện hiện là frontend tĩnh (chưa có controller/service/database): dữ liệu trong bảng là dữ liệu mẫu, các nút "Thêm mới", "Xem chi tiết", chọn trạng thái chỉ hiện thông báo minh họa (xem `js/roles.js`).

## Môi trường Java

Ứng dụng Spring Boot yêu cầu Java 25 LTS. Dùng Maven Wrapper để biên dịch và chạy kiểm thử: `mvnw.cmd clean test` trên Windows hoặc `./mvnw clean test` trên macOS/Linux. Docker build và runtime cũng sử dụng Java 25.

## Cấu trúc thư mục

```text
resources/
├── application.properties
├── static/
│   ├── css/style.css          Toàn bộ style: storefront (01–17) + dashboard 3 role (18)
│   ├── js/                    data.js, main.js (+ ROUTES), các script trang, roles.js (dashboard)
│   └── images/
└── templates/
    ├── layouts/
    │   ├── customer-layout.html    Khung trang Customer/Guest (navbar, footer, scripts)
    │   └── dashboard-layout.html   Khung dashboard Manager/Employee/Shipper
    ├── fragments/
    │   ├── header.html             <head> dùng chung cho mọi trang
    │   ├── customer-navbar.html    Thanh thông báo + header (navbar, navbar-minimal)
    │   ├── footer.html             Footer (footer, footer-minimal)
    │   ├── dashboard-topbar.html   Thanh trên cùng của dashboard
    │   └── manager-/employee-/shipper-sidebar.html   Menu riêng từng role
    ├── customer/  index, products, product-details, cart, wishlist
    ├── pages/     about, contact
    ├── auth/      login
    ├── manager/   index, users, products, categories, promotions, carriers, orders, statistics
    ├── employee/  index, orders-new, orders-confirmed, orders-picked, orders-delivering,
    │              orders-delivered, orders-cancelled, orders-returns
    └── shipper/   index, assigned, accepted, delivering, delivered, failed, statistics
```

## Chuẩn đường dẫn Thymeleaf (@{/...})

Dự án sử dụng cú pháp Link Expression chuẩn của Thymeleaf (`@{/...}`) để tự động quản lý Context Path và tài nguyên:

```html
<link th:href="@{/css/style.css}" rel="stylesheet">
<a th:href="@{/customer/products(category='laptops')}">Laptops</a>
<img th:src="@{/images/logo.svg}" alt="">
```

Với JavaScript, `fragments/header.html` đặt `window.APP_URL = /*[[@{/}]]*/ '/';`; `data.js` (`IMG_BASE`) và `main.js` (`ROUTES`) dựng mọi đường dẫn từ đó.

## Typography & Font tiếng Việt

Dự án tích hợp bộ đôi Google Fonts hiện đại **Plus Jakarta Sans** và **Be Vietnam Pro** với đầy đủ bộ glyphs Tiếng Việt, khắc phục triệt để hiện tượng vỡ nét hoặc lỗi dấu tiếng Việt khi hiển thị các sản phẩm và giao diện người dùng.

## Đường dẫn các trang (đặt controller theo bảng này)

| URL | Template |
|---|---|
| `/customer/index`, `/customer/products`, `/customer/product-details?id=`, `/customer/cart`, `/customer/wishlist` | `customer/*` |
| `/pages/about`, `/pages/contact`, `/auth/login` | `pages/*`, `auth/login` |
| `/manager/{index,users,products,categories,promotions,carriers,orders,statistics}` | `manager/*` |
| `/employee/{index,orders-new,orders-confirmed,orders-picked,orders-delivering,orders-delivered,orders-cancelled,orders-returns}` | `employee/*` |
| `/shipper/{index,assigned,accepted,delivering,delivered,failed,statistics}` | `shipper/*` |

Ví dụ controller tối thiểu: `@GetMapping("/manager/{page}") public String manager(@PathVariable String page) { return "manager/" + page; }` (nên giới hạn `page` bằng danh sách hợp lệ, và bảo vệ `/manager/**`, `/employee/**`, `/shipper/**` bằng Spring Security). Cho phép `permitAll` với `/css/**`, `/js/**`, `/images/**`.

## Cách tạo một trang mới

Trang dashboard (nội dung đặt trong `<section class="content">`):

```html
<html lang="vi" th:replace="~{layouts/dashboard-layout :: layout('Tiêu đề', 'manager', 'users', ~{::section.content})}">
<head><title>Tiêu đề | TechNova</title></head>
<body><section class="content"> ... </section></body>
</html>
```

Tham số: tiêu đề, role (`manager`/`employee`/`shipper`), khóa mục menu cần sáng (thêm link tương ứng vào `*-sidebar.html`), nội dung.

Trang Customer (nội dung đặt trong `<main>`):

```html
<html lang="en" th:replace="~{layouts/customer-layout :: layout('Tiêu đề', 'Mô tả', ~{::main}, ~{}, 'ten-file.js', false)}">
<head><title>Tiêu đề</title></head>
<body><main> ... </main></body>
</html>
```

Tham số: tiêu đề, mô tả, nội dung, phần chèn thêm giữa main và footer (vd. `~{::#checkoutModal}` hoặc `~{}`), file JS riêng trong `static/js` (hoặc `''`), `true` để dùng header/footer rút gọn như trang đăng nhập.

## Giao diện dashboard

- Style nằm ở mục 18 của `css/style.css` (không có `roles.css` riêng), tất cả nằm trong `[data-role]` nên không ảnh hưởng storefront; dùng chung biến màu và chế độ sáng/tối (`technova_theme`).
- Máy tính: menu cố định bên trái. Màn hình nhỏ (< 992px): menu thu gọn thành thanh trên cùng có nút mở.
- Mỗi sidebar có nút đổi sáng/tối và nút đăng xuất (form `POST /logout`, `th:action` tự thêm CSRF token của Spring Security).
