package com.example.gateway.enums;
public enum ToAccountType {
    DEFAULT("20"); // To account 20

    private final String code;

    ToAccountType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}