package com.gh.vo;

public class Booking {
	private String bookingId; // 칼럼에는 booking_id
	private int people; // 칼럼에는 people
	private String checkInDate; // 칼럼에는 check_in
	private int nights;
	private int totalPrice; // 칼럼에는 total_price
	
	public Booking(){}
	
	public Booking(String bookingId, int people, String checkInDate, int nights, int totalPrice) {
		super();
		this.bookingId = bookingId;
		this.people = people;
		this.checkInDate = checkInDate;
		this.nights = nights;
		this.totalPrice = totalPrice;
	}
	
	public String getBookingId() {
		return bookingId;
	}

	public void setBookingId(String bookingId) {
		this.bookingId = bookingId;
	}

	public int getpeople() {
		return people;
	}

	public void setpeople(int people) {
		this.people = people;
	}

	public String getcheckInDate() {
		return checkInDate;
	}

	public void setcheckInDate(String checkInDate) {
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
		return "Booking [bookingId=" + bookingId + ", people=" + people + ", checkInDate=" + checkInDate + ", nights=" + nights
				+ ", bookingStatus=" + ", totalPrice=" + totalPrice + "]";
	}
	
}
