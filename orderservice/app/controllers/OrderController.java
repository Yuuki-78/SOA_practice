package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Order;
import play.db.Database;
import play.libs.Json;
import play.mvc.*;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@With(JwtAction.class)
public class OrderController extends Controller {

    @Inject
    Database db;

    // GET /orders - Lấy danh sách tất cả đơn hàng
    public Result getAllOrders(Http.Request request) {
        List<Order> orders = new ArrayList<>();
        
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM orders ORDER BY created_at DESC")) {
            
            while (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("customer_name"),
                    rs.getString("customer_email"),
                    rs.getBigDecimal("total_amount"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at"),
                    rs.getTimestamp("updated_at")
                );
                orders.add(order);
            }
            
            return ok(Json.toJson(orders));
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi lấy danh sách đơn hàng: " + e.getMessage()));
        }
    }

    // GET /orders/:id - Lấy thông tin chi tiết một đơn hàng
    public Result getOrderById(Http.Request request, Integer id) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM orders WHERE id = ?")) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("customer_name"),
                    rs.getString("customer_email"),
                    rs.getBigDecimal("total_amount"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at"),
                    rs.getTimestamp("updated_at")
                );
                return ok(Json.toJson(order));
            } else {
                return notFound(Json.newObject().put("error", "Không tìm thấy đơn hàng"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi lấy thông tin đơn hàng: " + e.getMessage()));
        }
    }

    // POST /orders - Tạo đơn hàng mới
    public Result createOrder(Http.Request request) {
        JsonNode json = request.body().asJson();
        
        if (json == null) {
            return badRequest(Json.newObject().put("error", "Dữ liệu JSON không hợp lệ"));
        }
        
        String customerName = json.get("customer_name").asText();
        String customerEmail = json.get("customer_email").asText();
        String status = json.has("status") ? json.get("status").asText() : "pending";
        BigDecimal totalAmount = json.has("total_amount") ? 
            new BigDecimal(json.get("total_amount").asText()) : BigDecimal.ZERO;
        
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO orders (customer_name, customer_email, total_amount, status) VALUES (?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, customerName);
            stmt.setString(2, customerEmail);
            stmt.setBigDecimal(3, totalAmount);
            stmt.setString(4, status);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    return created(Json.newObject()
                        .put("message", "Tạo đơn hàng thành công")
                        .put("id", newId));
                }
            }
            
            return internalServerError(Json.newObject().put("error", "Không thể tạo đơn hàng"));
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi tạo đơn hàng: " + e.getMessage()));
        }
    }

    // PUT /orders/:id - Cập nhật trạng thái đơn hàng
    public Result updateOrder(Http.Request request, Integer id) {
        JsonNode json = request.body().asJson();
        
        if (json == null) {
            return badRequest(Json.newObject().put("error", "Dữ liệu JSON không hợp lệ"));
        }
        
        try (Connection conn = db.getConnection()) {
            // Kiểm tra đơn hàng có tồn tại không
            PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM orders WHERE id = ?");
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                return notFound(Json.newObject().put("error", "Không tìm thấy đơn hàng"));
            }
            
            // Cập nhật đơn hàng
            StringBuilder sql = new StringBuilder("UPDATE orders SET ");
            List<Object> params = new ArrayList<>();
            
            if (json.has("customer_name")) {
                sql.append("customer_name = ?, ");
                params.add(json.get("customer_name").asText());
            }
            if (json.has("customer_email")) {
                sql.append("customer_email = ?, ");
                params.add(json.get("customer_email").asText());
            }
            if (json.has("total_amount")) {
                sql.append("total_amount = ?, ");
                params.add(new BigDecimal(json.get("total_amount").asText()));
            }
            if (json.has("status")) {
                sql.append("status = ?, ");
                params.add(json.get("status").asText());
            }
            
            // Thêm updated_at
            sql.append("updated_at = GETDATE() WHERE id = ?");
            params.add(id);
            
            PreparedStatement updateStmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                updateStmt.setObject(i + 1, params.get(i));
            }
            
            int affectedRows = updateStmt.executeUpdate();
            
            if (affectedRows > 0) {
                return ok(Json.newObject().put("message", "Cập nhật đơn hàng thành công"));
            } else {
                return internalServerError(Json.newObject().put("error", "Không thể cập nhật đơn hàng"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi cập nhật đơn hàng: " + e.getMessage()));
        }
    }

    // DELETE /orders/:id - Xóa một đơn hàng
    public Result deleteOrder(Http.Request request, Integer id) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM orders WHERE id = ?")) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                return ok(Json.newObject().put("message", "Xóa đơn hàng thành công"));
            } else {
                return notFound(Json.newObject().put("error", "Không tìm thấy đơn hàng"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi xóa đơn hàng: " + e.getMessage()));
        }
    }
}
