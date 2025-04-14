package com.example.gateway.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.gateway.enums.ResponseCode;

import lombok.Data;

@Data
public class GatewayResponse {
    // Thông tin kết quả giao dịch
    private ResponseCode responseCode; // DE#39 - Mã enum
    private String transactionStatusDescription; // Mô tả trạng thái dễ hiểu

    // Thông tin tham chiếu
    private String transactionReferenceNumber; // DE#63 - Mã tham chiếu giao dịch NAPAS
    private String retrievalReferenceNumber; // DE#37 - Mã tham chiếu truy vấn (thường là từ request gốc)
    private String systemTraceAuditNumber; // DE#11 - Mã trace gốc từ request

    // Thông tin ngày giờ (đã chuyển đổi định dạng)
    private LocalDateTime transmissionDateTime; // DE#7 - Chuyển thành LocalDateTime
    private LocalDateTime localTransactionDateTime; // DE#12 + DE#13 - Gộp lại thành LocalDateTime
    private LocalDate settlementDate; // DE#15 - Chuyển thành LocalDate

    // Thông tin tài khoản/thẻ
    private String primaryAccountNumber; // DE#2 - PAN/Số tài khoản
    private String fromAccountIdentification; // DE#102
    private String toAccountIdentification; // DE#103
    private String beneficialCardHolderInfo; // DE#120 - Thông tin người hưởng

    // Thông tin giao dịch
    private BigDecimal transactionAmount; // DE#4 - Chuyển thành BigDecimal
    private String currencyCodeTransaction; // DE#49 - Mã tiền tệ (ví dụ "VND")
    private String contentTransfer; // DE#104 - Nội dung chuyển tiền

    // Thông tin đơn vị chấp nhận thẻ (nếu cần)
    private String acquiringInstitutionCode; // DE#32
    private String cardAcceptorTerminalIdentification; // DE#41
    private String cardAcceptorNameLocation; // DE#43
    private String cardAcceptorIdentificationCode; // DE#42 (Thường chỉ có ở chiều đi, nhưng có thể cần log lại ở response)

    // Thông tin bổ sung
    private String additionalPrivateData; // DE#48
    private String selfDefinedField; // DE#60
    private String serviceCode; // DE#62
    private String paymentCode; // DE#67

    // Thông tin bảo mật (nếu Core cần)
    private String messageAuthenticationCode; // DE#128

    
}
