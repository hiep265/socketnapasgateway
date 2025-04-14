package com.example.gateway.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import com.example.gateway.entity.ActiveLogEntity;

import com.example.gateway.entity.TransactionEntity;
import com.example.gateway.enums.CurrencyCode;
import com.example.gateway.enums.FromAccountType;
import com.example.gateway.enums.MessageType;
import com.example.gateway.enums.PaymentCodeValue;
import com.example.gateway.enums.PointOfServiceConditionCodeValue;
import com.example.gateway.enums.PointOfServiceEntryModeValue;
import com.example.gateway.enums.ProcessingCodeCategory;
import com.example.gateway.enums.ResponseCode;
import com.example.gateway.enums.SelfDefinedFieldValue;
import com.example.gateway.enums.ServiceCode;
import com.example.gateway.enums.ToAccountType;
import com.example.gateway.feign.NapasClient;
import com.example.gateway.model.GatewayRequest;
import com.example.gateway.model.GatewayResponse;
import com.example.gateway.model.NapasRequest;

import com.example.gateway.model.NapasResponse;
import com.example.gateway.repository.ActiveLogRepository;
import com.example.gateway.repository.TransactionRepository;
import com.example.gateway.until.BitmapUtil;
import com.example.gateway.until.TranformData;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GatewayService {

    @Autowired
    private NapasClient napasClient;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private ActiveLogRepository activeLogRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${HMAC_SECRET_KEY}")
    private  String SECRET_KEY;
    public NapasResponse processNapasQuery(GatewayRequest gatewayRequest) throws Exception {
        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity = transactionRepository.save(transactionEntity); // Save transaction to get ID

        ActiveLogEntity coreToGatewayLog = SaveLog.createActiveLog(transactionEntity, "CoreToGateway");
        try {
            coreToGatewayLog.setJsonData(objectMapper.writeValueAsString(gatewayRequest));
            coreToGatewayLog.setStatus("SUCCESS");
            activeLogRepository.save(coreToGatewayLog);
        } catch (Exception e) {
            coreToGatewayLog.setStatus("FAILED");
            coreToGatewayLog.setJsonData("Error serializing GatewayRequest JSON: " + e.getMessage());
            activeLogRepository.save(coreToGatewayLog);
            throw e; // Re-throw exception after logging failure
        }

        // 2. Transform GatewayRequest sang NapasRequest
        NapasRequest napasRequest = transformGatewayRequestToNapasRequest(gatewayRequest);
        ActiveLogEntity gatewayToNapasRequestLog = SaveLog.createActiveLog(transactionEntity, "GatewayToNapasRequest");
        try {
            gatewayToNapasRequestLog.setJsonData(objectMapper.writeValueAsString(napasRequest));
            gatewayToNapasRequestLog.setStatus("SUCCESS");
            activeLogRepository.save(gatewayToNapasRequestLog);
        } catch (Exception e) {
            gatewayToNapasRequestLog.setStatus("FAILED");
            gatewayToNapasRequestLog.setJsonData("Error serializing NapasRequest JSON: " + e.getMessage());
            activeLogRepository.save(gatewayToNapasRequestLog);
            throw e;
        }
        // 3. Serialize NapasRequest to String
        String napasRequestString = serializeNapasRequestToString(napasRequest);

       // 4. Send String via UDP and Receive Response String
        String napasResponseString = sendTcpMessage(napasRequestString);
        if ("NO_RESPONSE".equals(napasResponseString)) {
            System.err.println("❌ Không nhận được phản hồi từ NAPAS. Dừng chương trình.");
            ActiveLogEntity gatewayToNapasTimeoutLog = SaveLog.createActiveLog(transactionEntity, "GatewayToNapasResponse");
            gatewayToNapasTimeoutLog.setStatus("TIMEOUT");
            gatewayToNapasTimeoutLog.setJsonData("Timeout waiting for NAPAS response.");
            ActiveLogEntity napasToGatewayResponseLog = SaveLog.createActiveLog(transactionEntity, "NapasToGatewayResponse");
            napasToGatewayResponseLog.setStatus("FAILED");
            napasToGatewayResponseLog.setJsonData("don't have JSON because time out: " );
            activeLogRepository.save(napasToGatewayResponseLog);
            activeLogRepository.save(gatewayToNapasTimeoutLog);
            throw new TimeoutException("Không nhận được phản hồi từ NAPAS trong thời gian quy định.");
        }
        // 5. Deserialize Response String to NapasResponse
        NapasResponse napasResponse = deserializeStringToNapasResponse(napasResponseString);
        ActiveLogEntity napasToGatewayResponseLog = SaveLog.createActiveLog(transactionEntity, "NapasToGatewayResponse");
        try {
            napasToGatewayResponseLog.setJsonData(objectMapper.writeValueAsString(napasResponse));
            napasToGatewayResponseLog.setStatus("SUCCESS");
            activeLogRepository.save(napasToGatewayResponseLog);
        } catch (Exception e) {
            napasToGatewayResponseLog.setStatus("FAILED");
            napasToGatewayResponseLog.setJsonData("Error serializing NapasResponse JSON: " + e.getMessage());
            activeLogRepository.save(napasToGatewayResponseLog);
            throw e;
        }

        // Gọi NAPAS qua Feign Client
       // NapasResponse napasResponse = napasClient.sendNapasRequest(napasRequest);

         // 6. Transform NapasResponse to GatewayResponse (for Core system)
        GatewayResponse coreResponse = TranformData.transformNapasResponseToGatewayResponse(napasResponse);

        // 6. Lưu GatewayResponse (for Core) - In this case, NapasResponse is returned to core
        ActiveLogEntity gatewayToCoreResponseLog = SaveLog.createActiveLog(transactionEntity, "GatewayToCoreResponse");
        try {
            gatewayToCoreResponseLog.setJsonData(objectMapper.writeValueAsString(coreResponse)); // Or transform to CoreResponseDTO if needed
            gatewayToCoreResponseLog.setStatus("SUCCESS");
            activeLogRepository.save(gatewayToCoreResponseLog);
        } catch (Exception e) {
            gatewayToCoreResponseLog.setStatus("FAILED");
            gatewayToCoreResponseLog.setJsonData("Error serializing GatewayToCoreResponse JSON: " + e.getMessage());
            activeLogRepository.save(gatewayToCoreResponseLog);
            throw e;
        }

        return napasResponse;
    }
    private static final String NAPAS_HOST = "localhost"; // Thay bằng IP NAPAS thực tế
    private static final int NAPAS_PORT = 8082; // Thay bằng PORT NAPAS TCP thực tế
    private static final int TIMEOUT_MS = 300000; // 3 giây timeout
    private String sendTcpMessage(String message) {
        Socket socket = null; // Khai báo Socket bên ngoài try để có thể đóng trong finally
        try {
            socket = new Socket(); // Tạo mới Socket TCP
            socket.connect(new InetSocketAddress(NAPAS_HOST, NAPAS_PORT), TIMEOUT_MS); // Kết nối tới NAPAS với timeout
            socket.setSoTimeout(TIMEOUT_MS); // Cài đặt timeout cho socket operations (receive)

            // Gửi dữ liệu qua TCP
            OutputStream outputStream = socket.getOutputStream(); // Lấy OutputStream từ Socket
            byte[] buffer = message.getBytes(); // Chuyển message thành byte array
            outputStream.write(buffer); // Gửi dữ liệu đi
            outputStream.flush(); // Đảm bảo dữ liệu được gửi đi ngay lập tức
            System.out.println("Gateway sent message to NAPAS via TCP: " + message); // Log message đã gửi

            // Nhận phản hồi qua TCP
            InputStream inputStream = socket.getInputStream(); // Lấy InputStream từ Socket
            byte[] receiveBuffer = new byte[2048]; // Chuẩn bị bộ đệm nhận dữ liệu
            int bytesRead = inputStream.read(receiveBuffer); // Đọc dữ liệu phản hồi
            if (bytesRead == -1) {
                // Socket có thể đã đóng từ phía server
                System.err.println("❌ NAPAS closed connection unexpectedly.");
                return "NO_RESPONSE";
            }

            String responseMessage = new String(receiveBuffer, 0, bytesRead); // Chuyển byte array phản hồi thành String
            System.out.println("Gateway received response from NAPAS via TCP: " + responseMessage); // Log response nhận được
            return responseMessage; // Trả về response

        } catch (SocketTimeoutException e) {
            System.err.println("❌ Timeout: Không nhận được phản hồi từ NAPAS sau " + TIMEOUT_MS + "ms.");
            return "NO_RESPONSE";
        } catch (IOException e) {
            System.err.println("TCP communication error: " + e.getMessage());
            return "ERROR"; // Xử lý lỗi TCP
        } finally {
            if (socket != null) {
                try {
                    socket.close(); // Đóng socket sau khi hoàn thành hoặc gặp lỗi
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }
    private NapasResponse deserializeStringToNapasResponse(String napasResponseString) {
        NapasResponse napasResponse = new NapasResponse();
        int currentIndex = 0;

        // Message Type (DE#0)
        napasResponse.setMessageType(MessageType.fromCode(extractFixedLengthValue(napasResponseString, currentIndex, 4)));
        currentIndex += 4;

        // Primary Bitmap
        String primaryBitmapHex = extractFixedLengthValue(napasResponseString, currentIndex, 16);
        napasResponse.setPrimaryBitmap(primaryBitmapHex);
        currentIndex += 16;
        Map<Integer, Boolean> primaryBitmap = BitmapUtil.parseBitmap(primaryBitmapHex,0);
        Map<Integer, Boolean> secondBitmap = null;
        // Secondary Bitmap (DE#1)
        if (primaryBitmap.getOrDefault(1, false)) {
            String secondaryBitmapHex = extractFixedLengthValue(napasResponseString, currentIndex, 16);
            napasResponse.setSecondaryBitmap(secondaryBitmapHex);
            secondBitmap= BitmapUtil.parseBitmap(secondaryBitmapHex,1);
            currentIndex += 16;
        }
        //Map<Integer, Boolean> secondaryBitmap = napasResponse.getSecondaryBitmap() != null ? BitmapUtil.parseBitmap(napasResponse.getSecondaryBitmap()) : new HashMap<>();


        // DE#2: Primary Account Number (PAN)
        if (primaryBitmap.getOrDefault(2, false)) {
            napasResponse.setPrimaryAccountNumber(extractLLVARValue(napasResponseString, currentIndex, 2));
            currentIndex += 2 + Integer.parseInt(napasResponseString.substring(currentIndex, currentIndex + 2));
        }
        //DE#3: Processing code
        if (primaryBitmap.getOrDefault(3, false)) {
            napasResponse.setProcessingCode(extractFixedLengthValue(napasResponseString, currentIndex, 6));
            currentIndex += 6;
        }
        //DE#4: Transaction amount
        if (primaryBitmap.getOrDefault(4, false)) {
            napasResponse.setTransactionAmount(extractFixedLengthValue(napasResponseString, currentIndex, 12));
            currentIndex += 12;
        }
        //DE#7: Transmission date and time
        if (primaryBitmap.getOrDefault(7, false)) {
            napasResponse.setTransmissionDateTime(extractFixedLengthValue(napasResponseString, currentIndex, 10));
            currentIndex += 10;
        }
        //DE#11: System trace audit number
        if (primaryBitmap.getOrDefault(11, false)) {
            napasResponse.setSystemTraceAuditNumber(extractFixedLengthValue(napasResponseString, currentIndex, 6));
            currentIndex += 6;
        }
        //DE#12: Local transaction time
        if (primaryBitmap.getOrDefault(12, false)) {
            napasResponse.setLocalTransactionTime(extractFixedLengthValue(napasResponseString, currentIndex, 6));
            currentIndex += 6;
        }
        //DE#13: Local transaction date
        if (primaryBitmap.getOrDefault(13, false)) {
            napasResponse.setLocalTransactionDate(extractFixedLengthValue(napasResponseString, currentIndex, 4));
            currentIndex += 4;
        }
        //DE#15: Settlement date
        if (primaryBitmap.getOrDefault(15, false)) {
            napasResponse.setSettlementDate(extractFixedLengthValue(napasResponseString, currentIndex, 4));
            currentIndex += 4;
        }
        //DE#32: Acquiring institution code
        if (primaryBitmap.getOrDefault(32, false)) {
            napasResponse.setAcquiringInstitutionCode(extractLLVARValue(napasResponseString, currentIndex, 2));
            currentIndex += 2 + Integer.parseInt(napasResponseString.substring(currentIndex, currentIndex + 2));
        }
        //DE#37: Retrieval reference number
        if (primaryBitmap.getOrDefault(37, false)) {
            napasResponse.setRetrievalReferenceNumber(extractFixedLengthValue(napasResponseString, currentIndex, 12));
            currentIndex += 12;
        }
        //DE#39: Response code
        if (primaryBitmap.getOrDefault(39, false)) {
            napasResponse.setResponseCode(ResponseCode.fromCode(extractFixedLengthValue(napasResponseString, currentIndex, 2))); // Assuming 2-digit response code
            currentIndex += 2;
        }
        //DE#41: Card acceptor terminal identification
        if (primaryBitmap.getOrDefault(41, false)) {
            napasResponse.setCardAcceptorTerminalIdentification(extractFixedLengthValue(napasResponseString, currentIndex, 8));
            currentIndex += 8;
        }
        //DE#43: Card acceptor name location
        if (primaryBitmap.getOrDefault(43, false)) {
            napasResponse.setCardAcceptorNameLocation(extractLLVARValue(napasResponseString, currentIndex, 2));
            currentIndex += 2 + Integer.parseInt(napasResponseString.substring(currentIndex, currentIndex + 2));
        }
        //DE#48: Additional private data
        if (primaryBitmap.getOrDefault(48, false)) {
            napasResponse.setAdditionalPrivateData(extractLLLVARValue(napasResponseString, currentIndex, 3));
            currentIndex += 3 + Integer.parseInt(napasResponseString.substring(currentIndex, currentIndex + 3));
        }
        //DE#49: Currency code transaction
        if (primaryBitmap.getOrDefault(49, false)) {
            napasResponse.setCurrencyCodeTransaction(CurrencyCode.fromCode(extractFixedLengthValue(napasResponseString, currentIndex, 3)));
            currentIndex += 3;
        }
        //DE#60: Self-defined field
        if (primaryBitmap.getOrDefault(60, false)) {
            napasResponse.setSelfDefinedField(SelfDefinedFieldValue.fromCode(extractFixedLengthValue(napasResponseString, currentIndex, 2)));
            currentIndex += 2;
        }
        //DE#63: Transaction reference number
        if (primaryBitmap.getOrDefault(63, false)) {
            napasResponse.setTransactionReferenceNumber(extractFixedLengthValue(napasResponseString, currentIndex, 16));
            currentIndex += 16;
        }
        //DE#67: Payment code
        if (secondBitmap.getOrDefault(67, false)) {
            napasResponse.setPaymentCode(PaymentCodeValue.fromCode(extractFixedLengthValue(napasResponseString, currentIndex, 2))); // Assuming 2-digit code
            currentIndex += 2;
        }
        //DE#102: From account identification
        if ( secondBitmap.getOrDefault(102, false)) { // Check both bitmaps
            napasResponse.setFromAccountIdentification(extractLLVARValue(napasResponseString, currentIndex, 2));
            currentIndex += 2 + Integer.parseInt(napasResponseString.substring(currentIndex, currentIndex + 2));
        }
        //DE#103: From account type
        if (secondBitmap.getOrDefault(103, false)) { // Check both bitmaps
            napasResponse.setToAccountIdentification(extractLLVARValue(napasResponseString, currentIndex, 2));
            currentIndex += 2 + Integer.parseInt(napasResponseString.substring(currentIndex, currentIndex + 2));
        }
        //DE#104: Content transfer
        try {
            if (secondBitmap.getOrDefault(104, false)) { // Check both bitmaps
                napasResponse.setContentTransfer(extractLLLVARValue(napasResponseString, currentIndex, 3));
                currentIndex += 3 + Integer.parseInt(napasResponseString.substring(currentIndex, currentIndex + 3));
            }
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e);
        }
        //DE#120: Beneficial card holder info
        if (secondBitmap.getOrDefault(120, false)) { // Check both bitmaps
            
            napasResponse.setBeneficialCardHolderInfo(extractLLLVARValue(napasResponseString, currentIndex+3, 3));
            currentIndex += 3 + Integer.parseInt(napasResponseString.substring(currentIndex, currentIndex + 3));
        }
        //DE#128: Message authentication code (HMAC)
        if (secondBitmap.getOrDefault(128, false)) { // DE#128 is in secondary bitmap
            napasResponse.setMessageAuthenticationCode(extractFixedLengthValue(napasResponseString, currentIndex, 64));
            currentIndex += 64;
        }

        return napasResponse;
    }


    private String extractFixedLengthValue(String napasResponseString, int startIndex, int length) {
        return napasResponseString.substring(startIndex, startIndex + length);
    }

    private String extractLLVARValue(String napasResponseString, int startIndex, int lengthBytes) {
        int length = Integer.parseInt(napasResponseString.substring(startIndex, startIndex + lengthBytes));
        return napasResponseString.substring(startIndex + lengthBytes, startIndex + lengthBytes + length);
    }

    private String extractLLLVARValue(String napasResponseString, int startIndex, int lengthBytes) {
        int length = Integer.parseInt(napasResponseString.substring(startIndex, startIndex + lengthBytes));
        return napasResponseString.substring(startIndex + lengthBytes, startIndex + lengthBytes + length);
    }

    private String serializeNapasRequestToString(NapasRequest napasRequest) {
        StringBuilder sb = new StringBuilder();
        // DE#0: Message Type (4 bytes)
        sb.append(napasRequest.getMessageType().getCode());
        // Primary Bit Map (16 bytes)
        sb.append(napasRequest.getPrimaryBitmap());
        // DE#1: Secondary Bit Map (16 bytes, conditional, based on bit 1 of Primary Bitmap)
        if (napasRequest.getSecondaryBitmap() != null) {
            sb.append(napasRequest.getSecondaryBitmap());
        }
        // DE#2: Primary Account Number (PAN) (LLVAR, 2 bytes length + data)
        if (napasRequest.getPrimaryAccountNumber() != null) {
            sb.append(String.format("%02d%s", napasRequest.getPrimaryAccountNumber().length(), napasRequest.getPrimaryAccountNumber()));
        }
        // DE#3: Processing Code (6 bytes)
        if (napasRequest.getProcessingCode() != null) {
            sb.append(napasRequest.getProcessingCode());
        }
        // DE#4: Transaction Amount (12 bytes)
        if (napasRequest.getTransactionAmount() != null) {
            sb.append(napasRequest.getTransactionAmount());
        }
        // DE#7: Transmission Date and Time (10 bytes, MMDDHHMMSS)
        if (napasRequest.getTransmissionDateTime() != null) {
            sb.append(napasRequest.getTransmissionDateTime());
        }
        // DE#11: System Trace Audit Number (6 bytes)
        if (napasRequest.getSystemTraceAuditNumber() != null) {
            sb.append(napasRequest.getSystemTraceAuditNumber());
        }
        // DE#12: Local Transaction Time (6 bytes, HHMMSS)
        if (napasRequest.getLocalTransactionTime() != null) {
            sb.append(napasRequest.getLocalTransactionTime());
        }
        // DE#13: Local Transaction Date (4 bytes, MMDD)
        if (napasRequest.getLocalTransactionDate() != null) {
            sb.append(napasRequest.getLocalTransactionDate());
        }
        // DE#22: Point-Of-Service Entry Mode (3 bytes)
        if (napasRequest.getPointOfServiceEntryMode() != null) {
            sb.append(napasRequest.getPointOfServiceEntryMode().getCode());
        }
        // DE#25: Point-of-Service Condition Code (2 bytes)
        if (napasRequest.getPointOfServiceConditionCode() != null) {
            sb.append(napasRequest.getPointOfServiceConditionCode().getCode());
        }
        // DE#32: Acquiring Institution Code (LLVAR, 2 bytes length + data)
        if (napasRequest.getAcquiringInstitutionCode() != null) {
            sb.append(String.format("%02d%s", napasRequest.getAcquiringInstitutionCode().length(), napasRequest.getAcquiringInstitutionCode()));
        }
        // DE#37: Retrieval reference number (12 bytes)
        if (napasRequest.getRetrievalReferenceNumber() != null) {
            sb.append(napasRequest.getRetrievalReferenceNumber());
        }
        // DE#41: Card Acceptor Terminal Identification (8 bytes)
        if (napasRequest.getCardAcceptorTerminalIdentification() != null) {
            sb.append(napasRequest.getCardAcceptorTerminalIdentification());
        }
        // DE#42: Card Acceptor Identification Code (LLVAR, 2 bytes length + data)
        if (napasRequest.getCardAcceptorIdentificationCode() != null) {
            sb.append(String.format("%02d%s", napasRequest.getCardAcceptorIdentificationCode().length(), napasRequest.getCardAcceptorIdentificationCode()));
        }
        // DE#43: Card Acceptor Name/Location (LLVAR, 2 bytes length + data)
        if (napasRequest.getCardAcceptorNameLocation() != null) {
            sb.append(String.format("%02d%s", napasRequest.getCardAcceptorNameLocation().length(), napasRequest.getCardAcceptorNameLocation()));
        }
        // DE#48: Additional Private Data (LLLVAR, 3 bytes length + data)
        if (napasRequest.getAdditionalPrivateData() != null) {
            sb.append(String.format("%03d%s", napasRequest.getAdditionalPrivateData().length(), napasRequest.getAdditionalPrivateData()));
        }
        // DE#49: Currency Code, Transaction (3 bytes)
        if (napasRequest.getCurrencyCodeTransaction() != null) {
            sb.append(napasRequest.getCurrencyCodeTransaction().getCode());
        }
        // DE#60: Self-Defined Field (2 bytes)
        if (napasRequest.getSelfDefinedField() != null) {
            sb.append(napasRequest.getSelfDefinedField().getCode());
        }
        // DE#62: Service Code (6 bytes - assuming fixed length "IF_INQ")
        if (napasRequest.getServiceCode() != null) {
            sb.append(napasRequest.getServiceCode().getCode());
        }
        // DE#102: From Account Identification (LLVAR, 2 bytes length + data)
        if (napasRequest.getFromAccountIdentification() != null) {
            sb.append(String.format("%02d%s", napasRequest.getFromAccountIdentification().length(), napasRequest.getFromAccountIdentification()));
        }
        // DE#103: To Account Identification (LLVAR, 2 bytes length + data)
        if (napasRequest.getToAccountIdentification() != null) {
            sb.append(String.format("%02d%s", napasRequest.getToAccountIdentification().length(), napasRequest.getToAccountIdentification()));
        }
        // DE#104: Content Transfer (LLLVAR, 3 bytes length + data)
        if (napasRequest.getContentTransfer() != null) {
            sb.append(String.format("%03d%s", napasRequest.getContentTransfer().length(), napasRequest.getContentTransfer()));
        }
       
        // DE#128: Message Authentication Code (MAC) (64 bytes)
        if (napasRequest.getMessageAuthenticationCode() != null) {
            sb.append(napasRequest.getMessageAuthenticationCode());
        }
        return sb.toString();
    }

    // private GatewayRequestEntity transformGatewayRequestToEntity(GatewayRequest gatewayRequest) {
    //     GatewayRequestEntity entity = new GatewayRequestEntity();
    //     entity.setPan(gatewayRequest.getPan());
    //     entity.setAmount(gatewayRequest.getAmount());
    //     entity.setTransmissionTime(gatewayRequest.getTransmissionTime());
    //     entity.setTransmissionDate(gatewayRequest.getTransmissionDate());
    //     entity.setTerminalId(gatewayRequest.getTerminalId());
    //     entity.setAcceptorNameLocation(gatewayRequest.getBankName()+" "+gatewayRequest.getLocation()+" "+"704");
    //     entity.setPrivateData(gatewayRequest.getPrivateData());
    //     entity.setQrCodeTransfer(gatewayRequest.getQrCodeTransfer());
    //     entity.setFromAccount(gatewayRequest.getFromAccount());
    //     entity.setContent(gatewayRequest.getContent());
    //     entity.setBeneficialInfo(gatewayRequest.getBeneficialInfo());
    //     return entity;
    // }

    private NapasRequest transformGatewayRequestToNapasRequest(GatewayRequest gatewayRequest) {
        NapasRequest napasRequest = new NapasRequest();

        Map<Integer, String> dataElements = new HashMap<>();

        // DE#2: Primary Account Number (PAN)
        if (gatewayRequest.getPan() != null) {
            dataElements.put(2, String.format("%02d%s", gatewayRequest.getPan().length(), gatewayRequest.getPan()));
            napasRequest.setPrimaryAccountNumber(gatewayRequest.getPan());
        }

        // DE#3: Processing Code
        String processingCode = ProcessingCodeCategory.TRANSFER_PAYMENT.getCode() +
                FromAccountType.DEFAULT.getCode() +
                ToAccountType.DEFAULT.getCode();
        dataElements.put(3, processingCode);
        napasRequest.setProcessingCode(processingCode);

        // DE#4: Transaction Amount
        if (gatewayRequest.getAmount() != null) {
            String amountStr = String.format("%012d", gatewayRequest.getAmount().intValue() * 100); // Chuyển sang đơn vị nhỏ nhất (ví dụ: cent)
            dataElements.put(4, amountStr);
            napasRequest.setTransactionAmount(amountStr);
        }

        // DE#7: Transmission Date and Time
        LocalDateTime now = LocalDateTime.now();
        String transmissionDateTime = now.format(DateTimeFormatter.ofPattern("MMddHHmmss"));
        dataElements.put(7, transmissionDateTime);
        napasRequest.setTransmissionDateTime(transmissionDateTime);

        // DE#11: System Trace Audit Number 
        if(gatewayRequest.getSystemTraceAuditNumber()!=0) {
            String traceNumber = String.format("%06d", gatewayRequest.getSystemTraceAuditNumber()); 
            dataElements.put(11, traceNumber);
            napasRequest.setSystemTraceAuditNumber(traceNumber); 
        }

        // DE#12: Local Transaction Time
        String localTime = gatewayRequest.getLocalTransactionTime();
        dataElements.put(12, localTime);
        napasRequest.setLocalTransactionTime(localTime);

        // DE#13: Local Transaction Date
        String localDate = now.format(DateTimeFormatter.ofPattern("MMdd"));
        dataElements.put(13, localDate);
        napasRequest.setLocalTransactionDate(localDate);

        // DE#22: Point-Of-Service Entry Mode
        dataElements.put(22, PointOfServiceEntryModeValue.QR_CODE.getCode());
        napasRequest.setPointOfServiceEntryMode(PointOfServiceEntryModeValue.QR_CODE);

        // DE#25: Point-of-Service Condition Code
        dataElements.put(25, PointOfServiceConditionCodeValue.CUSTOMER_AUTHENTICATED.getCode());
        napasRequest.setPointOfServiceConditionCode(PointOfServiceConditionCodeValue.CUSTOMER_AUTHENTICATED);

        // DE#32: Acquiring Institution Code
        String acquiringCode = "970422"; // Mã định danh tại MB
        dataElements.put(32, String.format("%02d%s", acquiringCode.length(), acquiringCode));
        napasRequest.setAcquiringInstitutionCode(acquiringCode);

        // Tạo DE#37: Retrieval Reference Number theo format "YDDDXXNNNNNN"
        try {
            // Lấy giá trị MMDD từ DE#7 (4 ký tự đầu tiên)
            String mmdd = transmissionDateTime.substring(0, 4);

            // Lấy năm hiện tại (YYYY)
            int currentYear = now.getYear();

            // Chuyển MMDD thành ngày thứ bao nhiêu trong năm (DDD)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMdd");
            MonthDay monthDay = MonthDay.parse(mmdd, formatter);
            LocalDate transactionDate = monthDay.atYear(currentYear);
            int dayOfYear = transactionDate.getDayOfYear(); // Lấy giá trị DDD

            // Lấy chữ số cuối của năm
            String yearDigit = String.valueOf(currentYear % 10);
            String traceNumber = String.format("%06d", gatewayRequest.getSystemTraceAuditNumber());
            // Phiên bản TCKT của dịch vụ (hiện tại là 2.0 => "20")
            String versionCode = "20";

            // Ghép thành DE#37
            String retrievalReferenceNumber = String.format("%s%03d%s%s", yearDigit, dayOfYear, versionCode, traceNumber);
            dataElements.put(37, retrievalReferenceNumber);
            napasRequest.setRetrievalReferenceNumber(retrievalReferenceNumber);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo DE#37: " + e.getMessage(), e);
        }

        // DE#41: Card Acceptor Terminal Identification
        String terminalId = gatewayRequest.getTerminalId() != null ?"0001"+ gatewayRequest.getTerminalId() : "0001XXXX"; // Ví dụ mặc định
        dataElements.put(41, terminalId);
        napasRequest.setCardAcceptorTerminalIdentification(terminalId);

        // DE#42: Card Acceptor Identification Code (Chiều đi) - Giá trị giả định
        String acceptorIdCode = "PAYMENT_GATEWAY"; // Giá trị giả định
        dataElements.put(42,  acceptorIdCode);
        napasRequest.setCardAcceptorIdentificationCode(acceptorIdCode);

        // DE#43: Card Acceptor Name/Location
        String acceptorNameLocation = String.format("%-22s", gatewayRequest.getBankName())+" "+String.format("%-13s", gatewayRequest.getLocation())+" "+CurrencyCode.VND.getCode();
        dataElements.put(43, acceptorNameLocation);
        napasRequest.setCardAcceptorNameLocation(acceptorNameLocation);

        // DE#48: Additional Private Data
        if (gatewayRequest.getSenderName() != null|| gatewayRequest.getAddressSender()!= null) {
            String additionalPrivateData=gatewayRequest.getSenderName()+"<CR>"+gatewayRequest.getAddressSender()+"<CR>"; 
            dataElements.put(48, String.format("%03d%s",additionalPrivateData.length(), additionalPrivateData));
            napasRequest.setAdditionalPrivateData(additionalPrivateData);
        }

        // DE#49: Currency Code, Transaction (Mặc định VND)
        dataElements.put(49, CurrencyCode.VND.getCode());
        napasRequest.setCurrencyCodeTransaction(CurrencyCode.VND);

        // DE#60: Self-Defined Field
        if (gatewayRequest.getQrCodeTransfer() != null && gatewayRequest.getQrCodeTransfer().equals("99")) {
            dataElements.put(60, SelfDefinedFieldValue.QR_TRANSFER.getCode());
            napasRequest.setSelfDefinedField(SelfDefinedFieldValue.QR_TRANSFER);
        }

        // DE#62: Service Code (Mặc định IF_INQ)
        dataElements.put(62, ServiceCode.IF_INQ.getCode());
        napasRequest.setServiceCode(ServiceCode.IF_INQ);

        // DE#102: From Account Identification
        if (gatewayRequest.getFromAccount() != null) {
            dataElements.put(102, String.format("%02d%s", gatewayRequest.getFromAccount().length(), gatewayRequest.getFromAccount()));
            napasRequest.setFromAccountIdentification(gatewayRequest.getFromAccount());
        }

        // DE#103: To Account Identification
        if (gatewayRequest.getToAccountIdentification() != null) {
            dataElements.put(103, String.format("%02d%s", gatewayRequest.getToAccountIdentification().length(), gatewayRequest.getToAccountIdentification()));
            napasRequest.setToAccountIdentification(gatewayRequest.getToAccountIdentification());
        }

        // DE#104: Content Transfer
        if (gatewayRequest.getContent() != null) {
            dataElements.put(104, String.format("%03d%s", gatewayRequest.getContent().length(), gatewayRequest.getContent()));
            napasRequest.setContentTransfer(gatewayRequest.getContent());
        }

        //DE#128
        // 3. Tạo HMAC cho NapasRequest và set vào NapasRequest
        String hmacValue = generateRequestHmac(napasRequest, SECRET_KEY);
        dataElements.put(128, hmacValue);
        napasRequest.setMessageAuthenticationCode(hmacValue);

        // Tạo Bitmap từ các DE đã sử dụng
        String bitmap = BitmapUtil.generateBitmap(dataElements);
        napasRequest.setPrimaryBitmap(bitmap.substring(0, 16)); // Primary Bitmap
        if (bitmap.length() > 16) {
            napasRequest.setSecondaryBitmap(bitmap.substring(16)); // Secondary Bitmap nếu có
            dataElements.put(1, napasRequest.getSecondaryBitmap()); // DE#1 is Secondary Bitmap itself
        }

        return napasRequest;
    }

    // private NapasRequestEntity transformNapasRequestToEntity(NapasRequest napasRequest) {
    //     NapasRequestEntity entity = new NapasRequestEntity();
    //     entity.setMessageType(napasRequest.getMessageType());
    //     entity.setPrimaryBitmap(napasRequest.getPrimaryBitmap());
    //     entity.setSecondaryBitmap(napasRequest.getSecondaryBitmap());
    //     entity.setPrimaryAccountNumber(napasRequest.getPrimaryAccountNumber());
    //     entity.setProcessingCode(napasRequest.getProcessingCode());
    //     entity.setTransactionAmount(napasRequest.getTransactionAmount());
    //     entity.setTransmissionDateTime(napasRequest.getTransmissionDateTime());
    //     entity.setSystemTraceAuditNumber(napasRequest.getSystemTraceAuditNumber());
    //     entity.setLocalTransactionTime(napasRequest.getLocalTransactionTime());
    //     entity.setLocalTransactionDate(napasRequest.getLocalTransactionDate());
    //     entity.setPointOfServiceEntryMode(napasRequest.getPointOfServiceEntryMode());
    //     entity.setPointOfServiceConditionCode(napasRequest.getPointOfServiceConditionCode());
    //     entity.setAcquiringInstitutionCode(napasRequest.getAcquiringInstitutionCode());
    //     entity.setCardAcceptorTerminalIdentification(napasRequest.getCardAcceptorTerminalIdentification());
    //     entity.setCardAcceptorIdentificationCode(napasRequest.getCardAcceptorIdentificationCode());
    //     entity.setCardAcceptorNameLocation(napasRequest.getCardAcceptorNameLocation());
    //     entity.setAdditionalPrivateData(napasRequest.getAdditionalPrivateData());
    //     entity.setCurrencyCodeTransaction(napasRequest.getCurrencyCodeTransaction());
    //     entity.setSelfDefinedField(napasRequest.getSelfDefinedField());
    //     entity.setServiceCode(napasRequest.getServiceCode());
    //     entity.setFromAccountIdentification(napasRequest.getFromAccountIdentification());
    //     entity.setContentTransfer(napasRequest.getContentTransfer());
    //     entity.setBeneficialCardHolderInfo(napasRequest.getBeneficialCardHolderInfo());
    //     return entity;
    // }

    // private NapasResponseEntity transformNapasResponseToEntity(NapasResponse napasResponse) {
    //     NapasResponseEntity entity = new NapasResponseEntity();
    //     entity.setMessageType(napasResponse.getMessageType());
    //     entity.setPrimaryBitmap(napasResponse.getPrimaryBitmap());
    //     entity.setSecondaryBitmap(napasResponse.getSecondaryBitmap());
    //     entity.setPrimaryAccountNumber(napasResponse.getPrimaryAccountNumber());
    //     entity.setProcessingCode(napasResponse.getProcessingCode());
    //     entity.setTransactionAmount(napasResponse.getTransactionAmount());
    //     entity.setTransmissionDateTime(napasResponse.getTransmissionDateTime());
    //     entity.setSystemTraceAuditNumber(napasResponse.getSystemTraceAuditNumber());
    //     entity.setLocalTransactionTime(napasResponse.getLocalTransactionTime());
    //     entity.setLocalTransactionDate(napasResponse.getLocalTransactionDate());
    //     entity.setSettlementDate(napasResponse.getSettlementDate());
    //     entity.setAcquiringInstitutionCode(napasResponse.getAcquiringInstitutionCode());
    //     entity.setRetrievalReferenceNumber(napasResponse.getRetrievalReferenceNumber());
    //     entity.setResponseCode(napasResponse.getResponseCode());
    //     entity.setCardAcceptorTerminalIdentification(napasResponse.getCardAcceptorTerminalIdentification());
    //     entity.setCardAcceptorNameLocation(napasResponse.getCardAcceptorNameLocation());
    //     entity.setAdditionalPrivateData(napasResponse.getAdditionalPrivateData());
    //     entity.setCurrencyCodeTransaction(napasResponse.getCurrencyCodeTransaction());
    //     entity.setSelfDefinedField(napasResponse.getSelfDefinedField());
    //     entity.setTransactionReferenceNumber(napasResponse.getTransactionReferenceNumber());
    //     entity.setPaymentCode(napasResponse.getPaymentCode());
    //     entity.setFromAccountIdentification(napasResponse.getFromAccountIdentification());
    //     entity.setContentTransfer(napasResponse.getContentTransfer());
    //     entity.setBeneficialCardHolderInfo(napasResponse.getBeneficialCardHolderInfo());
    //     entity.setMessageAuthenticationCode(napasResponse.getMessageAuthenticationCode());
    //     return entity;
    // }

    // private GatewayResponseEntity transformNapasResponseToGatewayResponseEntity(NapasResponse napasResponse) {
    //     GatewayResponseEntity entity = new GatewayResponseEntity();
    //     entity.setMessageType(napasResponse.getMessageType());
    //     entity.setPrimaryBitmap(napasResponse.getPrimaryBitmap());
    //     entity.setSecondaryBitmap(napasResponse.getSecondaryBitmap());
    //     entity.setPrimaryAccountNumber(napasResponse.getPrimaryAccountNumber());
    //     entity.setProcessingCode(napasResponse.getProcessingCode());
    //     entity.setTransactionAmount(napasResponse.getTransactionAmount());
    //     entity.setTransmissionDateTime(napasResponse.getTransmissionDateTime());
    //     entity.setSystemTraceAuditNumber(napasResponse.getSystemTraceAuditNumber());
    //     entity.setLocalTransactionTime(napasResponse.getLocalTransactionTime());
    //     entity.setLocalTransactionDate(napasResponse.getLocalTransactionDate());
    //     entity.setSettlementDate(napasResponse.getSettlementDate());
    //     entity.setAcquiringInstitutionCode(napasResponse.getAcquiringInstitutionCode());
    //     entity.setRetrievalReferenceNumber(napasResponse.getRetrievalReferenceNumber());
    //     entity.setResponseCode(napasResponse.getResponseCode());
    //     entity.setCardAcceptorTerminalIdentification(napasResponse.getCardAcceptorTerminalIdentification());
    //     entity.setCardAcceptorNameLocation(napasResponse.getCardAcceptorNameLocation());
    //     entity.setAdditionalPrivateData(napasResponse.getAdditionalPrivateData());
    //     entity.setCurrencyCodeTransaction(napasResponse.getCurrencyCodeTransaction());
    //     entity.setSelfDefinedField(napasResponse.getSelfDefinedField());
    //     entity.setTransactionReferenceNumber(napasResponse.getTransactionReferenceNumber());
    //     entity.setPaymentCode(napasResponse.getPaymentCode());
    //     entity.setFromAccountIdentification(napasResponse.getFromAccountIdentification());
    //     entity.setContentTransfer(napasResponse.getContentTransfer());
    //     entity.setBeneficialCardHolderInfo(napasResponse.getBeneficialCardHolderInfo());
    //     entity.setMessageAuthenticationCode(napasResponse.getMessageAuthenticationCode());
    //     return entity;
    // }

    private String generateRequestHmac(NapasRequest napasRequest, String secretKey) {
        try {
            String dataForHmac = buildDataStringForHmac(napasRequest); // Hàm tạo chuỗi dữ liệu để tạo HMAC
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            byte[] hmacBytes = hmacSha256.doFinal(dataForHmac.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexHmac = new StringBuilder(hmacBytes.length * 2);
            for (byte b : hmacBytes) {
                String hex = String.format("%02X", b);
                hexHmac.append(hex);
            }
            return hexHmac.toString().toUpperCase(); // Trả về HMAC dạng uppercase
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("Error generating HMAC: " + e.getMessage());
            return null; // Hoặc throw exception tùy xử lý lỗi
        }
    }
    private String buildDataStringForHmac(NapasRequest napasRequest) {
        StringBuilder dataBuilder = new StringBuilder();
        Map<Integer, String> deValues = extractDataElementsForHmac(napasRequest); // Lấy DE values theo thứ tự

        for (Map.Entry<Integer, String> entry : deValues.entrySet()) {
            dataBuilder.append(entry.getValue()); // Nối giá trị các DE theo thứ tự
        }
        return dataBuilder.toString();
    }
    private Map<Integer, String> extractDataElementsForHmac(NapasRequest napasRequest) {
        Map<Integer, String> dataElementsHmac = new HashMap<>();

        if (napasRequest.getPrimaryAccountNumber() != null) {
            dataElementsHmac.put(2, String.format("%02d%s", napasRequest.getPrimaryAccountNumber().length(), napasRequest.getPrimaryAccountNumber()));
        }
        if (napasRequest.getProcessingCode() != null) {
            dataElementsHmac.put(3, napasRequest.getProcessingCode());
        }
        if (napasRequest.getTransactionAmount() != null) {
            dataElementsHmac.put(4, napasRequest.getTransactionAmount());
        }
        if (napasRequest.getTransmissionDateTime() != null) {
            dataElementsHmac.put(7, napasRequest.getTransmissionDateTime());
        }
        if (napasRequest.getSystemTraceAuditNumber() != null) {
            dataElementsHmac.put(11, napasRequest.getSystemTraceAuditNumber());
        }
        if (napasRequest.getLocalTransactionTime() != null) {
            dataElementsHmac.put(12, napasRequest.getLocalTransactionTime());
        }
        if (napasRequest.getLocalTransactionDate() != null) {
            dataElementsHmac.put(13, napasRequest.getLocalTransactionDate());
        }
        if (napasRequest.getPointOfServiceEntryMode() != null) {
            dataElementsHmac.put(22, napasRequest.getPointOfServiceEntryMode().getCode());
        }
        if (napasRequest.getPointOfServiceConditionCode() != null) {
            dataElementsHmac.put(25, napasRequest.getPointOfServiceConditionCode().getCode());
        }
        if (napasRequest.getAcquiringInstitutionCode() != null) {
            dataElementsHmac.put(32, String.format("%02d%s", napasRequest.getAcquiringInstitutionCode().length(), napasRequest.getAcquiringInstitutionCode()));
        }
        if (napasRequest.getRetrievalReferenceNumber() != null) {
            dataElementsHmac.put(37, napasRequest.getRetrievalReferenceNumber());
        }
        if (napasRequest.getCardAcceptorTerminalIdentification() != null) {
            dataElementsHmac.put(41, napasRequest.getCardAcceptorTerminalIdentification());
        }
        if (napasRequest.getCardAcceptorIdentificationCode() != null) {
            dataElementsHmac.put(42, String.format("%02d%s", napasRequest.getCardAcceptorIdentificationCode().length(), napasRequest.getCardAcceptorIdentificationCode()));
        }
        if (napasRequest.getCardAcceptorNameLocation() != null) {
            dataElementsHmac.put(43, String.format("%02d%s", napasRequest.getCardAcceptorNameLocation().length(), napasRequest.getCardAcceptorNameLocation()));
        }
        if (napasRequest.getAdditionalPrivateData() != null) {
            dataElementsHmac.put(48, String.format("%03d%s", napasRequest.getAdditionalPrivateData().length(), napasRequest.getAdditionalPrivateData()));
        }
        if (napasRequest.getCurrencyCodeTransaction() != null) {
            dataElementsHmac.put(49, napasRequest.getCurrencyCodeTransaction().getCode());
        }
        if (napasRequest.getSelfDefinedField() != null) {
            dataElementsHmac.put(60, napasRequest.getSelfDefinedField().getCode());
        }
        if (napasRequest.getServiceCode() != null) {
            dataElementsHmac.put(62, napasRequest.getServiceCode().getCode());
        }
        if (napasRequest.getFromAccountIdentification() != null) {
            dataElementsHmac.put(102, String.format("%02d%s", napasRequest.getFromAccountIdentification().length(), napasRequest.getFromAccountIdentification()));
        }
        if (napasRequest.getToAccountIdentification() != null) {
            dataElementsHmac.put(103, String.format("%02d%s", napasRequest.getToAccountIdentification().length(), napasRequest.getToAccountIdentification()));
        }
        if (napasRequest.getContentTransfer() != null) {
            dataElementsHmac.put(104, String.format("%03d%s", napasRequest.getContentTransfer().length(), napasRequest.getContentTransfer()));
        }
        if (napasRequest.getBeneficialCardHolderInfo() != null) {
            dataElementsHmac.put(120, String.format("%03d%s", napasRequest.getBeneficialCardHolderInfo().length(), napasRequest.getBeneficialCardHolderInfo()));
        }
        return dataElementsHmac;
    }

}