package com.example.gateway.enums;

public enum PointOfServiceEntryModeValue {
    QR_CODE("039"); // Giao dịch bằng mã QR

    private final String code;

    PointOfServiceEntryModeValue(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PointOfServiceEntryModeValue fromCode(String code) {
        for (PointOfServiceEntryModeValue value : PointOfServiceEntryModeValue.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy PointOfServiceEntryModeValue với mã: " + code);
    }
}