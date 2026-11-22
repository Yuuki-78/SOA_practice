# 🏗️ HỆ THỐNG SOA - QUẢN LÝ SẢN PHẨM VÀ ĐỚN HÀNG

## 📋 Giới thiệu

Đây là hệ thống quản lý sản phẩm và đơn hàng được xây dựng theo kiến trúc **SOA (Service-Oriented Architecture)**, bao gồm 4 services độc lập:

- **AuthService** - Dịch vụ xác thực người dùng (JWT)
- **ProductService** - Dịch vụ quản lý sản phẩm
- **OrderService** - Dịch vụ quản lý đơn hàng
- **ReportingService** - Dịch vụ báo cáo và thống kê

Mỗi service hoạt động độc lập với database riêng, giao tiếp với nhau thông qua RESTful API.

---

## 🎯 Danh sách Bài thực hành đã hoàn thành

### ✅ Bài thực hành số 2: Router, Middleware và bảo mật với JWT

**Mục tiêu:** Xây dựng dịch vụ đăng nhập và xác thực người dùng bằng JWT

**Service:** AuthService  
**Port:** 9000  
**Database:** soa_auth  
**Thư mục:** `authservice/`

**Chức năng:**
- API đăng nhập: `POST /auth/login`
- Xác thực JWT token
- Middleware để bảo vệ các API trong các service khác

**Công nghệ:**
- Play Framework (Java)
- JWT (JSON Web Token) với thư viện `java-jwt`
- SQL Server

---

### ✅ Bài thực hành số 3: Quản lý sản phẩm (Product Management)

**Mục tiêu:** Xây dựng dịch vụ quản lý sản phẩm (CRUD operations)

**Service:** ProductService  
**Port:** 9001  
**Database:** soa_product  
**Thư mục:** `productservice/`

**API Endpoints:**
- `GET /products` - Lấy danh sách sản phẩm
- `GET /products/:id` - Lấy chi tiết sản phẩm
- `POST /products` - Thêm sản phẩm mới
- `PUT /products/:id` - Cập nhật sản phẩm
- `DELETE /products/:id` - Xóa sản phẩm

**Tính năng:**
- CRUD đầy đủ cho sản phẩm
- Xác thực JWT trên tất cả endpoints
- Quản lý thông tin: tên, mô tả, giá, số lượng tồn kho

---

### ✅ Bài thực hành số 4: Quản lý đơn hàng (Order Management)

**Mục tiêu:** Xây dựng dịch vụ quản lý đơn hàng với kiểm tra tồn kho

**Service:** OrderService  
**Port:** 9002  
**Database:** soa_order  
**Thư mục:** `orderservice/`

**API Endpoints:**

**Orders:**
- `GET /orders` - Lấy danh sách đơn hàng
- `GET /orders/:id` - Lấy chi tiết đơn hàng
- `POST /orders` - Tạo đơn hàng mới
- `PUT /orders/:id` - Cập nhật trạng thái đơn hàng
- `DELETE /orders/:id` - Xóa đơn hàng

**Order Items:**
- `GET /order_items` - Lấy danh sách mặt hàng
- `GET /order_items/:id` - Lấy chi tiết mặt hàng
- `POST /order_items` - Thêm sản phẩm vào đơn hàng (tự động kiểm tra tồn kho)
- `PUT /order_items/:id` - Cập nhật mặt hàng
- `DELETE /order_items/:id` - Xóa mặt hàng

**Tính năng nổi bật:**
- Tích hợp với ProductService để kiểm tra tồn kho
- Tự động tính tổng tiền đơn hàng
- Cập nhật tổng tiền khi thêm/xóa/sửa order_items
- Xác thực JWT

---

### ✅ Bài thực hành số 5: Dịch vụ báo cáo (Reporting Service)

**Mục tiêu:** Xây dựng dịch vụ báo cáo với thống kê doanh thu, chi phí, lợi nhuận

**Service:** ReportingService  
**Port:** 9003  
**Database:** soa_reporting  
**Thư mục:** `reportingservice/`

**API Endpoints:**

**Order Reports:**
- `GET /reports/orders` - Lấy danh sách báo cáo đơn hàng
- `GET /reports/orders/:id` - Lấy chi tiết báo cáo đơn hàng
- `POST /reports/orders` - Tạo báo cáo đơn hàng (tự động tính toán)
- `DELETE /reports/orders/:id` - Xóa báo cáo đơn hàng

