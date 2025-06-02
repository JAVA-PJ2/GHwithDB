package com.gh.vo;

public class Booking {
	private String bookingId;
	private int peopleCount;
	private String checkIn;
	private int nights;
	private String bookingStatus;
	private int totalPrice;
	public Booking() {
	}
	
	public Booking(String bookingId, int peopleCount, String checkIn, int nights, String bookingStatus, int totalPrice) {
		super();
		this.bookingId = bookingId;
		this.peopleCount = peopleCount;
		this.checkIn = checkIn;
		this.nights = nights;
		this.bookingStatus = bookingStatus;
		this.totalPrice = totalPrice;
	}
	
	public String getBookingId() {
		return bookingId;
	}

	public void setBookingId(String bookingId) {
		this.bookingId = bookingId;
	}

	public int getPeopleCount() {
		return peopleCount;
	}

	public void setPeopleCount(int peopleCount) {
		this.peopleCount = peopleCount;
	}

	public String getCheckIn() {
		return checkIn;
	}

	public void setCheckIn(String checkIn) {
		this.checkIn = checkIn;
	}

	public int getNights() {
		return nights;
	}

	public void setNights(int nights) {
		this.nights = nights;
	}

	public String getBookingStatus() {
		return bookingStatus;
	}

	public void setBookingStatus(String bookingStatus) {
		this.bookingStatus = bookingStatus;
	}

	public int getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(int totalPrice) {
		this.totalPrice = totalPrice;
	}

	@Override
	public String toString() {
		return "Booking [bookingId=" + bookingId + ", peopleCount=" + peopleCount + ", checkIn=" + checkIn + ", nights=" + nights
				+ ", bookingStatus=" + bookingStatus + ", totalPrice=" + totalPrice + "]";
	}
	
}
