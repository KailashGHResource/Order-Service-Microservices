package com.example.order_service.exception;

public class OrderAbortedException extends RuntimeException {
    public OrderAbortedException(String message) {
        super(message);
    }
}