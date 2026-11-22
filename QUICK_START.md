# 🚀 Quick Start Guide - SOA Services

## Bước 1: Chuẩn bị Database

### Tạo databases trong SQL Server

```sql
-- Mở SQL Server Management Studio hoặc dùng sqlcmd

-- Tạo database cho ProductService
sqlcmd -S localhost -U sa -P 123456 -i productservice\conf\sql\soa_product.sql

-- Tạo database cho OrderService
sqlcmd -S localhost -U sa -P 123456 -i orderservice\conf\sql\soa_order.sql

-- Tạo database cho ReportingService
sqlcmd -S localhost -U sa -P 123456 -i reportingservice\conf\sql\soa_reporting.sql
```

Hoặc copy-paste SQL scripts vào SQL Server Management Studio:
- File: `productservice\conf\sql\soa_product.sql`
- File: `orderservice\conf\sql\soa_order.sql`

---

## Bước 2: Khởi động tất cả Services

### Cách 1: Dùng Batch Script (Khuyến nghị - Tự động)

```bash
# Double-click file này
start-all-services.bat
```

Script sẽ:
- ✅ Mở 4 terminal riêng biệt
- ✅ Tự động start AuthService (port 9000)
- ✅ Tự động start ProductService (port 9001)
- ✅ Tự động start OrderService (port 9002)
- ✅ Tự động start ReportingService (port 9003)

**Đợi 30-60 giây** cho các service khởi động hoàn toàn.

### Cách 2: Start thủ công (Manual)

Mở 3 terminal/PowerShell riêng:

```bash
# Terminal 1
cd d:\SOA_pratice\authservice
sbt run

# Terminal 2
cd d:\SOA_pratice\productservice
sbt run

# Terminal 3
cd d:\SOA_pratice\orderservice
sbt run

# Terminal 4
cd d:\SOA_pratice\reportingservice
sbt run
```

---

## Bước 3: Kiểm tra Services đang chạy

Mở browser và truy cập:
- http://localhost:9000 - AuthService ✓
- http://localhost:9001 - ProductService ✓
- http://localhost:9002 - OrderService ✓
- http://localhost:9003 - ReportingService ✓

---

## Bước 4: Test APIs

### Cách 1: Dùng Batch Script (Tự động test cơ bản)

```bash
# Double-click file này
test-apis.bat
```

Script sẽ tự động:
1. ✅ Kiểm tra services đang chạy
2. ✅ Login và lấy JWT token
3. ✅ Test GET products
4. ✅ Test GET orders
5. ✅ Test CREATE order
6. ✅ Test ADD product to order (với stock check)

### Cách 2: Dùng Postman (Test chi tiết)

1. **Import Collection:**
   - Mở Postman
   - Import file: `SOA_Services.postman_collection.json`

2. **Tạo Environment:**
   - Tạo environment mới tên "SOA Local"
   - Thêm variable: `jwt_token` (để lưu token)

3. **Chạy Tests:**
   - Chạy folder "1. Authentication" → Login để lấy token
   - Token sẽ tự động lưu vào environment variable
   - Chạy các request khác theo thứ tự

### Cách 3: Dùng curl (Manual)

```bash
# 1. Login
curl -X POST http://localhost:9000/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"

# Response: {"token":"eyJ..."}
# Copy token này!

# 2. Get Products
curl -X GET http://localhost:9001/products ^
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# 3. Get Orders
curl -X GET http://localhost:9002/orders ^
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# 4. Create Order
curl -X POST http://localhost:9002/orders ^
  -H "Authorization: Bearer YOUR_TOKEN_HERE" ^
  -H "Content-Type: application/json" ^
  -d "{\"customer_name\":\"John Doe\",\"customer_email\":\"john@example.com\"}"

# 5. Add Product to Order (Auto check stock)
curl -X POST http://localhost:9002/order_items ^
  -H "Authorization: Bearer YOUR_TOKEN_HERE" ^
  -H "Content-Type: application/json" ^
  -d "{\"order_id\":1,\"product_id\":1,\"quantity\":2}"
```

---

## Bước 5: Dừng tất cả Services

### Cách 1: Dùng Batch Script

```bash
# Double-click file này
stop-all-services.bat
```

### Cách 2: Manual

Đóng 3 terminal windows hoặc nhấn `Ctrl+C` trong mỗi terminal.

---

## 📋 Checklist Hoàn thành Bài thực hành

