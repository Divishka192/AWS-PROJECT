package com.example.eventnotification.model;

/** The JSON message published to SNS. */
public class OrderEvent {

    private String orderId;
    private String customer;
    private String product;
    private Double amount;
    private String event;

    public OrderEvent() {
    }

    public OrderEvent(String orderId, String customer, String product, Double amount, String event) {
        this.orderId = orderId;
        this.customer = customer;
        this.product = product;
        this.amount = amount;
        this.event = event;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
}
