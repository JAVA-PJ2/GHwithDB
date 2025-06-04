package com.gh.vo;

import java.time.LocalDate;

public class Booking {
	private String bookingId; // 칼럼에는 booking_id
	private String clientId;
	private int peopleCnt; // 칼럼에는 people
	private LocalDate checkInDate; // 칼럼에는 check_in
	private int nights;
	private int totalPrice; // 칼럼에는 total_price
	private String ghName;
	
	public Booking(){}
	
	public Booking(String bookingId,String clientId, String ghName, int peopleCnt, LocalDate checkInDate, int nights, int totalPrice) {
		super();
		this.bookingId = bookingId;
		this.clientId = clientId;
		this.ghName = ghName;
		this.peopleCnt = peopleCnt;
		this.checkInDate = checkInDate;
		this.nights = nights;
		this.totalPrice = totalPrice;
	}
	
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getBookingId() {
		return bookingId;
	}

	public void setBookingId(String bookingId) {
		this.bookingId = bookingId;
	}

	public String getGhName() {
		return ghName;
	}

	public void setGhName(String ghName) {
		this.ghName = ghName;
	}

	public int getPeopleCnt() {
		return peopleCnt;
	}

	public void setPeopleCnt(int peopleCnt) {
		this.peopleCnt = peopleCnt;
	}

	public LocalDate getCheckInDate() {
		return checkInDate;
	}

	public void setCheckInDate(LocalDate checkInDate) {
		this.checkInDate = checkInDate;
	}

	public int getNights() {
		return nights;
	}

	public void setNights(int nights) {
		this.nights = nights;
	}

	public int getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(int totalPrice) {
		this.totalPrice = totalPrice;
	}

	@Override
	public String toString() {
		return "Booking [bookingId=" + bookingId + ", peopleCnt=" + peopleCnt + ", checkInDate=" + checkInDate + ", nights=" + nights
				+ ", bookingStatus=" + ", totalPrice=" + totalPrice + "]";
	}
	
}
