package com.example.shop_management.DTO;

import org.w3c.dom.Text;

import java.math.BigDecimal;

public class OrderHistoryDTO {

    private String username;
    private String order_items;
    private Integer status;
    private BigDecimal total_amount;

    public OrderHistoryDTO(String username, String order_items, Integer status, BigDecimal total_amount) {
        this.username = username;
        this.total_amount = total_amount;
        this.order_items = order_items;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOrder_items() {
        return order_items;
    }

    public void setOrder_items(String order_items) {
        this.order_items = order_items;
    }

    public BigDecimal getTotal_amount() {
        return total_amount;
    }

    public void setTotal_amount(BigDecimal total_amount) {
        this.total_amount = total_amount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
