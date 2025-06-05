package com.gh.vo;

import java.util.ArrayList;

public class Client {
	private String id; // 칼럼에는 client_id
	private String password; // 칼럼에는 client_password
	private String name; // 칼럼에는 client_name
	private char mbti;
	private Character tier;
	private ArrayList<Booking> bookings;
	
	public Client() {}

	public Client(String id, String password, String name, char mbti, Character tier) {
		super();
		this.id = id;
		this.password = password;
		this.name = name;
		this.mbti = mbti;
		this.tier = tier;
	}
	
	public Client(String id, String password, String name, char mbti, Character tier, ArrayList<Booking> bookings) {
		super();
		this.id = id;
		this.password = password;
		this.name = name;
		this.mbti = mbti;
		this.tier = tier;
		this.bookings = bookings;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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

	public Character getTier() {
		return tier;
	}

	public void setTier(Character tier) {
		this.tier = tier;
	}

	public ArrayList<Booking> getBookings() {
		return bookings;
	}

	public void setBookings(ArrayList<Booking> bookings) {
		this.bookings = bookings;
	}

	@Override
	public String toString() {
		return "Client [id=" + id + ", password=" + password + ", name=" + name + ", mbti=" + mbti + ", tier=" + tier
				+ ", bookings=" + bookings + "]";
	};
	
}
