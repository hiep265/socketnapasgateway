package com.example.gateway.enums;
public enum ResponseCode {
    SUCCESS_00("00", "Giao dịch thành công"),
    DECLINE_05("05", "Từ chối phản hồi"),
    TIMEOUT_68("68", "Timeout");

    private final String code;
    private final String description;

    ResponseCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ResponseCode fromCode(String code) {
        for (ResponseCode value : ResponseCode.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy ResponseCode với mã: " + code);
    }
}