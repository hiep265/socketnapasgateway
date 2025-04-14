package com.example.gateway.enums;

public enum PaymentCodeValue {
    QUICK_TRANSFER_247("91"); // Dịch vụ chuyển tiền 24/7

    private final String code;

    PaymentCodeValue(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PaymentCodeValue fromCode(String code) {
        for (PaymentCodeValue value : PaymentCodeValue.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy PaymentCodeValue với mã: " + code);
    }
}