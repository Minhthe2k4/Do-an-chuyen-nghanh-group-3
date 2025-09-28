package com.example.shop_management.Enum;

public enum PaymentStatus {
    PENDING(0),
    SUCCESS(1),
    FAILED(2),
    REFUND(3);

    private final int code;

    PaymentStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static PaymentStatus fromCode(int code) {
        for (PaymentStatus ps : values()) {
            if (ps.getCode() == code) return ps;
        }
        throw new IllegalArgumentException("Invalid PaymentStatus code: " + code);
    }
}
