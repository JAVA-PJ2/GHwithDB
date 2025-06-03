package com.gh.exception;

@SuppressWarnings("serial")
public class BookCancelledException extends Exception {
	public BookCancelledException() {
		this("이미 취소된 예약입니다...");
	}
	
	public BookCancelledException(String message) {
		super(message);
	}
}

