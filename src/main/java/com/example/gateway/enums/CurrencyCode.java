package com.example.gateway.enums;

public enum CurrencyCode {
    VND("704"); // Mã tiền tệ VND

    private final String code;

    CurrencyCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
    public static CurrencyCode fromCode(String code) {
        for (CurrencyCode value : CurrencyCode.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy CurrencyCode với mã: " + code);
    }
}