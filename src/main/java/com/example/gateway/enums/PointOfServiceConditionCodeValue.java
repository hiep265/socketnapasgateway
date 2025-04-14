package com.example.gateway.enums;

public enum PointOfServiceConditionCodeValue {
    CUSTOMER_AUTHENTICATED("10"); // Định danh khách hàng được kiểm chứng

    private final String code;

    PointOfServiceConditionCodeValue(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PointOfServiceConditionCodeValue fromCode(String code) {
        for (PointOfServiceConditionCodeValue value : PointOfServiceConditionCodeValue.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy PointOfServiceConditionCodeValue với mã: " + code);
    }
}