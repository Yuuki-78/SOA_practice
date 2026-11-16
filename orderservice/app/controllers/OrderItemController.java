package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.OrderItem;
import play.db.Database;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.*;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@With(JwtAction.class)
public class OrderItemController extends Controller {

    @Inject
    Database db;

    @Inject
    WSClient ws;

    @Inject
    play.Configuration config;

    // GET /order_items - Lấy danh sách tất cả mặt hàng trong đơn hàng
    public Result getAllOrderItems(Http.Request request) {
        List<OrderItem> items = new ArrayList<>();
        
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM order_items ORDER BY id DESC")) {
            
            while (rs.next()) {
                OrderItem item = new OrderItem(
                    rs.getInt("id"),
                    rs.getInt("order_id"),
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("unit_price"),
                    rs.getBigDecimal("total_price")
                );
                items.add(item);
            }
            
            return ok(Json.toJson(items));
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi lấy danh sách mặt hàng: " + e.getMessage()));
        }
    }

    // GET /order_items/:id - Lấy thông tin chi tiết một mặt hàng
    public Result getOrderItemById(Http.Request request, Integer id) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM order_items WHERE id = ?")) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                OrderItem item = new OrderItem(
                    rs.getInt("id"),
                    rs.getInt("order_id"),
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("unit_price"),
                    rs.getBigDecimal("total_price")
                );
                return ok(Json.toJson(item));
            } else {
                return notFound(Json.newObject().put("error", "Không tìm thấy mặt hàng"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi lấy thông tin mặt hàng: " + e.getMessage()));
        }
    }

    // POST /order_items - Tạo mặt hàng mới (kiểm tra tồn kho từ ProductService)
    public CompletionStage<Result> createOrderItem(Http.Request request) {
        JsonNode json = request.body().asJson();
        
        if (json == null) {
            return CompletableFuture.completedFuture(
                badRequest(Json.newObject().put("error", "Dữ liệu JSON không hợp lệ"))
            );
        }
        
        Integer orderId = json.get("order_id").asInt();
        Integer productId = json.get("product_id").asInt();
        Integer quantity = json.get("quantity").asInt();
        
        // Lấy Authorization header từ request hiện tại
        String authHeader = request.header("Authorization").orElse("");
        
        // Gọi ProductService để kiểm tra tồn kho và lấy thông tin sản phẩm
        String productServiceUrl = config.getString("productservice.url");
        
        return ws.url(productServiceUrl + "/products/" + productId)
            .addHeader("Authorization", authHeader)
            .get()
            .thenApply(response -> {
                if (response.getStatus() != 200) {
                    return badRequest(Json.newObject()
                        .put("error", "Không thể lấy thông tin sản phẩm từ ProductService"));
                }
                
                JsonNode productJson = response.asJson();
                String productName = productJson.get("name").asText();
                BigDecimal unitPrice = new BigDecimal(productJson.get("price").asText());
                Integer stock = productJson.get("stock").asInt();
                
                // Kiểm tra tồn kho
                if (stock < quantity) {
                    return badRequest(Json.newObject()
                        .put("error", "Sản phẩm không đủ tồn kho. Còn lại: " + stock));
                }
                
                // Tính tổng giá
                BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(quantity));
                
                // Lưu vào database
                try (Connection conn = db.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, total_price) " +
                         "VALUES (?, ?, ?, ?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                    
                    stmt.setInt(1, orderId);
                    stmt.setInt(2, productId);
                    stmt.setString(3, productName);
                    stmt.setInt(4, quantity);
                    stmt.setBigDecimal(5, unitPrice);
                    stmt.setBigDecimal(6, totalPrice);
                    
                    int affectedRows = stmt.executeUpdate();
                    
                    if (affectedRows > 0) {
                        ResultSet generatedKeys = stmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);
                            
                            // Cập nhật tổng tiền của đơn hàng
                            updateOrderTotalAmount(orderId);
                            
                            return created(Json.newObject()
                                .put("message", "Tạo mặt hàng thành công")
                                .put("id", newId));
                        }
                    }
                    
                    return internalServerError(Json.newObject().put("error", "Không thể tạo mặt hàng"));
                    
                } catch (SQLException e) {
                    return internalServerError(Json.newObject()
                        .put("error", "Lỗi khi tạo mặt hàng: " + e.getMessage()));
                }
            });
    }

    // PUT /order_items/:id - Cập nhật mặt hàng
    public Result updateOrderItem(Http.Request request, Integer id) {
        JsonNode json = request.body().asJson();
        
        if (json == null) {
            return badRequest(Json.newObject().put("error", "Dữ liệu JSON không hợp lệ"));
        }
        
        try (Connection conn = db.getConnection()) {
            // Kiểm tra mặt hàng có tồn tại không
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM order_items WHERE id = ?");
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                return notFound(Json.newObject().put("error", "Không tìm thấy mặt hàng"));
            }
            
            Integer orderId = rs.getInt("order_id");
            
            // Cập nhật mặt hàng
            StringBuilder sql = new StringBuilder("UPDATE order_items SET ");
            List<Object> params = new ArrayList<>();
            
            if (json.has("quantity")) {
                sql.append("quantity = ?, ");
                Integer newQuantity = json.get("quantity").asInt();
                params.add(newQuantity);
                
                // Tính lại total_price
                BigDecimal unitPrice = rs.getBigDecimal("unit_price");
                BigDecimal newTotalPrice = unitPrice.multiply(new BigDecimal(newQuantity));
                sql.append("total_price = ?, ");
                params.add(newTotalPrice);
            }
            
            if (json.has("unit_price")) {
                sql.append("unit_price = ?, ");
                BigDecimal newUnitPrice = new BigDecimal(json.get("unit_price").asText());
                params.add(newUnitPrice);
                
                // Tính lại total_price
                Integer quantity = rs.getInt("quantity");
                BigDecimal newTotalPrice = newUnitPrice.multiply(new BigDecimal(quantity));
                sql.append("total_price = ?, ");
                params.add(newTotalPrice);
            }
            
            // Xóa dấu phẩy cuối cùng
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE id = ?");
            params.add(id);
            
            PreparedStatement updateStmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                updateStmt.setObject(i + 1, params.get(i));
            }
            
            int affectedRows = updateStmt.executeUpdate();
            
            if (affectedRows > 0) {
                // Cập nhật tổng tiền của đơn hàng
                updateOrderTotalAmount(orderId);
                
                return ok(Json.newObject().put("message", "Cập nhật mặt hàng thành công"));
            } else {
                return internalServerError(Json.newObject().put("error", "Không thể cập nhật mặt hàng"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi cập nhật mặt hàng: " + e.getMessage()));
        }
    }

    // DELETE /order_items/:id - Xóa một mặt hàng trong đơn hàng
    public Result deleteOrderItem(Http.Request request, Integer id) {
        try (Connection conn = db.getConnection()) {
            // Lấy order_id trước khi xóa
            PreparedStatement getOrderStmt = conn.prepareStatement("SELECT order_id FROM order_items WHERE id = ?");
            getOrderStmt.setInt(1, id);
            ResultSet rs = getOrderStmt.executeQuery();
            
            if (!rs.next()) {
                return notFound(Json.newObject().put("error", "Không tìm thấy mặt hàng"));
            }
            
            Integer orderId = rs.getInt("order_id");
            
            // Xóa mặt hàng
            PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM order_items WHERE id = ?");
            deleteStmt.setInt(1, id);
            int affectedRows = deleteStmt.executeUpdate();
            
            if (affectedRows > 0) {
                // Cập nhật tổng tiền của đơn hàng
                updateOrderTotalAmount(orderId);
                
                return ok(Json.newObject().put("message", "Xóa mặt hàng thành công"));
            } else {
                return internalServerError(Json.newObject().put("error", "Không thể xóa mặt hàng"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi xóa mặt hàng: " + e.getMessage()));
        }
    }

    // Helper method: Cập nhật tổng tiền của đơn hàng
    private void updateOrderTotalAmount(Integer orderId) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE orders SET total_amount = (SELECT ISNULL(SUM(total_price), 0) FROM order_items WHERE order_id = ?) WHERE id = ?")) {
            
            stmt.setInt(1, orderId);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            // Log error but don't throw
            System.err.println("Lỗi khi cập nhật tổng tiền đơn hàng: " + e.getMessage());
        }
    }
}
