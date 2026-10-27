# Ví điện tử — VietQR / SePay

## Phạm vi

- Mỗi Customer sử dụng số dư `users.e_wallet` (entity field `eWallet`); số dư Staff/Admin không được phép nạp hoặc dùng thanh toán.
- Tạo yêu cầu nạp ví tách biệt với đơn hàng, sử dụng cùng cấu hình tài khoản VietQR và mã tham chiếu SePay.
- QR nạp ví **không chứa tham số số tiền**. QR chỉ điền tài khoản nhận và nội dung chuyển khoản có mã tham chiếu.
- Webhook SePay là nguồn xác nhận tiền thực nhận. Frontend không được phép tự cộng số dư hoặc gửi trạng thái thanh toán thành công.
- Ghi lịch sử vào bảng `transactions`: `WALLET_TOP_UP` là tiền vào; `ORDER_PAYMENT` là tiền trừ để thanh toán đơn.
- Giao dịch webhook được ghi tại `bank_transfer_events`; mỗi sự kiện có loại `ORDER_PAYMENT` hoặc `WALLET_TOP_UP` khi xác định được đích.

## Luồng nạp ví

1. Customer gọi `POST /api/wallet/top-ups` để tạo mã tham chiếu nạp ví; không cần nhập số tiền.
2. Backend tạo yêu cầu PENDING và mã tham chiếu duy nhất. QR sử dụng tài khoản nhận đã cấu hình trong `PAYMENT_*` và không có query parameter `amount`.
3. Customer quét QR, tự chọn số tiền chuyển trong ứng dụng ngân hàng và giữ nguyên nội dung chuyển khoản.
4. SePay gọi `POST /api/webhooks/sepay`. Backend xác thực API key, loại giao dịch tiền vào và tài khoản nhận; sau đó dò mã tham chiếu trong `code/content`.
5. Nếu yêu cầu còn hạn và chưa PAID, backend cộng **toàn bộ số tiền thực nhận từ webhook** vào ví và ghi ledger trong cùng transaction.
6. Một yêu cầu đã PAID không được cộng lại bởi giao dịch khác. Webhook gửi lặp có cùng mã giao dịch được chặn qua unique key `provider + provider_transaction_id`.

## Thanh toán đơn bằng ví

- Checkout trả thêm phương thức `E_WALLET`.
- Backend tạo đơn và trừ ví cùng transaction database; trước khi trừ, backend khóa dòng User bằng `PESSIMISTIC_WRITE` rồi kiểm tra số dư mới nhất.
- Khi số dư không đủ hoặc người dùng không thuộc CUSTOMER, toàn bộ transaction bị rollback: không giữ đơn, không trừ tồn kho, không xóa giỏ hàng và không ghi ledger.
- Thành công ghi `orders.payment_status = PAID`, `orders.is_paid_before = true`, `orders.paid_at`; GHN tiếp tục nhận `cod_amount = 0` theo logic đã có.
- Nhân viên xác nhận đơn rồi tạo vận đơn GHN như luồng VietQR. Backend chỉ cho xác nhận/tạo vận đơn khi trạng thái đơn đã PAID và ledger có giao dịch trừ ví đúng bằng tổng đơn; GHN nhận `cod_amount = 0`.
- Đơn đã trả trước không cho Customer tự hủy để tránh mất tiền; chưa có cơ chế hoàn tiền tự động.

## Endpoint

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/wallet` | Số dư và 20 giao dịch ví gần nhất |
| POST | `/api/wallet/top-ups` | Tạo yêu cầu nạp và mã QR; không cần request body |
| GET | `/api/wallet/top-ups/{id}` | Trạng thái và QR của yêu cầu thuộc Customer đang đăng nhập |
| POST | `/api/webhooks/sepay` | Webhook SePay; cần `Authorization: Apikey <SEPAY_API_KEY>` |
| POST | `/orders` | Đặt hàng bằng COD, BANK_QR hoặc E_WALLET |
| GET | `/customer/wallet` | Giao diện ví |

## Cấu hình và triển khai

1. Nếu database chưa có bảng `bank_transfer_events`, chạy `migration_vietqr_payment.sql` trước. Sau đó chạy `migration_e_wallet.sql` một lần trên DB hiện có khi Hibernate không dùng `ddl-auto=update`. Với database đã có bảng `wallet_top_up_requests`, chạy thêm `migration_e_wallet_amount_optional.sql` để cho phép cột `requested_amount` rỗng. Cột `users.e_wallet` đã tồn tại trong schema của nhánh hiện tại.
2. Cấu hình cùng một tài khoản nhận, BIN, template QR và `SEPAY_API_KEY` như luồng VietQR trước đây. Không commit secret.
3. Cấu trúc mã thanh toán trên SePay phải chấp nhận tiền tố `PAYMENT_REFERENCE_PREFIX` và hậu tố 8 ký tự chữ/số; cả đơn hàng và yêu cầu nạp ví dùng chung cấu trúc để tương thích bộ lọc SePay.
4. URL webhook vẫn là `https://<domain-https-cong-khai>/api/webhooks/sepay`.
5. Nạp thử một số tiền nhỏ trên môi trường staging, xác nhận QR không điền sẵn số tiền, số dư chỉ tăng sau webhook hợp lệ, và gửi lại cùng webhook để kiểm tra không cộng trùng.

## Kiểm thử

Bổ sung unit test cho: nạp đúng số tiền, cùng yêu cầu nạp không cộng lần hai, số tiền webhook không khớp, thanh toán ví thành công, số dư không đủ và webhook cùng mã giao dịch gửi lặp. Kiểm thử tích hợp với SePay/ngân hàng thực tế vẫn cần chạy trên môi trường staging có cấu hình secret và webhook công khai.
