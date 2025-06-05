package com.gh.exception;

@SuppressWarnings("serial")
public class RecordNotFoundException extends Exception {
	public RecordNotFoundException() {
		this("예약 정보를 찾을 수 없습니다...");
	}
	
	public RecordNotFoundException(String message) {
		super(message);
	}
}

