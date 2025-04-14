package com.example.gateway.until;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.example.gateway.model.GatewayResponse;
import com.example.gateway.model.NapasResponse;

public class TranformData {
    public static GatewayResponse transformNapasResponseToGatewayResponse(NapasResponse napasResponse) {
        GatewayResponse gatewayResponse = new GatewayResponse();

        // --- Mapping trực tiếp các trường không cần chuyển đổi nhiều ---
        gatewayResponse.setResponseCode(napasResponse.getResponseCode()); // DE#39
        gatewayResponse.setTransactionReferenceNumber(napasResponse.getTransactionReferenceNumber()); // DE#63
        gatewayResponse.setRetrievalReferenceNumber(napasResponse.getRetrievalReferenceNumber()); // DE#37
        gatewayResponse.setSystemTraceAuditNumber(napasResponse.getSystemTraceAuditNumber()); // DE#11
        gatewayResponse.setPrimaryAccountNumber(napasResponse.getPrimaryAccountNumber()); // DE#2
        gatewayResponse.setFromAccountIdentification(napasResponse.getFromAccountIdentification()); // DE#102
        gatewayResponse.setToAccountIdentification(napasResponse.getToAccountIdentification()); // DE#103
        gatewayResponse.setBeneficialCardHolderInfo(napasResponse.getBeneficialCardHolderInfo()); // DE#120
        gatewayResponse.setContentTransfer(napasResponse.getContentTransfer()); // DE#104
        gatewayResponse.setAcquiringInstitutionCode(napasResponse.getAcquiringInstitutionCode()); // DE#32
        gatewayResponse.setCardAcceptorTerminalIdentification(napasResponse.getCardAcceptorTerminalIdentification()); // DE#41
        gatewayResponse.setCardAcceptorNameLocation(napasResponse.getCardAcceptorNameLocation()); // DE#43
        // gatewayResponse.setCardAcceptorIdentificationCode(...); // DE#42 thường không có ở response
        gatewayResponse.setAdditionalPrivateData(napasResponse.getAdditionalPrivateData()); // DE#48
        gatewayResponse.setMessageAuthenticationCode(napasResponse.getMessageAuthenticationCode()); // DE#128
       // gatewayResponse.setProcessingCode(napasResponse.getProcessingCode()); // DE#3 (giữ nguyên nếu Core cần)

        // --- Mapping và Chuyển đổi ---

        // Trạng thái giao dịch
        if (napasResponse.getResponseCode() != null) {
            gatewayResponse.setTransactionStatusDescription(napasResponse.getResponseCode().getDescription());
        }

        // Số tiền giao dịch (DE#4) - Chuyển từ chuỗi 12 ký tự sang BigDecimal
        gatewayResponse.setTransactionAmount(parseNapasAmount(napasResponse.getTransactionAmount()));

        // Mã tiền tệ (DE#49) - Có thể chuyển từ code số sang mã chữ (nếu enum CurrencyCode có hỗ trợ)
        if (napasResponse.getCurrencyCodeTransaction() != null) {
            // Giả sử enum CurrencyCode có phương thức getAlphaCode() hoặc tương tự
            // Nếu không, bạn có thể tạo một map hoặc switch-case để chuyển đổi
            // gatewayResponse.setCurrencyCodeTransaction(napasResponse.getCurrencyCodeTransaction().getAlphaCode());
            // Hoặc giữ nguyên mã số nếu Core System hiểu:
            gatewayResponse.setCurrencyCodeTransaction(napasResponse.getCurrencyCodeTransaction().getCode());
        }

        // Ngày giờ (DE#7, DE#12, DE#13, DE#15) - Chuyển đổi định dạng
        gatewayResponse.setTransmissionDateTime(parseNapasDateTime(napasResponse.getTransmissionDateTime(), "MMddHHmmss"));
        gatewayResponse.setLocalTransactionDateTime(parseNapasLocalDateTime(napasResponse.getLocalTransactionDate(), napasResponse.getLocalTransactionTime(), "MMdd", "HHmmss"));
        gatewayResponse.setSettlementDate(parseNapasLocalDate(napasResponse.getSettlementDate(), "MMdd"));

        // Các trường Enum khác (DE#60, DE#62, DE#67) - Có thể lấy code hoặc mô tả
        if (napasResponse.getSelfDefinedField() != null) {
            gatewayResponse.setSelfDefinedField(napasResponse.getSelfDefinedField().getCode());
        }
        // if (napasResponse.getServiceCode() != null) {
        //     gatewayResponse.setServiceCode(napasResponse.getServiceCode().getCode());
        // }
        if (napasResponse.getPaymentCode() != null) {
            gatewayResponse.setPaymentCode(napasResponse.getPaymentCode().getCode());
        }

        return gatewayResponse;
    }

    // --- Helper Functions ---

