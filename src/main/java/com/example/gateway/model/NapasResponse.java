package com.example.gateway.model;
import com.example.gateway.enums.*;

import lombok.Data;
@Data
public class NapasResponse {
    private MessageType messageType = MessageType.RESPONSE_0210; // DE#0: Message Type (Mặc định 0210)
    private String primaryBitmap;      // Primary Bit Map
    private String secondaryBitmap;    // Secondary Bit Map (DE#1)
    private String primaryAccountNumber; // DE#2: Primary Account Number (PAN)
    private String processingCode;     // DE#3: Processing Code
    private String transactionAmount;   // DE#4: Transaction Amount
    private String transmissionDateTime; // DE#7: Transmission Date and Time
    private String systemTraceAuditNumber; // DE#11: System Trace Audit Number
    private String localTransactionTime; // DE#12: Local Transaction Time
    private String localTransactionDate; // DE#13: Local Transaction Date
    private String settlementDate;       // DE#15: Settlement Date (NAPAS tạo)
    private String acquiringInstitutionCode; // DE#32: Acquiring Institution Code
    private String retrievalReferenceNumber; // DE#37: Retrieval Reference Number (NAPAS tạo)
    private ResponseCode responseCode;     // DE#39: Response Code
    private String cardAcceptorTerminalIdentification; // DE#41: Card Acceptor Terminal Identification
    private String cardAcceptorNameLocation; // DE#43: Card Acceptor Name/Location
    private String additionalPrivateData; // DE#48: Additional Private Data
    private CurrencyCode currencyCodeTransaction; // DE#49: Currency Code, Transaction
    private SelfDefinedFieldValue selfDefinedField; // DE#60: Self-Defined Field
    private String transactionReferenceNumber; // DE#63: Transaction Reference Number (NAPAS tạo)
    private PaymentCodeValue paymentCode; // DE#67: Payment code (chiều về)
    private String fromAccountIdentification; // DE#102: From Account Identification
    private String contentTransfer; // DE#104: Content Transfer
    private String beneficialCardHolderInfo; // DE#120: Beneficial Card Holder Information (chiều về)
    private String messageAuthenticationCode; // DE#128: Message Authentication Code (HMAC) (NAPAS tạo)
    private String toAccountIdentification; // Thêm DE#103 
}