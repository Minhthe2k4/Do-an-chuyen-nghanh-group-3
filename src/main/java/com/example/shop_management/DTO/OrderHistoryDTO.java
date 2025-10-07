package com.example.shop_management.DTO;

import org.w3c.dom.Text;

import java.math.BigDecimal;

public class OrderHistoryDTO {

    private String username;
    private String order_items;
    private Integer status;

    private Integer shipping_status;

    private String payment_method;

    private String address;
    private BigDecimal total_amount;

    public OrderHistoryDTO(String username, BigDecimal total_amount, String address,
                           com.example.shop_management.Enum.PaymentMethod payment_method,
                           Integer shipping_status, Integer status, String order_items) {
        this.username = username;
        this.total_amount = total_amount;
        this.address = address;
        this.payment_method = payment_method != null ? payment_method.name() : null;
        this.shipping_status = shipping_status;
        this.status = status;
        this.order_items = order_items;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getTotal_amount() {
        return total_amount;
    }

    public void setTotal_amount(BigDecimal total_amount) {
        this.total_amount = total_amount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPayment_method() {
        return payment_method;
    }

    public void setPayment_method(String payment_method) {
        this.payment_method = payment_method;
    }

    public Integer getShipping_status() {
        return shipping_status;
    }

    public void setShipping_status(Integer shipping_status) {
        this.shipping_status = shipping_status;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getOrder_items() {
        return order_items;
    }

    public void setOrder_items(String order_items) {
        this.order_items = order_items;
    }
}
