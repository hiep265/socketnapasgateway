package com.example.gateway.until;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BitmapUtil {

    /**
     * Sinh bitmap từ các data element theo dạng nhị phân rồi chuyển sang hexa.
     * Nếu có DE > 64 thì dùng secondary bitmap (tổng độ dài 128 bit), ngược lại chỉ dùng 64 bit.
     *
     * @param dataElements Các data element cần đánh dấu (key là số DE)
     * @return Chuỗi bitmap dạng hexa
     */
    public static String generateBitmap(Map<Integer, String> dataElements) {
        // Kiểm tra xem có DE nào > 64 để quyết định có secondary bitmap hay không
        boolean secondaryBitmapNeeded = dataElements.keySet().stream().anyMatch(de -> de > 64);
        int bitmapLength = secondaryBitmapNeeded ? 128 : 64;

        // Khởi tạo mảng int để lưu các bit (0 hoặc 1) của bitmap
        int[] bitmapArray = new int[bitmapLength];

        // Nếu cần secondary bitmap, đánh dấu bit đầu tiên (DE1) là 1
        if (secondaryBitmapNeeded) {
            bitmapArray[0] = 1;
        }

        // Đánh dấu các bit tương ứng với các DE có trong dataElements (ngoại trừ DE1 đã dùng cho secondary bitmap)
        for (int de : dataElements.keySet()) {
            if (de > 1 && de <= bitmapLength) {
                bitmapArray[de - 1] = 1; // chuyển số DE sang chỉ số mảng (bắt đầu từ 0)
            }
        }

        // In ra thông báo cho các DE được đánh dấu (dùng cho debug)
        for (int i = 0; i < bitmapLength; i++) {
            if (bitmapArray[i] == 1) {
                System.out.println("DE#" + (i + 1) + " is marked");
            }
        }

        // Chuyển từ mảng nhị phân sang chuỗi hexa: nhóm 4 bit thành 1 ký tự hexa
        StringBuilder hexBitmap = new StringBuilder();
        for (int i = 0; i < bitmapLength; i += 4) {
            int decimalValue = (bitmapArray[i] << 3) 
                             | (bitmapArray[i + 1] << 2)
                             | (bitmapArray[i + 2] << 1)
                             | (bitmapArray[i + 3]);
            hexBitmap.append(Integer.toHexString(decimalValue).toUpperCase());
        }

        return hexBitmap.toString();
    }

    /**
     * Phân tích chuỗi bitmap hexa thành map chứa thông tin các DE (true nếu bit được đánh dấu).
     * Chỉ xử lý các DE từ 1 đến 128.
     *
     * @param hexBitmap Chuỗi bitmap dạng hexa
     * @return Map chứa số DE và trạng thái (true/false)
     */
    public static Map<Integer, Boolean> parseBitmap(String hexBitmap, int offset) {
    
        if (hexBitmap.length() != 16) { // 16 hex = 64 bits
            throw new IllegalArgumentException("Invalid bitmap length. Must be exactly 16 hex characters (64 bits).");
        }
    
        int[] bitmapArray = new int[64]; // Mảng nhị phân cho 64 bit
    
        // Chuyển đổi từng ký tự hex thành nhị phân
        for (int i = 0; i < 16; i++) {
            char hexChar = hexBitmap.charAt(i);
            int decimalValue = Integer.parseInt(String.valueOf(hexChar), 16);
            int baseIndex = i * 4;
    
            bitmapArray[baseIndex]     = (decimalValue & 8) >> 3;
            bitmapArray[baseIndex + 1] = (decimalValue & 4) >> 2;
            bitmapArray[baseIndex + 2] = (decimalValue & 2) >> 1;
            bitmapArray[baseIndex + 3] = decimalValue & 1;
    
          
        }
    
        // Xác định dải DE dựa trên offset
        int startDE = (offset == 0) ? 1 : 65;
        int endDE = (offset == 0) ? 64 : 128;
    
        Map<Integer, Boolean> deMap = new HashMap<>();
        for (int i = 0; i < 64; i++) {
            int deNumber = startDE + i; // Nếu là Secondary Bitmap, DE sẽ bắt đầu từ 65
            if (deNumber > endDE) break; // Đảm bảo không vượt quá 128
    
            boolean isMarked = bitmapArray[i] == 1;
            deMap.put(deNumber, isMarked);
            if (isMarked) {
                log.info("DE#" + deNumber + " is marked");
            }
        }
    
        log.info("Finished parseBitmap for " + (offset == 0 ? "Primary" : "Secondary") + " Bitmap");
        return deMap;
    }
}
