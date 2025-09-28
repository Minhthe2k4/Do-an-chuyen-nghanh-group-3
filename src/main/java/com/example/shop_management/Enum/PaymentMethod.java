package com.example.shop_management.Enum;

public enum PaymentMethod {
    CREDIT_LIMIT(1),
    VN_PAY(2),
    CREDIT_CARD(3),
    COD(4),
    SPAY_LATER(5);

    private final int code;

    PaymentMethod(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static PaymentMethod fromCode(int code) {
        for (PaymentMethod pm : values()) {
            if (pm.getCode() == code) return pm;
        }
        throw new IllegalArgumentException("Invalid PaymentMethod code: " + code);
    }
}