**Product Reports:**
- `GET /reports/products` - Lấy danh sách báo cáo sản phẩm
- `GET /reports/products/:id` - Lấy chi tiết báo cáo sản phẩm
- `POST /reports/products` - Tạo báo cáo sản phẩm
- `DELETE /reports/products/:id` - Xóa báo cáo sản phẩm

**Tính năng nổi bật:**
- Tự động tính toán doanh thu, chi phí, lợi nhuận
- Tích hợp với cả ProductService và OrderService
- Sử dụng async/CompletionStage cho concurrent API calls
- Công thức tính toán:
  - `revenue = price × quantity`
  - `cost = price × 0.8 × quantity`
  - `profit = revenue - cost`

---

## 🏗️ Kiến trúc Hệ thống

```
┌─────────────────┐
│  AuthService    │ (Port 9000)
│  JWT Auth       │ ← Xác thực cho tất cả services
└─────────────────┘
         ↓
    ┌────┴─────────────────┐
    ↓                      ↓
┌──────────┐         ┌──────────┐
│ Product  │←────────│  Order   │ (Port 9002)
│ Service  │  Check  │ Service  │ ← Kiểm tra tồn kho
│(Port 9001)│  Stock  └──────────┘
└──────────┘              ↑
    ↑                     │
    │                     │
    └─────────┬───────────┘
              ↓
       ┌─────────────┐
       │ Reporting   │ (Port 9003)
       │ Service     │ ← Tổng hợp báo cáo
       └─────────────┘
```

### Luồng hoạt động:

1. **User** → Đăng nhập qua AuthService → Nhận JWT token
2. **User** → Gửi request với JWT token trong header
3. **Service** → Xác thực token → Xử lý nghiệp vụ
4. **OrderService** → Gọi ProductService kiểm tra tồn kho
5. **ReportingService** → Gọi OrderService + ProductService → Tính toán báo cáo

---

## 💾 Cơ sở dữ liệu

Hệ thống sử dụng **SQL Server** với 4 databases độc lập:

| Database | Service | Tables |
|----------|---------|--------|
| `soa_auth` | AuthService | users |
| `soa_product` | ProductService | products |
| `soa_order` | OrderService | orders, order_items |
| `soa_reporting` | ReportingService | orders_reports, product_reports |

**Nguyên tắc SOA:**
- Mỗi service có database riêng
- Không chia sẻ database giữa các services
- Giao tiếp qua RESTful API

---

## 🚀 Cài đặt và Chạy

### Yêu cầu hệ thống:

- **Java**: JDK 11 trở lên
- **SBT**: 1.10.x
- **SQL Server**: 2019 trở lên
- **Scala**: 2.13.x (đi kèm với sbt)

### Bước 1: Tạo Databases

Chạy lần lượt các script SQL:

```bash
# AuthService database (nếu chưa có)
sqlcmd -S localhost -U sa -P 123456 -i authservice\conf\sql\soa_auth.sql

# ProductService database
sqlcmd -S localhost -U sa -P 123456 -i productservice\conf\sql\soa_product.sql

# OrderService database
sqlcmd -S localhost -U sa -P 123456 -i orderservice\conf\sql\soa_order.sql

# ReportingService database
sqlcmd -S localhost -U sa -P 123456 -i reportingservice\conf\sql\soa_reporting.sql
```

### Bước 2: Khởi động tất cả Services

**Cách 1: Tự động (Khuyến nghị)**

```bash
# Double-click file này hoặc chạy trong terminal
start-all-services.bat
```

Script sẽ tự động mở 4 terminal windows và khởi động tất cả services.

**Cách 2: Thủ công**

Mở 4 terminal/PowerShell riêng biệt:

```bash
# Terminal 1: AuthService
cd authservice
sbt run

# Terminal 2: ProductService
cd productservice
sbt run

# Terminal 3: OrderService
cd orderservice
sbt run

# Terminal 4: ReportingService
cd reportingservice
sbt run
```

**Đợi 30-60 giây** cho các service khởi động hoàn toàn.

### Bước 3: Kiểm tra Services

Mở browser và truy cập:
- http://localhost:9000 - AuthService ✓
- http://localhost:9001 - ProductService ✓
- http://localhost:9002 - OrderService ✓
- http://localhost:9003 - ReportingService ✓

---

## 🧪 Test và Sử dụng

