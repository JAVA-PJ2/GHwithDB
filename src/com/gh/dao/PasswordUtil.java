package com.gh.dao;

import java.security.MessageDigest;

public class PasswordUtil {
	
    public static String encrypt(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b)); // 16진수
            }
            return sb.toString(); // 64자 길이
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 암호화 실패", e);
        }
    }
}
