package com.example.gateway.enums;

public enum FromAccountType {
    DEFAULT("20"); // From account 20

    private final String code;

    FromAccountType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}