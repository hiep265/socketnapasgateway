package com.example.gateway.model;

import lombok.Data;

@Data
public class GatewayRequest {
    private String pan;
    private Double amount;
    private String transmissionTime; // "HHmmss"
    private String transmissionDate; // "MMdd"
    private String terminalId;
    private String privateData;
    private String qrCodeTransfer; // "99" if QR code transfer
    private String fromAccount;
    private String content;
    private String beneficialInfo;
    private int systemTraceAuditNumber; 
    private String localTransactionTime;
    private String localTransactionDate;
    private String bankName;
    private String location;
    private String senderName;
    private String addressSender;
    private String toAccountIdentification;
}
