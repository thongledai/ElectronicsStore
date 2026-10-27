# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Khách hàng là người dùng chính, tìm kiếm và mua sản phẩm điện tử trực tuyến. Nhân viên và quản lý sử dụng các màn hình vận hành để quản lý đơn hàng và cửa hàng.

## Product Purpose

UTE Shop cung cấp trải nghiệm mua sắm sản phẩm điện tử trực tuyến, từ xem sản phẩm đến thanh toán, theo dõi đơn hàng và quản lý tài khoản. Thành công là giúp khách hàng hoàn tất các tác vụ mua sắm và giúp nhân viên, quản lý xử lý hoạt động cửa hàng.

## Positioning

Chưa xác định điểm khác biệt riêng cần cam kết.

## Operating Context

Ứng dụng web dùng Spring Boot và Thymeleaf, với giao diện storefront cho khách hàng cùng các khu vực vận hành dành cho nhân viên và quản lý. Một số dữ liệu hiển thị được tải qua API backend.

## Capabilities and Constraints

- Bảo toàn các luồng xác thực, phân quyền, catalog, giỏ hàng, đơn hàng, tồn kho, thanh toán, ví, VietQR/SePay và giao hàng GHN hiện có.
- Giữ Spring Boot, Thymeleaf, HTML, CSS và JavaScript cùng cấu trúc dự án hiện tại.
- Không tạo dữ liệu giả để thay dữ liệu nghiệp vụ; khi backend chưa cung cấp dữ liệu cần thiết, cần nêu rõ giới hạn.
- Thay đổi trong phạm vi giao diện phải giữ nguyên hợp đồng API, tên trường và định danh kỹ thuật.

## Brand Commitments

Tên thương hiệu hiển thị cho người dùng là UTE Shop.

## Product Principles

- Hỗ trợ khách hàng hoàn tất mua sắm rõ ràng và thuận tiện.
- Giúp nhân viên và quản lý theo dõi hoạt động cửa hàng bằng dữ liệu backend.
- Bảo toàn tính chính xác của giá, đơn hàng, thanh toán và trạng thái giao hàng.
- Giữ giao diện nhất quán, dễ đọc trên máy tính và thiết bị di động.
