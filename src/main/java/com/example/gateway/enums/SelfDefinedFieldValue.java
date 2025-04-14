package com.example.gateway.enums;
public enum SelfDefinedFieldValue {
    QR_TRANSFER("99"); // Chuyển tiền qua mã QR

    private final String code;

    SelfDefinedFieldValue(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SelfDefinedFieldValue fromCode(String code) {
        for (SelfDefinedFieldValue value : SelfDefinedFieldValue.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy SelfDefinedFieldValue với mã: " + code);
    }
}