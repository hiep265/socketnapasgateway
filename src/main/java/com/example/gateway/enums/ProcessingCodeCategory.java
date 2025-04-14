
package com.example.gateway.enums;
public enum ProcessingCodeCategory {
    TRANSFER_PAYMENT("43"); // Mã loại giao dịch 43

    private final String code;

    ProcessingCodeCategory(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}