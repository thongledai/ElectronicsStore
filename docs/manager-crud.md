# CRUD quản trị

## Phạm vi

Các API quản trị nằm dưới `/api/manager/` và được giới hạn cho quyền Manager. Giao diện quản lý sản phẩm được đặt tại trang manager; dữ liệu được tải và cập nhật qua REST API.

Tài liệu này tập trung vào CRUD sản phẩm và các đối tượng catalog liên quan. Màn hình/API quản lý đơn hàng, thống kê và nhân viên/người dùng có luồng nghiệp vụ riêng, không được xem là CRUD sản phẩm.

## Quản lý sản phẩm

| Thao tác | Method và endpoint | Mô tả |
|---|---|---|
| Danh sách | `GET /api/manager/products` | Tìm kiếm, lọc danh mục/thương hiệu/trạng thái và phân trang. |
| Chi tiết | `GET /api/manager/products/{id}` | Lấy thông tin sản phẩm để xem hoặc mở form chỉnh sửa. |
| Tạo | `POST /api/manager/products` | Tạo sản phẩm từ JSON; dữ liệu được kiểm tra bởi Bean Validation. |
| Cập nhật | `PUT /api/manager/products/{id}` | Cập nhật thông tin sản phẩm. |
| Xóa | `DELETE /api/manager/products/{id}` | Xóa sản phẩm; hệ thống có thể từ chối nếu sản phẩm còn được dữ liệu khác tham chiếu. |
| Khôi phục | `POST /api/manager/products/{id}/restore` | Khôi phục sản phẩm đã xóa mềm. |

Danh sách hỗ trợ các tham số `search`, `categoryId`, `brandId`, `isSelling`, `isActive`, `page` và `size`. Mặc định mỗi trang có 10 dòng; endpoint giới hạn kích thước tối đa 50.

Form sản phẩm gồm tên, danh mục, thương hiệu, mô tả và hai trạng thái độc lập: hoạt động (`isActive`) và đang bán (`isSelling`). Quản lý sản phẩm, biến thể và ảnh biến thể được thực hiện ở các khu vực riêng.

## Biến thể và hình ảnh

Mỗi sản phẩm có thể có nhiều biến thể, bao gồm thuộc tính/giá/tồn kho. API biến thể dùng đường dẫn lồng trong sản phẩm:

| Thao tác | Endpoint |
|---|---|
| Danh sách | `GET /api/manager/products/{productId}/variants` |
| Chi tiết một biến thể | `GET /api/manager/products/{productId}/variants/{variantId}` |
| Tạo | `POST /api/manager/products/{productId}/variants` |
| Cập nhật | `PUT /api/manager/products/{productId}/variants/{variantId}` |
| Xóa | `DELETE /api/manager/products/{productId}/variants/{variantId}` |
| Khôi phục | `POST /api/manager/products/{productId}/variants/{variantId}/restore` |

Quản lý ảnh biến thể:

- `GET /api/manager/products/{productId}/variants/{variantId}/images`
- `POST /api/manager/products/{productId}/variants/{variantId}/images` (multipart)
- `DELETE /api/manager/products/{productId}/variants/{variantId}/images/{imageId}`

## Các dữ liệu quản trị liên quan

Các tài nguyên catalog sau cũng có API tạo, đọc, cập nhật, xóa và khôi phục:

| Tài nguyên | Base endpoint |
|---|---|
| Danh mục | `/api/manager/categories` |
| Thương hiệu | `/api/manager/brands` |
| Thuộc tính kiểu dáng | `/api/manager/styles` |
| Giá trị thuộc tính | `/api/manager/style-values` |

API danh sách/tùy chọn của từng tài nguyên có thể có endpoint con như `/options` hoặc `/by-category/{categoryId}`. Hãy xem controller tương ứng để biết chính xác kiểu dữ liệu request, quyền và biến thể upload multipart.

Người dùng được quản lý qua `/api/manager/users`; API hiện có thao tác lấy danh sách/chi tiết, tạo, cập nhật và xóa. Không suy diễn rằng endpoint người dùng có thao tác khôi phục nếu chưa được cung cấp bởi controller.

## Lưu ý về xóa

- Nút xóa sản phẩm hiện gọi `DELETE /api/manager/products/{id}` (được ghi audit là xóa vĩnh viễn); đây không phải nút chuyển trạng thái bán hàng.
- Danh mục, thương hiệu, biến thể và thuộc tính có thể có luồng xóa mềm/khôi phục tương ứng. Hành vi cụ thể tùy service và ràng buộc dữ liệu.
- Tắt trạng thái “Đang bán” hoặc “Hoạt động” khác với xóa. Dùng trạng thái nếu muốn ẩn/ngừng bán mà vẫn giữ bản ghi.
- Các thay đổi qua manager API cần được thực hiện với JWT/phiên hợp lệ và quyền Manager. Không gửi token trong tài liệu, URL hoặc mã nguồn.

## Mã nguồn liên quan

- Giao diện: `src/main/resources/templates/manager/products.html`
- JavaScript quản lý sản phẩm: `src/main/resources/static/js/manager/products.js`
- API sản phẩm: `src/main/java/com/example/store/controller/api/manager/ProductManagerControllerAPI.java`
- API biến thể và ảnh: `ProductVariantManagerControllerAPI.java`, `ProductVariantImageManagerControllerAPI.java`
- Quy tắc nghiệp vụ sản phẩm: `src/main/java/com/example/store/service/product/impl/ProductServiceImpl.java`