    /**
     * Chuyển đổi chuỗi số tiền 12 ký tự của NAPAS thành BigDecimal.
     * Giả định 2 chữ số cuối là phần thập phân.
     *
     * @param napasAmountString Chuỗi số tiền 12 ký tự từ NAPAS (DE#4).
     * @return BigDecimal tương ứng, hoặc null nếu input không hợp lệ.
     */
    private static BigDecimal parseNapasAmount(String napasAmountString) {
        if (napasAmountString == null || napasAmountString.length() != 12) {
            return null;
        }
        try {
            // Lấy phần nguyên và phần thập phân
            String integerPart = napasAmountString.substring(0, 10);
            String decimalPart = napasAmountString.substring(10, 12);
            // Tạo chuỗi có dấu chấm thập phân
            String decimalString = integerPart + "." + decimalPart;
            return new BigDecimal(decimalString);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing NAPAS amount string: " + napasAmountString + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Chuyển đổi chuỗi ngày giờ từ định dạng NAPAS sang LocalDateTime.
     *
     * @param napasDateTimeString Chuỗi ngày giờ từ NAPAS.
     * @param pattern             Định dạng của chuỗi ngày giờ NAPAS (ví dụ "MMddHHmmss").
     * @return LocalDateTime tương ứng, hoặc null nếu input không hợp lệ.
     */
    private static LocalDateTime parseNapasDateTime(String napasDateTimeString, String pattern) {
        if (napasDateTimeString == null || pattern == null) {
            return null;
        }
        try {
            DateTimeFormatter napasFormatter = DateTimeFormatter.ofPattern(pattern);
            // Cần thêm năm vào để parse thành LocalDateTime
            // Giả định năm hiện tại, cần logic xử lý nếu giao dịch qua năm
            int currentYear = Year.now().getValue();
            // Tùy thuộc vào pattern, cần tạo TemporalAccessor phù hợp
            if (pattern.equals("MMddHHmmss")) {
                // Parse MMdd trước, thêm năm, rồi parse HHmmss
                LocalDate datePart = LocalDate.parse(napasDateTimeString.substring(0, 4) + currentYear, DateTimeFormatter.ofPattern("MMddyyyy"));
                LocalTime timePart = LocalTime.parse(napasDateTimeString.substring(4), DateTimeFormatter.ofPattern("HHmmss"));
                return LocalDateTime.of(datePart, timePart);
            }
            // Thêm các trường hợp pattern khác nếu cần
            return LocalDateTime.parse(napasDateTimeString + currentYear, DateTimeFormatter.ofPattern(pattern + "yyyy"));

        } catch (DateTimeParseException e) {
            System.err.println("Error parsing NAPAS date/time string: " + napasDateTimeString + " with pattern " + pattern + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Gộp chuỗi ngày và giờ từ NAPAS thành LocalDateTime.
     *
     * @param napasDateString Chuỗi ngày (ví dụ "MMdd").
     * @param napasTimeString Chuỗi giờ (ví dụ "HHmmss").
     * @param datePattern     Định dạng của chuỗi ngày.
     * @param timePattern     Định dạng của chuỗi giờ.
     * @return LocalDateTime tương ứng, hoặc null nếu input không hợp lệ.
     */
    private static LocalDateTime parseNapasLocalDateTime(String napasDateString, String napasTimeString, String datePattern, String timePattern) {
        LocalDate datePart = parseNapasLocalDate(napasDateString, datePattern);
        LocalTime timePart = parseNapasLocalTime(napasTimeString, timePattern);

        if (datePart != null && timePart != null) {
            return LocalDateTime.of(datePart, timePart);
        }
        return null;
    }

     /**
     * Chuyển đổi chuỗi ngày từ định dạng NAPAS sang LocalDate.
     *
     * @param napasDateString Chuỗi ngày từ NAPAS (ví dụ "MMdd").
     * @param pattern         Định dạng của chuỗi ngày NAPAS.
     * @return LocalDate tương ứng, hoặc null nếu input không hợp lệ.
     */
    private static LocalDate parseNapasLocalDate(String napasDateString, String pattern) {
         if (napasDateString == null || pattern == null) {
             return null;
         }
         try {
             DateTimeFormatter napasFormatter = DateTimeFormatter.ofPattern(pattern);
             // Thêm năm hiện tại để parse thành LocalDate
             int currentYear = Year.now().getValue();
             // Cần xử lý logic nếu ngày tháng gần cuối năm/đầu năm
             return LocalDate.parse(napasDateString + currentYear, DateTimeFormatter.ofPattern(pattern + "yyyy"));
         } catch (DateTimeParseException e) {
             System.err.println("Error parsing NAPAS date string: " + napasDateString + " with pattern " + pattern + " - " + e.getMessage());
             return null;
         }
     }

    /**
     * Chuyển đổi chuỗi giờ từ định dạng NAPAS sang LocalTime.
     *
     * @param napasTimeString Chuỗi giờ từ NAPAS (ví dụ "HHmmss").
     * @param pattern         Định dạng của chuỗi giờ NAPAS.
     * @return LocalTime tương ứng, hoặc null nếu input không hợp lệ.
     */
    private static LocalTime parseNapasLocalTime(String napasTimeString, String pattern) {
        if (napasTimeString == null || pattern == null) {
            return null;
        }
        try {
            DateTimeFormatter napasFormatter = DateTimeFormatter.ofPattern(pattern);
            return LocalTime.parse(napasTimeString, napasFormatter);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing NAPAS time string: " + napasTimeString + " with pattern " + pattern + " - " + e.getMessage());
            return null;
        }
    }

}
