package com.example.stocknewsbot.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class TextUtil {

    private TextUtil() {}

    /**
     *
     * @param input 문자열 입력값
     * @return 변환된 SHA-256 해시
     */
    public static String sha256(String input) {
        try {
            MessageDigest disget = MessageDigest.getInstance("SHA-256");
            byte[] hash = disget.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    /**
     *
     * @param text 텔레그램의 스페셜키워드 목록
     * @return 이스케이프된 텔레그램의 스페셜키워드가 포함된 text
     */
    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * 네이버 뉴스 제목에 포함된 HTML 태그를 제거 용도
     */
    public static String stripHtml(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "");
    }
}