### Cách 1: Test tự động bằng Script

```bash
# Double-click file này
test-apis.bat
```

Script sẽ tự động:
1. Kiểm tra tất cả services đang chạy
2. Login và lấy JWT token
3. Test các API của ProductService
4. Test các API của OrderService
5. Test các API của ReportingService
6. Hiển thị kết quả

### Cách 2: Test bằng Postman

1. **Import Collection:**
   - Mở Postman
   - Import file: `SOA_Services.postman_collection.json`

2. **Tạo Environment:**
   - Tạo environment mới tên "SOA Local"
   - Thêm variable: `jwt_token`

3. **Chạy Tests:**
   - Folder "1. Authentication" → Login để lấy token
   - Token tự động lưu vào environment
   - Chạy các folder khác theo thứ tự

### Cách 3: Test bằng curl

```bash
# 1. Login để lấy token
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"

# Response: {"token":"eyJ..."}
# Lưu token này!

# 2. Lấy danh sách sản phẩm
curl -X GET http://localhost:9001/products \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# 3. Tạo đơn hàng
curl -X POST http://localhost:9002/orders \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"customer_name\":\"John Doe\",\"customer_email\":\"john@example.com\"}"

# 4. Thêm sản phẩm vào đơn (tự động check stock)
curl -X POST http://localhost:9002/order_items \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"order_id\":1,\"product_id\":1,\"quantity\":2}"

# 5. Tạo báo cáo (tự động tính toán)
curl -X POST http://localhost:9003/reports/orders \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"order_id\":1}"
```

---

## 📁 Cấu trúc Project

```
SOA_pratice/
├── authservice/              # Bài 2: Xác thực JWT
│   ├── app/
│   │   ├── controllers/      # AuthController
│   │   └── utils/            # JwtUtil
│   ├── conf/
│   │   ├── application.conf  # Port 9000
│   │   └── routes
│   └── build.sbt
│
├── productservice/           # Bài 3: Quản lý sản phẩm
│   ├── app/
│   │   ├── controllers/      # ProductController, JwtAction
│   │   ├── models/           # Product
│   │   └── utils/            # JwtUtil
│   ├── conf/
│   │   ├── application.conf  # Port 9001
│   │   ├── routes
│   │   └── sql/
│   │       └── soa_product.sql
│   └── build.sbt
│
├── orderservice/             # Bài 4: Quản lý đơn hàng
│   ├── app/
│   │   ├── controllers/      # OrderController, OrderItemController
│   │   ├── models/           # Order, OrderItem
│   │   └── utils/            # JwtUtil
│   ├── conf/
│   │   ├── application.conf  # Port 9002
│   │   ├── routes
│   │   └── sql/
│   │       └── soa_order.sql
│   └── build.sbt
│
├── reportingservice/         # Bài 5: Báo cáo
│   ├── app/
│   │   ├── controllers/      # OrderReportController, ProductReportController
│   │   ├── models/           # OrderReport, ProductReport
│   │   └── utils/            # JwtUtil
│   ├── conf/
│   │   ├── application.conf  # Port 9003
│   │   ├── routes
│   │   └── sql/
│   │       └── soa_reporting.sql
│   └── build.sbt
│
├── start-all-services.bat    # Script khởi động tất cả services
├── stop-all-services.bat     # Script dừng tất cả services
├── test-apis.bat             # Script test tự động
├── SOA_Services.postman_collection.json  # Postman collection
├── QUICK_START.md            # Hướng dẫn nhanh
└── README.md                 # File này
```

---

## 🔑 Thông tin đăng nhập mặc định

**Username:** `admin`  
**Password:** `admin123`

Thông tin này được cấu hình sẵn trong AuthService. Có thể thêm users khác vào database `soa_auth`.

---

## 🎯 Các tính năng nổi bật

### 1. Xác thực JWT toàn hệ thống
- Tất cả APIs đều được bảo vệ bằng JWT
- Token được tạo khi login
- Mỗi request cần có header: `Authorization: Bearer <token>`

### 2. Inter-Service Communication
- OrderService gọi ProductService để kiểm tra tồn kho
- ReportingService gọi cả OrderService và ProductService
- Sử dụng HTTP/REST để giao tiếp

### 3. Async Programming
- Sử dụng `CompletionStage` trong Java
- Xử lý concurrent requests hiệu quả
- Non-blocking I/O

