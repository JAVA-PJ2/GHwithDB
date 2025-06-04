package com.gh.vo;

public class Booking {
	private String bookingId; // 칼럼에는 booking_id
	private int peopleCnt; // 칼럼에는 people
	private String checkInDate; // 칼럼에는 check_in
	private int nights;
	private int totalPrice; // 칼럼에는 total_price
	private String gh_name;
	
	public Booking(){}
	
	public Booking(String bookingId, String gh_name, int peopleCnt, String checkInDate, int nights, int totalPrice) {
		super();
		this.bookingId = bookingId;
		this.gh_name = gh_name;
		this.peopleCnt = peopleCnt;
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

	public String getGh_name() {
		return gh_name;
	}

	public void setGh_name(String gh_name) {
		this.gh_name = gh_name;
	}

	public int getPeopleCnt() {
		return peopleCnt;
	}

	public void setPeopleCnt(int peopleCnt) {
		this.peopleCnt = peopleCnt;
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
		return "Booking [bookingId=" + bookingId + ", peopleCnt=" + peopleCnt + ", checkInDate=" + checkInDate + ", nights=" + nights
				+ ", bookingStatus=" + ", totalPrice=" + totalPrice + "]";
	}
	
}
