package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.OrderReport;
import models.ProductReport;
import play.db.Database;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.mvc.*;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

@With(JwtAction.class)
public class OrderReportController extends Controller {

    @Inject
    Database db;

    @Inject
    WSClient ws;

    @Inject
    play.Configuration config;

    // GET /reports/orders - Lấy danh sách tất cả báo cáo đơn hàng
    public Result getAllOrderReports(Http.Request request) {
        List<OrderReport> reports = new ArrayList<>();
        
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM orders_reports ORDER BY created_at DESC")) {
            
            while (rs.next()) {
                OrderReport report = new OrderReport(
                    rs.getInt("id"),
                    rs.getInt("order_id"),
                    rs.getBigDecimal("total_revenue"),
                    rs.getBigDecimal("total_cost"),
                    rs.getBigDecimal("total_profit"),
                    rs.getTimestamp("created_at")
                );
                reports.add(report);
            }
            
            return ok(Json.toJson(reports));
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi lấy danh sách báo cáo: " + e.getMessage()));
        }
    }

    // GET /reports/orders/:id - Lấy chi tiết báo cáo đơn hàng
    public Result getOrderReportById(Http.Request request, Integer id) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM orders_reports WHERE id = ?")) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                OrderReport report = new OrderReport(
                    rs.getInt("id"),
                    rs.getInt("order_id"),
                    rs.getBigDecimal("total_revenue"),
                    rs.getBigDecimal("total_cost"),
                    rs.getBigDecimal("total_profit"),
                    rs.getTimestamp("created_at")
                );
                return ok(Json.toJson(report));
            } else {
                return notFound(Json.newObject().put("error", "Không tìm thấy báo cáo"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi lấy báo cáo: " + e.getMessage()));
        }
    }

    // POST /reports/orders - Tạo báo cáo đơn hàng mới từ OrderService
    public CompletionStage<Result> createOrderReport(Http.Request request) {
        JsonNode json = request.body().asJson();
        
        if (json == null || !json.has("order_id")) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                badRequest(Json.newObject().put("error", "Thiếu order_id trong request"))
            );
        }
        
        Integer orderId = json.get("order_id").asInt();
        String authHeader = request.header("Authorization").orElse("");
        String orderServiceUrl = config.getString("orderservice.url");
        String productServiceUrl = config.getString("productservice.url");
        
        // Gọi OrderService để lấy thông tin đơn hàng và order_items
        return ws.url(orderServiceUrl + "/orders/" + orderId)
            .addHeader("Authorization", authHeader)
            .get()
            .thenCompose(orderResponse -> {
                if (orderResponse.getStatus() != 200) {
                    return java.util.concurrent.CompletableFuture.completedFuture(
                        badRequest(Json.newObject().put("error", "Không tìm thấy đơn hàng"))
                    );
                }
                
                // Lấy order_items
                return ws.url(orderServiceUrl + "/order_items")
                    .addHeader("Authorization", authHeader)
                    .get()
                    .thenCompose(itemsResponse -> {
                        if (itemsResponse.getStatus() != 200) {
                            return java.util.concurrent.CompletableFuture.completedFuture(
                                badRequest(Json.newObject().put("error", "Không lấy được danh sách order_items"))
                            );
                        }
                        
                        JsonNode orderItemsArray = itemsResponse.asJson();
                        
                        // Tính toán tổng revenue, cost, profit
                        return calculateOrderReport(orderId, orderItemsArray, authHeader, productServiceUrl);
                    });
            });
    }

    // Helper method: Tính toán báo cáo từ order_items
    private CompletionStage<Result> calculateOrderReport(Integer orderId, JsonNode orderItems, 
                                                         String authHeader, String productServiceUrl) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        
        List<CompletionStage<JsonNode>> productFutures = new ArrayList<>();
        
        // Lọc các items thuộc đơn hàng này
        for (JsonNode item : orderItems) {
            if (item.get("order_id").asInt() == orderId) {
                Integer productId = item.get("product_id").asInt();
                Integer quantity = item.get("quantity").asInt();
                BigDecimal unitPrice = new BigDecimal(item.get("unit_price").asText());
                
                // Tính revenue từ order_items
                BigDecimal itemRevenue = unitPrice.multiply(new BigDecimal(quantity));
                totalRevenue = totalRevenue.add(itemRevenue);
                
                // Gọi ProductService để lấy cost (giả sử cost = 80% của price)
                CompletionStage<JsonNode> productFuture = ws.url(productServiceUrl + "/products/" + productId)
                    .addHeader("Authorization", authHeader)
                    .get()
                    .thenApply(response -> {
                        if (response.getStatus() == 200) {
                            JsonNode product = response.asJson();
                            BigDecimal price = new BigDecimal(product.get("price").asText());
                            // Cost giả định = 80% giá bán
                            BigDecimal cost = price.multiply(new BigDecimal("0.8")).multiply(new BigDecimal(quantity));
                            return Json.newObject()
                                .put("product_id", productId)
                                .put("quantity", quantity)
                                .put("revenue", itemRevenue)
                                .put("cost", cost)
                                .put("profit", itemRevenue.subtract(cost));
                        }
                        return Json.newObject();
                    });
                
                productFutures.add(productFuture);
            }
        }
        
        // Đợi tất cả product lookups hoàn thành
        BigDecimal finalTotalRevenue = totalRevenue;
        return java.util.concurrent.CompletableFuture.allOf(
            productFutures.toArray(new java.util.concurrent.CompletableFuture[0])
        ).thenApply(v -> {
            BigDecimal calculatedTotalCost = BigDecimal.ZERO;
            BigDecimal calculatedTotalProfit = BigDecimal.ZERO;
            List<JsonNode> productReportData = new ArrayList<>();
            
            for (CompletionStage<JsonNode> future : productFutures) {
                try {
                    JsonNode data = future.toCompletableFuture().get();
                    if (data.has("cost")) {
                        calculatedTotalCost = calculatedTotalCost.add(new BigDecimal(data.get("cost").asText()));
                        calculatedTotalProfit = calculatedTotalProfit.add(new BigDecimal(data.get("profit").asText()));
                        productReportData.add(data);
                    }
                } catch (Exception e) {
                    // Skip failed product lookups
                }
            }
            
            BigDecimal totalProfit = finalTotalRevenue.subtract(calculatedTotalCost);
            
            // Lưu vào database
            try (Connection conn = db.getConnection()) {
                // Insert order report
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO orders_reports (order_id, total_revenue, total_cost, total_profit) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                stmt.setInt(1, orderId);
                stmt.setBigDecimal(2, finalTotalRevenue);
                stmt.setBigDecimal(3, calculatedTotalCost);
                stmt.setBigDecimal(4, totalProfit);
                stmt.executeUpdate();
                
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int orderReportId = generatedKeys.getInt(1);
                    
                    // Insert product reports
                    PreparedStatement productStmt = conn.prepareStatement(
                        "INSERT INTO product_reports (order_report_id, product_id, total_sold, revenue, cost, profit) " +
                        "VALUES (?, ?, ?, ?, ?, ?)"
                    );
                    
                    for (JsonNode productData : productReportData) {
                        productStmt.setInt(1, orderReportId);
                        productStmt.setInt(2, productData.get("product_id").asInt());
                        productStmt.setInt(3, productData.get("quantity").asInt());
                        productStmt.setBigDecimal(4, new BigDecimal(productData.get("revenue").asText()));
                        productStmt.setBigDecimal(5, new BigDecimal(productData.get("cost").asText()));
                        productStmt.setBigDecimal(6, new BigDecimal(productData.get("profit").asText()));
                        productStmt.addBatch();
                    }
                    productStmt.executeBatch();
                    
                    return created(Json.newObject()
                        .put("message", "Tạo báo cáo đơn hàng thành công")
                        .put("id", orderReportId)
                        .put("total_revenue", finalTotalRevenue)
                        .put("total_cost", calculatedTotalCost)
                        .put("total_profit", totalProfit));
                }
                
                return internalServerError(Json.newObject().put("error", "Không thể tạo báo cáo"));
                
            } catch (SQLException e) {
                return internalServerError(Json.newObject()
                    .put("error", "Lỗi khi lưu báo cáo: " + e.getMessage()));
            }
        });
    }

    // DELETE /reports/orders/:id - Xóa báo cáo đơn hàng
    public Result deleteOrderReport(Http.Request request, Integer id) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM orders_reports WHERE id = ?")) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                return ok(Json.newObject().put("message", "Xóa báo cáo thành công"));
            } else {
                return notFound(Json.newObject().put("error", "Không tìm thấy báo cáo"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi xóa báo cáo: " + e.getMessage()));
        }
    }
}
