package com.example.gateway.dto;

import lombok.Data;

@Data
public class InquiryRequest {
    private String messageType;                // Loại thông điệp (ví dụ: "0200")
    private String primaryBitMap;              // Bitmap chính (64 bit)
    private String secondaryBitMap;            // Bitmap phụ (64 bit)
    private String pan;                        // Số tài khoản/thẻ
    private String processingCode;             // Mã xử lý giao dịch
    private String transactionAmount;          // Số tiền giao dịch (12 chữ số)
    private String transmissionDateTime;       // Thời gian gửi (MMDDhhmmss)
    private String systemTraceAuditNumber;     // Số tham chiếu hệ thống
    private String localTransactionTime;       // Thời gian giao dịch cục bộ (hhmmss)
    private String localTransactionDate;       // Ngày giao dịch cục bộ (MMDD)
    private String pointOfServiceEntryMode;    // Phương thức nhập dữ liệu
    private String pointOfServiceConditionCode;// Mã điều kiện dịch vụ
    private String acquiringInstitutionCode;   // Mã tổ chức thu nhận
    private String cardAcceptorTerminalIdentification; // Mã thiết bị chấp nhận thẻ
    private String cardAcceptorIdentificationCode;     // Mã định danh chấp nhận thẻ
    private String cardAcceptorNameLocation;   // Tên và địa điểm chấp nhận thẻ
    private String additionalPrivateData;      // Dữ liệu riêng bổ sung
    private String currencyCode;               // Mã tiền tệ (ví dụ: "704" cho VND)
    private String selfDefinedField;           // Trường tự định nghĩa
    private String serviceCode;                // Mã dịch vụ
    private String fromAccountIdentification;  // Định danh tài khoản nguồn
    private String contentTransfer;            // Nội dung chuyển khoản
    private String messageAuthenticationCode;  // Mã xác thực thông điệp

    
}
