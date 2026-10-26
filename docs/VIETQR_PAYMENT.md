# Thanh toán chuyển khoản VietQR (MB Bank 0905928100) + xác nhận tự động

## Cách hoạt động
1. Khách chọn **Chuyển khoản QR (VietQR)** ở checkout → backend tạo đơn `NOT_PROCESSED`, `paymentStatus = UNPAID`,
   mã tham chiếu duy nhất (vd `ESK3M9X2QA`) và hạn thanh toán (mặc định 30 phút, kho được giữ đến hạn này).
2. Backend dựng ảnh QR bằng **VietQR Quick Link** (`img.vietqr.io`) với đúng STK, số tiền **do backend tính** và mã tham chiếu.
   Khách quét QR trong app ngân hàng.
3. VietQR **chỉ tạo ảnh QR**, không báo tiền vào. Việc xác nhận do **SePay** đảm nhiệm: SePay theo dõi tài khoản MB
   và gọi `POST /api/webhooks/sepay` khi có tiền vào.
4. Backend chỉ chuyển `paymentStatus = PAID` (và `isPaidBefore = true`) khi webhook: đúng API key, đúng tài khoản nhận,
   là tiền vào, khớp mã tham chiếu, **số tiền bằng đúng tổng đơn**, và mã giao dịch chưa xử lý trước đó.
5. Đơn PAID mới được nhân viên **Xác nhận** → **Tạo vận đơn GHN** (backend chặn đơn chưa PAID).
   Vận đơn của đơn đã trả trước gửi `cod_amount = 0` (logic `isPaidBefore` có sẵn trong `GhnOrderMapper`).
6. Quá hạn mà chưa có tiền → scheduler hủy đơn, hoàn tồn kho, `paymentStatus = EXPIRED`.

> Muốn hệ thống tự chuyển đơn sang CONFIRMED ngay khi nhận tiền: đặt `PAYMENT_AUTO_CONFIRM_AFTER_PAID=true`
> (mặc định `false` để giữ bước nhân viên xác nhận như quy trình hiện tại). Hệ thống không bao giờ tự tạo vận đơn GHN.

## Biến môi trường (.env)
| Biến | Ví dụ (giá trị giả) | Ghi chú |
|---|---|---|
| `SEPAY_API_KEY` | `thay-bang-khoa-tu-sepay` | **Bắt buộc** để nhận webhook. Để trống = từ chối mọi webhook. Không commit lên Git. |
| `PAYMENT_ACCOUNT_NUMBER` | `0905928100` | STK nhận tiền (mặc định đã là 0905928100) |
| `PAYMENT_BANK_BIN` | `970422` | MB Bank |
| `PAYMENT_ACCOUNT_NAME` | `NGUYEN VAN A` | Tên chủ TK hiển thị trên QR (tùy chọn, bạn tự điền đúng tên) |
| `PAYMENT_REFERENCE_PREFIX` | `ES` | Phải khớp "Cấu trúc mã thanh toán" trên SePay |
| `PAYMENT_EXPIRE_MINUTES` | `30` | Hạn thanh toán / thời gian giữ hàng |
| `PAYMENT_AUTO_CONFIRM_AFTER_PAID` | `false` | Xem ghi chú trên |

## Thiết lập SePay (bắt buộc để xác nhận tiền tự động)
1. Đăng ký tại `my.sepay.vn`, **liên kết tài khoản MB Bank 0905928100** (làm theo hướng dẫn trong SePay).
2. *Công ty → Cấu hình chung → Cấu trúc mã thanh toán*: tiền tố `ES`, độ dài 8, ký tự chữ + số.
3. *WebHooks → Thêm*: 
   - URL: `https://<domain-https-cong-khai>/api/webhooks/sepay`
   - Kiểu chứng thực: **API Key** → đặt cùng giá trị với `SEPAY_API_KEY`
   - Content-Type: `application/json`; chọn đúng tài khoản MB; bật "Là WebHooks xác thực thanh toán".
4. Chạy local: cần URL công khai HTTPS (vd ngrok) trỏ về `localhost:8080`.
5. Thử thật: tạo đơn QR, chuyển khoản đúng số tiền + nội dung, xem đơn chuyển PAID trong vài giây.

## Endpoint
| Method | Đường dẫn | Quyền | Mục đích |
|---|---|---|---|
| POST | `/orders` | CUSTOMER | Đặt hàng (COD hoặc BANK_QR); không đổi so với trước |
| GET | `/orders/{id}/payment` | CUSTOMER (chỉ chủ đơn) | Lấy QR + trạng thái thanh toán (giao diện poll mỗi 4s) |
| POST | `/api/webhooks/sepay` | Header `Authorization: Apikey ...` | Webhook tiền vào của SePay |

## Đối soát
- Mọi giao dịch webhook được lưu ở `bank_transfer_events` (chống trùng theo `provider + provider_transaction_id`).
- Đơn bị đánh dấu `NEEDS_RECONCILIATION` khi: sai số tiền (thiếu/thừa), tiền đến sau khi đơn hết hạn/đã hủy.
  Các đơn này **không** thể xác nhận/tạo vận đơn; nhân viên liên hệ khách, hoàn tiền thủ công hoặc hủy đơn.
- Hệ thống **không** có tính năng tự hoàn tiền và **không** có nút "xác nhận thanh toán" thủ công (cố ý, để tránh gian lận).
- Đơn đã PAID không cho khách tự hủy (cần cửa hàng hoàn tiền thủ công).

## Lưu ý
- Link `img.vietqr.io` là Quick Link công cộng; VietQR khuyến nghị tạo template riêng (my.vietqr.io) cho dự án chính thức, sau đó đổi `PAYMENT_QR_TEMPLATE`.
- GHN webhook controller đang bị comment trong nhánh này; thay đổi này không đụng tới nó.
