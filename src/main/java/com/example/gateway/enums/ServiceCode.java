package com.example.gateway.enums;
public enum ServiceCode {
    IF_INQ("IF_INQ"); // Truy vấn thông tin chủ thẻ/ tài khoản thụ hưởng

    private final String code;

    ServiceCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ServiceCode fromCode(String code) {
        for (ServiceCode value : ServiceCode.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy ServiceCode với mã: " + code);
    }
}