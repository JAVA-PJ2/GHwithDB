package com.gh.exception;

@SuppressWarnings("serial")
public class BookNotFoundException extends Exception {
	public BookNotFoundException() {
		this("예약 정보를 찾을 수 없습니다...");
	}
	
	public BookNotFoundException(String message) {
		super(message);
	}
}

