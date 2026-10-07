package com.example.order_service.exception;

public class OutOfOrderEventException extends RuntimeException {
    public OutOfOrderEventException(String message) {
        super(message);
    }
}