### 4. Tính toán tự động
- OrderService: Tự động tính `total_amount` của đơn hàng
- ReportingService: Tự động tính `revenue`, `cost`, `profit`

### 5. Database Integrity
- Foreign key constraints
- Cascade delete
- Database triggers
- Views để tổng hợp dữ liệu

---

## 🛠️ Công nghệ sử dụng

- **Framework:** Play Framework 3.0.5 (Java)
- **Language:** Java 11+, Scala 2.13
- **Build Tool:** SBT 1.10.1
- **Database:** Microsoft SQL Server 2019+
- **Authentication:** JWT (java-jwt 4.4.0)
- **HTTP Client:** Play WS (để gọi APIs giữa các services)

---

## 📖 Tài liệu chi tiết

Mỗi service có file README.md riêng với hướng dẫn chi tiết:

- `authservice/README.md` - Chi tiết về AuthService
- `productservice/README.md` - Chi tiết về ProductService
- `orderservice/README.md` - Chi tiết về OrderService
- `orderservice/TESTING.md` - Hướng dẫn test OrderService
- `reportingservice/README.md` - Chi tiết về ReportingService
- `QUICK_START.md` - Hướng dẫn nhanh cho toàn bộ hệ thống

---

## 🐛 Troubleshooting

### Lỗi: "Cannot connect to database"

**Nguyên nhân:** SQL Server không chạy hoặc thông tin kết nối sai

**Giải pháp:**
```bash
# Kiểm tra SQL Server
sqlcmd -S localhost -U sa -P 123456 -Q "SELECT @@VERSION"

# Kiểm tra databases đã tạo
sqlcmd -S localhost -U sa -P 123456 -Q "SELECT name FROM sys.databases WHERE name LIKE 'soa_%'"
```

### Lỗi: "Port already in use"

**Nguyên nhân:** Port đang bị service khác sử dụng

**Giải pháp:**
```bash
# Tìm process đang dùng port
netstat -ano | findstr :9001

# Kill process
taskkill /PID <PID_NUMBER> /F
```

### Lỗi: "Token không hợp lệ"

**Nguyên nhân:** Token hết hạn hoặc secret key không đồng bộ

**Giải pháp:**
- Lấy token mới từ AuthService
- Đảm bảo `play.http.secret.key` giống nhau trong tất cả `application.conf`

### Lỗi: "SBT not found"

**Giải pháp:**
- Download SBT từ: https://www.scala-sbt.org/download.html
- Install và thêm vào PATH

---

## 📊 Kết quả Demo

### 1. Login thành công
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2. Lấy danh sách sản phẩm
```json
[
  {
    "id": 1,
    "name": "iPhone 15 Pro",
    "price": 1500000.00,
    "stock": 10
  }
]
```

### 3. Tạo đơn hàng
```json
{
  "message": "Tạo đơn hàng thành công",
  "id": 4
}
```

### 4. Thêm sản phẩm (với kiểm tra tồn kho)
```json
{
  "message": "Tạo mặt hàng thành công",
  "id": 5
}
```

### 5. Báo cáo tự động
```json
{
  "message": "Tạo báo cáo đơn hàng thành công",
  "id": 1,
  "total_revenue": 3000000.00,
  "total_cost": 2400000.00,
  "total_profit": 600000.00
}
```

---

## 👨‍🏫 Kết luận

Hệ thống đã được xây dựng hoàn chỉnh với 4 bài thực hành:

✅ **Bài 2:** AuthService với JWT Authentication  
✅ **Bài 3:** ProductService với CRUD operations  
✅ **Bài 4:** OrderService với stock checking  
✅ **Bài 5:** ReportingService với auto-calculation  

Tất cả services:
- Hoạt động độc lập với database riêng
- Giao tiếp qua RESTful API
- Được bảo vệ bằng JWT
- Tuân thủ nguyên tắc SOA

---

## 📞 Liên hệ và Hỗ trợ

Nếu có vấn đề khi chạy hệ thống, vui lòng:
1. Kiểm tra lại các bước cài đặt
2. Xem file `QUICK_START.md`
3. Kiểm tra logs trong thư mục `logs/` của mỗi service
4. Đảm bảo tất cả databases đã được tạo

---

**Ngày hoàn thành:** November 2024  
**Framework:** Play Framework 3.0.5  
**Kiến trúc:** SOA (Service-Oriented Architecture)
