-- Tạo database
CREATE DATABASE soa_reporting;
GO

USE soa_reporting;
GO

-- Bảng báo cáo đơn hàng (orders_reports)
CREATE TABLE orders_reports (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_id INT NOT NULL,
    total_revenue DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_cost DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_profit DECIMAL(10, 2) NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- Bảng báo cáo sản phẩm (product_reports)
CREATE TABLE product_reports (
    id INT PRIMARY KEY IDENTITY(1,1),
    order_report_id INT NOT NULL,
    product_id INT NOT NULL,
    total_sold INT NOT NULL,
    revenue DECIMAL(10, 2) NOT NULL,
    cost DECIMAL(10, 2) NOT NULL,
    profit DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_report_id) REFERENCES orders_reports(id) ON DELETE CASCADE
);
GO

-- Insert dữ liệu mẫu
-- Báo cáo đơn hàng 1
INSERT INTO orders_reports (order_id, total_revenue, total_cost, total_profit) 
VALUES (1, 1500000, 1200000, 300000);
GO

-- Báo cáo sản phẩm cho đơn hàng 1
INSERT INTO product_reports (order_report_id, product_id, total_sold, revenue, cost, profit)
VALUES (1, 1, 1, 1500000, 1200000, 300000);
GO

-- Báo cáo đơn hàng 2
INSERT INTO orders_reports (order_id, total_revenue, total_cost, total_profit) 
VALUES (2, 3000000, 2400000, 600000);
GO

-- Báo cáo sản phẩm cho đơn hàng 2
INSERT INTO product_reports (order_report_id, product_id, total_sold, revenue, cost, profit)
VALUES (2, 2, 2, 3000000, 2400000, 600000);
GO

-- View tổng hợp báo cáo sản phẩm
CREATE VIEW vw_product_summary AS
SELECT 
    pr.product_id,
    SUM(pr.total_sold) AS total_quantity_sold,
    SUM(pr.revenue) AS total_revenue,
    SUM(pr.cost) AS total_cost,
    SUM(pr.profit) AS total_profit
FROM product_reports pr
GROUP BY pr.product_id;
GO

-- View tổng hợp báo cáo đơn hàng
CREATE VIEW vw_order_summary AS
SELECT 
    or2.order_id,
    or2.total_revenue,
    or2.total_cost,
    or2.total_profit,
    COUNT(pr.id) AS total_products,
    or2.created_at
FROM orders_reports or2
LEFT JOIN product_reports pr ON or2.id = pr.order_report_id
GROUP BY or2.order_id, or2.total_revenue, or2.total_cost, or2.total_profit, or2.created_at;
GO
