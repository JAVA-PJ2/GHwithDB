package com.gh.vo;

import java.util.ArrayList;

public class Guesthouse {
	private String name;
	private char mbti;
	private int priceWeekday;
	private int priceWeekend;
	private int maxCapacity;
	private ArrayList<Booking> bookings;
	
	public Guesthouse(){}
	
	public Guesthouse(String name, char mbti, int priceWeekday, int priceWeekend, int maxCapacity,
			ArrayList<Booking> bookings) {
		super();
		this.name = name;
		this.mbti = mbti;
		this.priceWeekday = priceWeekday;
		this.priceWeekend = priceWeekend;
		this.maxCapacity = maxCapacity;
		this.bookings = bookings;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public char getMbti() {
		return mbti;
	}

	public void setMbti(char mbti) {
		this.mbti = mbti;
	}

	public int getPriceWeekday() {
		return priceWeekday;
	}

	public void setPriceWeekday(int priceWeekday) {
		this.priceWeekday = priceWeekday;
	}

	public int getPriceWeekend() {
		return priceWeekend;
	}

	public void setPriceWeekend(int priceWeekend) {
		this.priceWeekend = priceWeekend;
	}

	public int getMaxCapacity() {
		return maxCapacity;
	}

	public void setMaxCapacity(int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	public ArrayList<Booking> getBookings() {
		return bookings;
	}

	public void setBookings(ArrayList<Booking> bookings) {
		this.bookings = bookings;
	}
	
	@Override
	public String toString() {
		return "Guesthouse [name=" + name + ", mbti=" + mbti + ", priceWeekday=" + priceWeekday + ", priceWeekend="
				+ priceWeekend + ", maxCapacity=" + maxCapacity + ", bookings=" + bookings + "]";
	}
}
