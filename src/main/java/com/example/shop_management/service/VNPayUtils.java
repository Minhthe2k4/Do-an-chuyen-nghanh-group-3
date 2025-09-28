package com.example.shop_management.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VNPayUtils {

    /**
     * Sinh chữ ký HMAC SHA512
     */
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) return null;
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8.toString());
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8.toString());
            byte[] result = hmac512.doFinal(dataBytes);

            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }


public static boolean validateSignature(Map<String, String> vnpParams, String secretKey) throws UnsupportedEncodingException {
    if (vnpParams == null || secretKey == null) return false;


    Map<String, String> sortedParams = new HashMap<>(vnpParams);
    sortedParams.remove("vnp_SecureHash");


    // Sort theo alphabet
    List<String> fieldNames = new ArrayList<>(sortedParams.keySet());
    Collections.sort(fieldNames);

    // Build hashData
    StringBuilder hashData = new StringBuilder();
    for (String fieldName : fieldNames) {
        String fieldValue = sortedParams.get(fieldName);
        if (fieldValue != null && !fieldValue.isEmpty()) {
            hashData.append(fieldName)
                    .append('=')
                    .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()))
                    .append('&');
        }
    }
    if (!hashData.isEmpty()) {
        hashData.deleteCharAt(hashData.length() - 1); // bỏ & cuối
    }

    // Tính lại SecureHash từ secretKey
    String expectedHash = hmacSHA512(secretKey, hashData.toString());
    String receivedHash = vnpParams.get("vnp_SecureHash");


    return expectedHash != null && expectedHash.equalsIgnoreCase(receivedHash);
}

}
