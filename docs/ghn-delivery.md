# Tích hợp giao hàng GHN

## Phạm vi

Ứng dụng tích hợp GHN để tra cứu địa chỉ và tạo vận đơn. Việc xác nhận đơn và tạo vận đơn là thao tác chủ động của nhân viên; ứng dụng không tự tạo vận đơn ngay khi khách đặt hàng hoặc khi nhận thanh toán.

## Luồng xử lý đơn

1. Khách đặt hàng và cung cấp thông tin nhận hàng.
2. Nhân viên kiểm tra đơn rồi xác nhận. Backend chỉ cho xác nhận khi đơn đang ở trạng thái `NOT_PROCESSED`; nếu đơn đã có mã GHN thì không xác nhận lại.
3. Backend kiểm tra trạng thái thanh toán đối với đơn chuyển khoản/ví. Các phương thức trả trước phải ở trạng thái `PAID` và có cờ đã thanh toán trước; thanh toán ví còn cần giao dịch ledger tương ứng.
4. Nhân viên tạo vận đơn GHN từ đơn đã xác nhận. Backend chuyển dữ liệu đơn sang cấu trúc request GHN, có thể nhận thông số cân nặng/kích thước từ form, gọi API tạo đơn và lưu mã vận đơn cùng thời gian giao dự kiến nếu GHN trả về.
5. Giao diện có thể đọc thông tin vận đơn đã tạo để nhân viên theo dõi.

Đơn trả trước gửi `cod_amount = 0` theo mapper vận đơn. Quy tắc này ngăn GHN thu COD lần nữa cho đơn đã thanh toán.

## Endpoint ứng dụng

| Method | Endpoint | Quyền | Mục đích |
|---|---|---|---|
| `GET` | `/address/provinces` | Theo cấu hình bảo mật ứng dụng | Tra cứu danh sách tỉnh/thành từ GHN. |
| `GET` | `/address/wards?provinceId={id}` | Theo cấu hình bảo mật ứng dụng | Tra cứu phường/xã theo tỉnh/thành. |
| `POST` | `/api/employee/orders/{orderId}/confirm` | EMPLOYEE | Xác nhận đơn. |
| `POST` | `/api/employee/orders/{orderId}/ghn-shipment` | EMPLOYEE | Tạo vận đơn; nhận thông số kiện hàng tùy chọn. |
| `GET` | `/api/employee/orders/{orderId}/ghn-shipment` | EMPLOYEE | Đọc trạng thái/thông tin vận đơn lưu trong ứng dụng. |

Đơn đã có mã vận đơn sẽ không được tạo lần nữa; service trả kết quả đánh dấu `alreadyCreated`.

## Cấu hình

Các biến môi trường được ánh xạ trong `src/main/resources/application.properties`:

| Biến | Ý nghĩa |
|---|---|
| `GHN_BASE_URL` | URL API GHN; mặc định dùng gateway môi trường phát triển (`https://dev-online-gateway.ghn.vn`). |
| `GHN_TOKEN` | Token API của GHN. |
| `GHN_SHOP_ID` | Mã shop GHN. |
| `GHN_SHOP_DISTRICT_ID` | Mã quận/huyện lấy hàng. |
| `GHN_SHOP_WARD_ID` | Mã phường/xã lấy hàng. |
| `GHN_WEBHOOK_SECRET` | Khóa cấu hình cho webhook trạng thái GHN nếu webhook được bật. |

Đặt các giá trị thật trong môi trường triển khai/secret store, không commit token hoặc khóa vào Git. Khi chuyển sang production, xác nhận base URL và thông tin shop theo tài khoản GHN thật.

## Giới hạn hiện tại

- `GhnWebhookController` trong nhánh hiện tại đang bị comment, nên không có endpoint webhook GHN hoạt động để đồng bộ thay đổi trạng thái từ GHN vào ứng dụng.
- Màn hình tra cứu vận đơn đọc thông tin ứng dụng đã lưu; không đồng nghĩa với việc gọi GHN để truy vấn tracking mới nhất.
- Việc tạo vận đơn là thủ công qua luồng nhân viên, không tự động theo webhook thanh toán.
- Tích hợp API thật cần cấu hình token, shop và địa chỉ lấy hàng hợp lệ; môi trường development không thay thế kiểm thử production.

## Mã nguồn liên quan

- API nhân viên: `src/main/java/com/example/store/controller/api/employee/OrderShipmentEmployeeControllerAPI.java`
- Tạo vận đơn và kiểm tra điều kiện: `src/main/java/com/example/store/service/delivery/impl/GhnDeliveryServiceImpl.java`
- Gọi API GHN: `src/main/java/com/example/store/service/delivery/GhnApiClient.java`
- Tra cứu địa chỉ: `src/main/java/com/example/store/service/delivery/impl/GhnAddressServiceImpl.java`
- Chuyển đơn thành request GHN: `src/main/java/com/example/store/mapper/GhnOrderMapper.java`
- Cấu hình: `src/main/resources/application.properties`
