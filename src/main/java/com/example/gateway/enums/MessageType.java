package com.example.gateway.enums;


public enum MessageType {
    REQUEST_0200("0200"),
    RESPONSE_0210("0210");

    private final String code;

    MessageType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MessageType fromCode(String code) {
        for (MessageType type : MessageType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        // Nếu không tìm thấy MessageType tương ứng, có thể trả về null hoặc
        // throw một exception để xử lý trường hợp mã không hợp lệ.
        return null; // Hoặc throw new IllegalArgumentException("Invalid MessageType code: " + code);
    }
}
