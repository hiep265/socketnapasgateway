package com.example.gateway.model;

import lombok.Data;
import com.example.gateway.enums.*;
@Data
public class NapasRequest {
    private MessageType messageType = MessageType.REQUEST_0200; // DE#0: Message Type (Mặc định 0200)
    private String primaryBitmap;      // Primary Bit Map
    private String secondaryBitmap;    // Secondary Bit Map (DE#1)
    private String primaryAccountNumber; // DE#2: Primary Account Number (PAN)
    private String processingCode;     // DE#3: Processing Code
    private String transactionAmount;   // DE#4: Transaction Amount
    private String transmissionDateTime; // DE#7: Transmission Date and Time
    private String systemTraceAuditNumber; // DE#11: System Trace Audit Number
    private String localTransactionTime; // DE#12: Local Transaction Time
    private String localTransactionDate; // DE#13: Local Transaction Date
    private PointOfServiceEntryModeValue pointOfServiceEntryMode; // DE#22: Point-Of-Service Entry Mode (chiều đi)
    private PointOfServiceConditionCodeValue pointOfServiceConditionCode; // DE#25: Point-of-Service Condition Code (chiều đi)
    private String acquiringInstitutionCode; // DE#32: Acquiring Institution Code
    private String cardAcceptorTerminalIdentification; // DE#41: Card Acceptor Terminal Identification
    private String cardAcceptorIdentificationCode; // DE#42: Card Acceptor Identification Code (chiều đi)
    private String cardAcceptorNameLocation; // DE#43: Card Acceptor Name/Location (chiều đi)
    private String additionalPrivateData; // DE#48: Additional Private Data
    private CurrencyCode currencyCodeTransaction = CurrencyCode.VND; // DE#49: Currency Code, Transaction (Mặc định VND)
    private SelfDefinedFieldValue selfDefinedField; // DE#60: Self-Defined Field
    private ServiceCode serviceCode = ServiceCode.IF_INQ; // DE#62: Service Code (Mặc định IF_INQ)
    private String fromAccountIdentification; // DE#102: From Account Identification
    private String contentTransfer; // DE#104: Content Transfer
    private String beneficialCardHolderInfo; // DE#120: Beneficial Card Holder Information
    private String retrievalReferenceNumber;
    // DE#128: Message Authentication Code (HMAC) - Có thể được thêm vào trước khi gửi hoặc xử lý riêng
    private String toAccountIdentification; // Thêm DE#103
    private String messageAuthenticationCode; // Thêm DE#128
}