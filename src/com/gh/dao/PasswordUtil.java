package com.gh.dao;

import java.security.MessageDigest;

public class PasswordUtil {
	public static String sha256(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] result = md.digest(input.getBytes("UTF-8"));
			
			StringBuilder sb = new StringBuilder();
			for(byte b : result) 
				sb.append(String.format("%02x", b)); // 2자리 hex 문자열
			
			return sb.toString(); // 총 64자리 문자열
		} catch (Exception e) {
			throw new RuntimeException("SHA-256 해싱 실패", e);
		}
	}
}
