-- Tạo database
CREATE DATABASE soa_order;
GO

USE soa_order;
GO

-- Bảng đơn hàng (orders)
CREATE TABLE orders (
    id INT PRIMARY KEY IDENTITY(1,1),
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

-- Bảng chi tiết đơn hàng (order_items)
CREATE TABLE order_items (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
GO

-- Insert dữ liệu mẫu
INSERT INTO orders (customer_name, customer_email, total_amount, status) VALUES
('Nguyen Van A', 'a@gmail.com', 1500000, 'pending'),
('Tran Thi B', 'b@gmail.com', 3000000, 'completed'),
('Le Van C', 'c@gmail.com', 750000, 'cancelled');
GO

INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, total_price) VALUES
(1, 1, 'iPhone 15 Pro', 1, 1500000, 1500000),
(2, 2, 'Samsung Galaxy S24', 2, 1500000, 3000000),
(3, 3, 'Xiaomi 14', 1, 750000, 750000);
GO

-- Tạo trigger để tự động cập nhật updated_at
CREATE TRIGGER trg_UpdateOrderTimestamp
ON orders
AFTER UPDATE
AS
BEGIN
    UPDATE orders
    SET updated_at = GETDATE()
    FROM orders o
    INNER JOIN inserted i ON o.id = i.id;
END;
GO
