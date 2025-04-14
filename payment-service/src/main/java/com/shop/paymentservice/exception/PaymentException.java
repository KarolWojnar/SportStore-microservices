package com.shop.paymentservice.exception;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
