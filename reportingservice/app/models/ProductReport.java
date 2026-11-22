package models;

import java.math.BigDecimal;

public class ProductReport {
    private Integer id;
    private Integer orderReportId;
    private Integer productId;
    private Integer totalSold;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal profit;

    // Constructors
    public ProductReport() {}

    public ProductReport(Integer id, Integer orderReportId, Integer productId, 
                        Integer totalSold, BigDecimal revenue, BigDecimal cost, BigDecimal profit) {
        this.id = id;
        this.orderReportId = orderReportId;
        this.productId = productId;
        this.totalSold = totalSold;
        this.revenue = revenue;
        this.cost = cost;
        this.profit = profit;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrderReportId() {
        return orderReportId;
    }

    public void setOrderReportId(Integer orderReportId) {
        this.orderReportId = orderReportId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getTotalSold() {
        return totalSold;
    }

    public void setTotalSold(Integer totalSold) {
        this.totalSold = totalSold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }
}
