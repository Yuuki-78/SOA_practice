# Reporting Service

Service báo cáo và thống kê doanh thu, chi phí, lợi nhuận trong kiến trúc SOA.

## Thông tin

- **Port**: 9003
- **Database**: soa_reporting
- **Framework**: Play Framework (Java)

## Tính năng

### Báo cáo Đơn hàng (Order Reports)
- Tự động tính toán tổng doanh thu, chi phí, lợi nhuận của đơn hàng
- Tích hợp với OrderService để lấy thông tin đơn hàng và order_items
- Tích hợp với ProductService để lấy giá cost của sản phẩm

### Báo cáo Sản phẩm (Product Reports)
- Thống kê số lượng bán được theo sản phẩm
- Tính doanh thu, chi phí, lợi nhuận theo sản phẩm
- Liên kết với báo cáo đơn hàng

## Cài đặt

### 1. Tạo database

```bash
sqlcmd -S localhost -U sa -P 123456 -i conf/sql/soa_reporting.sql
```

### 2. Chạy service

```bash
sbt run
```

Service sẽ chạy tại: http://localhost:9003

## API Endpoints

### Order Reports

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | /reports/orders | Lấy danh sách tất cả báo cáo đơn hàng |
| GET | /reports/orders/:id | Lấy chi tiết báo cáo đơn hàng |
| POST | /reports/orders | Tạo báo cáo đơn hàng mới (tự động tính toán) |
| DELETE | /reports/orders/:id | Xóa báo cáo đơn hàng |

### Product Reports

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | /reports/products | Lấy danh sách tất cả báo cáo sản phẩm |
| GET | /reports/products/:id | Lấy chi tiết báo cáo sản phẩm |
| POST | /reports/products | Tạo báo cáo sản phẩm thủ công |
| DELETE | /reports/products/:id | Xóa báo cáo sản phẩm |

## Xác thực

Tất cả API yêu cầu JWT token:

```
Authorization: Bearer <your-jwt-token>
```

## Ví dụ sử dụng

### 1. Tạo báo cáo cho đơn hàng

```bash
POST http://localhost:9003/reports/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "order_id": 1
}
```

**Quy trình tự động:**
1. Gọi OrderService để lấy thông tin đơn hàng
2. Lấy danh sách order_items của đơn hàng
3. Với mỗi order_item:
   - Tính revenue = unit_price × quantity
   - Gọi ProductService để lấy giá gốc (cost)
   - Cost = giá gốc × 0.8 × quantity (giả định)
   - Profit = revenue - cost
4. Tổng hợp:
   - total_revenue = SUM(all revenue)
   - total_cost = SUM(all cost)
   - total_profit = total_revenue - total_cost
5. Lưu vào database cả order report và product reports

### 2. Xem danh sách báo cáo

```bash
GET http://localhost:9003/reports/orders
Authorization: Bearer <token>
```

Response:
```json
[
  {
    "id": 1,
    "order_id": 1,
    "total_revenue": 1500000.00,
    "total_cost": 1200000.00,
    "total_profit": 300000.00,
    "created_at": "2024-11-16T10:00:00"
  }
]
```

### 3. Xem báo cáo sản phẩm

```bash
GET http://localhost:9003/reports/products
Authorization: Bearer <token>
```

Response:
```json
[
  {
    "id": 1,
    "order_report_id": 1,
    "product_id": 1,
    "total_sold": 2,
    "revenue": 1500000.00,
    "cost": 1200000.00,
    "profit": 300000.00
  }
]
```

## Tích hợp với các service khác

- **AuthService** (port 9000): Xác thực JWT token
- **ProductService** (port 9001): Lấy thông tin sản phẩm và giá cost
- **OrderService** (port 9002): Lấy thông tin đơn hàng và order_items

## Công thức tính toán

### Doanh thu (Revenue)
```
revenue = unit_price × quantity
```

### Chi phí (Cost)
```
cost = product_cost × 0.8 × quantity
// Giả định cost = 80% giá bán
```

### Lợi nhuận (Profit)
```
profit = revenue - cost
```

### Tổng báo cáo đơn hàng
```
total_revenue = SUM(revenue của tất cả sản phẩm trong đơn)
total_cost = SUM(cost của tất cả sản phẩm trong đơn)
total_profit = total_revenue - total_cost
```

## Database Schema

### Bảng orders_reports

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | INT (PK) | ID báo cáo |
| order_id | INT | ID đơn hàng |
| total_revenue | DECIMAL(10,2) | Tổng doanh thu |
| total_cost | DECIMAL(10,2) | Tổng chi phí |
| total_profit | DECIMAL(10,2) | Tổng lợi nhuận |
| created_at | DATETIME | Ngày tạo |

### Bảng product_reports

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | INT (PK) | ID báo cáo sản phẩm |
| order_report_id | INT (FK) | ID báo cáo đơn hàng |
| product_id | INT | ID sản phẩm |
| total_sold | INT | Tổng số lượng bán |
| revenue | DECIMAL(10,2) | Doanh thu |
| cost | DECIMAL(10,2) | Chi phí |
| profit | DECIMAL(10,2) | Lợi nhuận |

## Troubleshooting

### Lỗi: "Không tìm thấy đơn hàng"

**Nguyên nhân:**
- OrderService không chạy
- Order ID không tồn tại
- Token không có quyền truy cập

**Giải pháp:**
```bash
# Kiểm tra OrderService
curl http://localhost:9002/orders -H "Authorization: Bearer TOKEN"
```

### Lỗi: "Không thể lấy thông tin sản phẩm"

**Giải pháp:**
- Đảm bảo ProductService đang chạy
- Kiểm tra product_id có tồn tại không
