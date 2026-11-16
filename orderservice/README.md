# Order Management Service

Service quản lý đơn hàng (Order Management) trong kiến trúc SOA.

## Thông tin

- **Port**: 9002
- **Database**: soa_order
- **Framework**: Play Framework (Java)

## Cài đặt

### 1. Tạo database

Chạy script SQL trong file `conf/sql/soa_order.sql` để tạo database và tables:

```bash
# Sử dụng SQL Server Management Studio hoặc sqlcmd
sqlcmd -S localhost -U sa -P 123456 -i conf/sql/soa_order.sql
```

### 2. Cài đặt dependencies

```bash
sbt update
```

### 3. Chạy service

```bash
sbt run
```

Service sẽ chạy tại: http://localhost:9002

## API Endpoints

### Orders (Đơn hàng)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | /orders | Lấy danh sách tất cả đơn hàng |
| GET | /orders/:id | Lấy thông tin chi tiết một đơn hàng |
| POST | /orders | Tạo đơn hàng mới |
| PUT | /orders/:id | Cập nhật trạng thái đơn hàng |
| DELETE | /orders/:id | Xóa một đơn hàng |

### Order Items (Chi tiết đơn hàng)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | /order_items | Lấy danh sách tất cả mặt hàng |
| GET | /order_items/:id | Lấy thông tin chi tiết một mặt hàng |
| POST | /order_items | Tạo mặt hàng mới (tự động kiểm tra tồn kho) |
| PUT | /order_items/:id | Cập nhật mặt hàng |
| DELETE | /order_items/:id | Xóa một mặt hàng |

## Xác thực

Tất cả API yêu cầu JWT token trong header:

```
Authorization: Bearer <your-jwt-token>
```

Lấy token từ AuthService (port 9000):

```bash
POST http://localhost:9000/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

## Ví dụ sử dụng

### 1. Tạo đơn hàng mới

```bash
POST http://localhost:9002/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "customer_name": "Nguyen Van A",
  "customer_email": "a@gmail.com",
  "status": "pending"
}
```

### 2. Thêm sản phẩm vào đơn hàng (tự động kiểm tra tồn kho)

```bash
POST http://localhost:9002/order_items
Authorization: Bearer <token>
Content-Type: application/json

{
  "order_id": 1,
  "product_id": 1,
  "quantity": 2
}
```

**Lưu ý**: API này sẽ:
- Gọi ProductService để lấy thông tin sản phẩm
- Kiểm tra tồn kho
- Tự động tính giá và cập nhật tổng tiền đơn hàng

### 3. Cập nhật trạng thái đơn hàng

```bash
PUT http://localhost:9002/orders/1
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "completed"
}
```

### 4. Lấy danh sách đơn hàng

```bash
GET http://localhost:9002/orders
Authorization: Bearer <token>
```

## Tích hợp với các service khác

- **AuthService** (port 9000): Xác thực JWT token
- **ProductService** (port 9001): Kiểm tra tồn kho sản phẩm

## Cấu trúc Database

### Bảng orders

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | INT (PK) | ID duy nhất |
| customer_name | VARCHAR(255) | Tên khách hàng |
| customer_email | VARCHAR(255) | Email khách hàng |
| total_amount | DECIMAL(10,2) | Tổng tiền |
| status | VARCHAR(50) | Trạng thái (pending/completed/cancelled) |
| created_at | DATETIME | Ngày tạo |
| updated_at | DATETIME | Ngày cập nhật |

### Bảng order_items

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | INT (PK) | ID duy nhất |
| order_id | INT (FK) | ID đơn hàng |
| product_id | INT | ID sản phẩm |
| product_name | VARCHAR(255) | Tên sản phẩm |
| quantity | INT | Số lượng |
| unit_price | DECIMAL(10,2) | Giá đơn vị |
| total_price | DECIMAL(10,2) | Tổng giá |

## Troubleshooting

### Không kết nối được database

Kiểm tra:
- SQL Server đang chạy
- Database `soa_order` đã được tạo
- Username/password trong `conf/application.conf` đúng

### Không gọi được ProductService

Kiểm tra:
- ProductService (port 9001) đang chạy
- URL trong `conf/application.conf` đúng
- JWT token hợp lệ