### ✅ Yêu cầu bắt buộc:

- [ ] Database `soa_order` đã được tạo
- [ ] Bảng `orders` và `order_items` có dữ liệu mẫu
- [ ] OrderService chạy trên port 9002
- [ ] API GET /orders hoạt động
- [ ] API GET /orders/:id hoạt động
- [ ] API POST /orders hoạt động
- [ ] API PUT /orders/:id hoạt động
- [ ] API DELETE /orders/:id hoạt động
- [ ] API GET /order_items hoạt động
- [ ] API GET /order_items/:id hoạt động
- [ ] API POST /order_items hoạt động
- [ ] API PUT /order_items/:id hoạt động
- [ ] API DELETE /order_items/:id hoạt động
- [ ] JWT Authentication hoạt động (tất cả API yêu cầu token)
- [ ] Kiểm tra tồn kho từ ProductService khi tạo order_item

### ✅ Tính năng nâng cao:

- [x] Tự động tính tổng tiền đơn hàng (total_amount)
- [x] Kiểm tra tồn kho tự động khi thêm sản phẩm
- [x] Trả về lỗi nếu sản phẩm không đủ hàng
- [x] Cập nhật total_amount khi thêm/sửa/xóa order_item
- [x] Database trigger cho updated_at
- [x] Foreign key constraint giữa orders và order_items

---

## 🐛 Troubleshooting

### Lỗi: "SBT not found"

**Giải pháp:** Install SBT từ https://www.scala-sbt.org/download.html

### Lỗi: "Cannot connect to database"

**Kiểm tra:**
```bash
# Test SQL Server connection
sqlcmd -S localhost -U sa -P 123456 -Q "SELECT @@VERSION"
```

**Giải pháp:**
- Đảm bảo SQL Server đang chạy
- Kiểm tra username/password trong `conf/application.conf`
- Đảm bảo databases đã được tạo

### Lỗi: "Port already in use"

**Giải pháp:**
```bash
# Tìm process đang dùng port
netstat -ano | findstr :9002

# Kill process
taskkill /PID <PID_NUMBER> /F
```

### Lỗi: "Token không hợp lệ"

**Giải pháp:**
- Lấy token mới từ AuthService
- Đảm bảo `play.http.secret.key` giống nhau trong tất cả service

### Lỗi: "Không thể lấy thông tin sản phẩm từ ProductService"

**Kiểm tra:**
```bash
# Test ProductService
curl http://localhost:9001/products/1 ^
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Giải pháp:**
- Đảm bảo ProductService đang chạy
- Kiểm tra `productservice.url` trong `orderservice/conf/application.conf`

---

## 📚 Tài liệu thêm

- **README.md** - Chi tiết về OrderService
- **TESTING.md** - Hướng dẫn test chi tiết
- **orderservice/conf/sql/soa_order.sql** - Database schema

---

## 🎯 Demo Flow hoàn chỉnh

```
1. Start all services (start-all-services.bat)
   └─> AuthService (9000), ProductService (9001), OrderService (9002)

2. Login
   └─> POST /auth/login → Get JWT token

3. Xem sản phẩm có sẵn
   └─> GET /products → Lấy danh sách sản phẩm và tồn kho

4. Tạo đơn hàng
   └─> POST /orders → Tạo đơn hàng với thông tin khách hàng

5. Thêm sản phẩm vào đơn
   └─> POST /order_items → OrderService tự động:
       ├─> Gọi ProductService kiểm tra sản phẩm
       ├─> Kiểm tra tồn kho
       ├─> Tính giá
       └─> Cập nhật tổng tiền đơn hàng

6. Xem đơn hàng
   └─> GET /orders/1 → Xem chi tiết đơn đã tạo

7. Cập nhật trạng thái
   └─> PUT /orders/1 {"status":"completed"}

8. Test case không đủ hàng
   └─> POST /order_items {quantity: 9999}
       └─> ❌ Error: "Sản phẩm không đủ tồn kho"
```

---

## ✅ Kết luận

Bạn đã có:
- ✅ 3 services hoạt động độc lập (SOA)
- ✅ JWT authentication
- ✅ Inter-service communication (OrderService → ProductService)
- ✅ Kiểm tra tồn kho tự động
- ✅ RESTful APIs đầy đủ
- ✅ Scripts tự động hóa test

**Chúc bạn hoàn thành tốt bài thực hành! 🎉**
