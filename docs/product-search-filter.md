# Tìm kiếm và lọc sản phẩm

## Tổng quan

Trang danh sách sản phẩm cho phép khách tìm và thu hẹp danh sách theo từ khóa, thuộc tính sản phẩm, sau đó sắp xếp và xem kết quả theo từng trang. Bộ lọc và thứ tự sắp xếp được gửi tới backend; trình duyệt không tự tải toàn bộ catalog để lọc.

## Giao diện

Trang `/customer/products` có các điều khiển:

- Từ khóa tìm kiếm.
- Danh mục và thương hiệu.
- Khoảng giá tối thiểu/tối đa (VNĐ).
- Tình trạng còn hàng/hết hàng.
- Mức đánh giá tối thiểu.
- Chỉ sản phẩm đang khuyến mãi.
- Sắp xếp theo mới nhất, cũ nhất, giá tăng/giảm, đánh giá, phổ biến hoặc mức giảm giá.
- Phân trang, mặc định 12 sản phẩm mỗi trang.

Trên màn hình nhỏ, bộ lọc có thể thu gọn. Nút xóa bộ lọc đặt lại điều kiện tìm kiếm; trạng thái bộ lọc được lưu trong query string để có thể tải lại hoặc chia sẻ URL.

## Luồng xử lý

1. JavaScript đọc điều kiện từ form và tạo query cho API.
2. Gửi `GET /api/products` kèm bộ lọc, sắp xếp và số trang.
3. Backend kiểm tra tham số, xử lý tìm kiếm/lọc/sắp xếp ở cơ sở dữ liệu rồi trả danh sách phân trang.
4. Giao diện cập nhật thẻ sản phẩm, số kết quả và điều khiển chuyển trang.

Trang web dùng số trang bắt đầu từ 1 trên URL; API nhận `page` bắt đầu từ 0. Tải trang danh sách mặc định dùng `size=12`; API chấp nhận tối đa 50 sản phẩm mỗi trang.

## Tham số API

| Tham số | Ý nghĩa |
|---|---|
| `q` hoặc `search` | Từ khóa; `q` được ưu tiên khi cả hai cùng có giá trị. |
| `category` | Một hoặc nhiều slug/UUID danh mục. Danh mục cha bao gồm cả các danh mục con. |
| `brand` | Một hoặc nhiều slug/ID thương hiệu. |
| `minPrice`, `maxPrice` | Cận dưới và cận trên của khoảng giá, không âm. |
| `minRating` | Điểm đánh giá tối thiểu từ 0 đến 5. |
| `inStock=true` | Chỉ hiển thị sản phẩm còn hàng. |
| `outOfStock=true` | Chỉ hiển thị sản phẩm hết hàng. |
| `onSale=true` | Chỉ hiển thị sản phẩm đang khuyến mãi. |
| `sort` | Chế độ sắp xếp tương ứng với các lựa chọn trên giao diện. |
| `page`, `size` | Trang (bắt đầu từ 0) và kích thước trang (1–50). |

Ví dụ:

```text
GET /api/products?q=laptop&category=laptop&minPrice=10000000&maxPrice=30000000&inStock=true&page=0&size=12
```

Khi không truyền điều kiện lọc, API trả danh sách sản phẩm công khai theo thứ tự mặc định. Kết quả chỉ chứa sản phẩm/biến thể đang hoạt động và được phép bán theo quy tắc của service.

## Mã nguồn liên quan

- Giao diện và form: `src/main/resources/templates/customer/products.html`
- Gọi API, đồng bộ URL và hiển thị kết quả: `src/main/resources/static/js/customer/product-list.js`
- Endpoint khách hàng: `src/main/java/com/example/store/controller/api/customer/ProductControllerAPI.java`
- Tham số lọc: `src/main/java/com/example/store/dto/product/ProductFilterDTO.java`
- Xử lý truy vấn: `src/main/java/com/example/store/service/product/impl/ProductServiceImpl.java`
