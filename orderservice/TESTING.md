# Hướng dẫn Test OrderService

## Bước 1: Chuẩn bị

### 1.1. Tạo Database

Chạy script SQL:

```sql
-- File: conf/sql/soa_order.sql
-- Chạy bằng SQL Server Management Studio hoặc sqlcmd
```

### 1.2. Khởi động các service cần thiết

```bash
# Terminal 1: AuthService (port 9000)
cd d:\SOA_pratice\authservice
sbt run

# Terminal 2: ProductService (port 9001)
cd d:\SOA_pratice\productservice
sbt run

# Terminal 3: OrderService (port 9002)
cd d:\SOA_pratice\orderservice
sbt run
```

## Bước 2: Lấy JWT Token

```bash
# POST đến AuthService
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Lưu token này để dùng cho các request tiếp theo!**

## Bước 3: Test Order APIs

### 3.1. Lấy danh sách đơn hàng

```bash
curl -X GET http://localhost:9002/orders \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 3.2. Lấy chi tiết đơn hàng

```bash
curl -X GET http://localhost:9002/orders/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 3.3. Tạo đơn hàng mới

```bash
curl -X POST http://localhost:9002/orders \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d "{
    \"customer_name\": \"Test Customer\",
    \"customer_email\": \"test@example.com\",
    \"status\": \"pending\"
  }"
```

### 3.4. Cập nhật trạng thái đơn hàng

```bash
curl -X PUT http://localhost:9002/orders/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"status\": \"completed\"}"
```

### 3.5. Xóa đơn hàng

```bash
curl -X DELETE http://localhost:9002/orders/3 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## Bước 4: Test Order Items APIs (với kiểm tra tồn kho)

### 4.1. Lấy danh sách mặt hàng

```bash
curl -X GET http://localhost:9002/order_items \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4.2. Thêm sản phẩm vào đơn hàng (tự động kiểm tra tồn kho)

```bash
# Thêm sản phẩm có ID=1 với số lượng 2
curl -X POST http://localhost:9002/order_items \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d "{
    \"order_id\": 1,
    \"product_id\": 1,
    \"quantity\": 2
  }"
```

**Điều gì xảy ra:**
1. OrderService gọi ProductService: `GET /products/1`
2. Lấy thông tin: tên, giá, tồn kho
3. Kiểm tra: `stock >= quantity`?
4. Nếu đủ hàng: Tạo order_item và cập nhật total_amount của order
5. Nếu không đủ: Trả về lỗi "Sản phẩm không đủ tồn kho"

### 4.3. Test trường hợp không đủ hàng

```bash
# Giả sử sản phẩm ID=1 chỉ còn 5 cái, thử mua 100
curl -X POST http://localhost:9002/order_items \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d "{
    \"order_id\": 1,
    \"product_id\": 1,
    \"quantity\": 100
  }"
```

Expected response:
```json
{
  "error": "Sản phẩm không đủ tồn kho. Còn lại: 5"
}
```

### 4.4. Cập nhật số lượng mặt hàng

```bash
curl -X PUT http://localhost:9002/order_items/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"quantity\": 5}"
```

### 4.5. Xóa mặt hàng

```bash
curl -X DELETE http://localhost:9002/order_items/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## Bước 5: Test với Postman

### Collection có thể import:

```json
{
  "info": {
    "name": "OrderService Tests"
  },
  "item": [
    {
      "name": "1. Login",
      "request": {
        "method": "POST",
        "url": "http://localhost:9000/auth/login",
        "body": {
          "mode": "raw",
          "raw": "{\"username\":\"admin\",\"password\":\"admin123\"}"
        }
      }
    },
    {
      "name": "2. Get All Orders",
      "request": {
        "method": "GET",
        "url": "http://localhost:9002/orders",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ]
      }
    },
    {
      "name": "3. Create Order",
      "request": {
        "method": "POST",
        "url": "http://localhost:9002/orders",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"customer_name\":\"Test\",\"customer_email\":\"test@example.com\"}"
        }
      }
    },
    {
      "name": "4. Add Product to Order",
      "request": {
        "method": "POST",
        "url": "http://localhost:9002/order_items",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"order_id\":1,\"product_id\":1,\"quantity\":2}"
        }
      }
    }
  ]
}
```

## Kết quả mong đợi

✅ **Thành công khi:**
- Tạo được đơn hàng mới
- Thêm được sản phẩm khi còn hàng
- Tự động tính tổng tiền đơn hàng
- Cập nhật được trạng thái
- Xóa được đơn hàng/mặt hàng

❌ **Thất bại khi:**
- Không có token hoặc token sai → 401 Unauthorized
- Sản phẩm không đủ tồn kho → 400 Bad Request
- ProductService không chạy → 400 Bad Request
- ID không tồn tại → 404 Not Found

## Troubleshooting

### Lỗi: "Không thể lấy thông tin sản phẩm từ ProductService"

**Nguyên nhân:**
- ProductService chưa chạy
- Token không được forward đúng cách
- URL sai trong `application.conf`

**Giải pháp:**
```bash
# Kiểm tra ProductService
curl http://localhost:9001

# Kiểm tra config
cat conf/application.conf | grep productservice.url
```

### Lỗi: "Token không hợp lệ"

**Nguyên nhân:**
- Token hết hạn
- Secret key khác nhau giữa các service

**Giải pháp:**
- Lấy token mới từ AuthService
- Đảm bảo `play.http.secret.key` giống nhau trong tất cả service

### Lỗi: "Không kết nối được database"

**Giải pháp:**
```bash
# Kiểm tra SQL Server
sqlcmd -S localhost -U sa -P 123456 -Q "SELECT name FROM sys.databases WHERE name = 'soa_order'"

# Nếu chưa có database, chạy script
sqlcmd -S localhost -U sa -P 123456 -i conf/sql/soa_order.sql
```
