package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.ProductReport;
import play.db.Database;
import play.libs.Json;
import play.mvc.*;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@With(JwtAction.class)
public class ProductReportController extends Controller {

    @Inject
    Database db;

    // GET /reports/products - Lấy danh sách tất cả báo cáo sản phẩm
    public Result getAllProductReports(Http.Request request) {
        List<ProductReport> reports = new ArrayList<>();
        
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM product_reports ORDER BY id DESC")) {
            
            while (rs.next()) {
                ProductReport report = new ProductReport(
                    rs.getInt("id"),
                    rs.getInt("order_report_id"),
                    rs.getInt("product_id"),
                    rs.getInt("total_sold"),
                    rs.getBigDecimal("revenue"),
                    rs.getBigDecimal("cost"),
                    rs.getBigDecimal("profit")
                );
                reports.add(report);
            }
            
            return ok(Json.toJson(reports));
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi lấy danh sách báo cáo sản phẩm: " + e.getMessage()));
        }
    }

    // GET /reports/products/:id - Lấy chi tiết báo cáo sản phẩm
    public Result getProductReportById(Http.Request request, Integer id) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM product_reports WHERE id = ?")) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                ProductReport report = new ProductReport(
                    rs.getInt("id"),
                    rs.getInt("order_report_id"),
                    rs.getInt("product_id"),
                    rs.getInt("total_sold"),
                    rs.getBigDecimal("revenue"),
                    rs.getBigDecimal("cost"),
                    rs.getBigDecimal("profit")
                );
                return ok(Json.toJson(report));
            } else {
                return notFound(Json.newObject().put("error", "Không tìm thấy báo cáo sản phẩm"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi lấy báo cáo sản phẩm: " + e.getMessage()));
        }
    }

    // POST /reports/products - Tạo báo cáo sản phẩm thủ công
    public Result createProductReport(Http.Request request) {
        JsonNode json = request.body().asJson();
        
        if (json == null) {
            return badRequest(Json.newObject().put("error", "Dữ liệu JSON không hợp lệ"));
        }
        
        Integer orderReportId = json.get("order_report_id").asInt();
        Integer productId = json.get("product_id").asInt();
        Integer totalSold = json.get("total_sold").asInt();
        BigDecimal revenue = new BigDecimal(json.get("revenue").asText());
        BigDecimal cost = new BigDecimal(json.get("cost").asText());
        BigDecimal profit = revenue.subtract(cost);
        
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO product_reports (order_report_id, product_id, total_sold, revenue, cost, profit) " +
                 "VALUES (?, ?, ?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, orderReportId);
            stmt.setInt(2, productId);
            stmt.setInt(3, totalSold);
            stmt.setBigDecimal(4, revenue);
            stmt.setBigDecimal(5, cost);
            stmt.setBigDecimal(6, profit);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    return created(Json.newObject()
                        .put("message", "Tạo báo cáo sản phẩm thành công")
                        .put("id", newId));
                }
            }
            
            return internalServerError(Json.newObject().put("error", "Không thể tạo báo cáo sản phẩm"));
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi tạo báo cáo sản phẩm: " + e.getMessage()));
        }
    }

    // DELETE /reports/products/:id - Xóa báo cáo sản phẩm
    public Result deleteProductReport(Http.Request request, Integer id) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM product_reports WHERE id = ?")) {
            
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                return ok(Json.newObject().put("message", "Xóa báo cáo sản phẩm thành công"));
            } else {
                return notFound(Json.newObject().put("error", "Không tìm thấy báo cáo sản phẩm"));
            }
            
        } catch (SQLException e) {
            return internalServerError(Json.newObject()
                .put("error", "Lỗi khi xóa báo cáo sản phẩm: " + e.getMessage()));
        }
    }
}
