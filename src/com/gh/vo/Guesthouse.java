package com.gh.vo;

public class Guesthouse {
	private String gh_name;
	private String mbti;
	private int price_weekday;
	private int price_weekend;
	private int max_capacity;
	
	public Guesthouse(){}
	
	public Guesthouse(String gh_name, String mbti, int price_weekday, int price_weekend, int max_capacity) {
		super();
		this.gh_name = gh_name;
		this.mbti = mbti;
		this.price_weekday = price_weekday;
		this.price_weekend = price_weekend;
		this.max_capacity = max_capacity;
	}
	
	public String getGh_name() {
		return gh_name;
	}

	public void setGh_name(String gh_name) {
		this.gh_name = gh_name;
	}

	public String getMbti() {
		return mbti;
	}

	public void setMbti(String mbti) {
		this.mbti = mbti;
	}

	public int getPrice_weekday() {
		return price_weekday;
	}

	public void setPrice_weekday(int price_weekday) {
		this.price_weekday = price_weekday;
	}

	public int getPrice_weekend() {
		return price_weekend;
	}

	public void setPrice_weekend(int price_weekend) {
		this.price_weekend = price_weekend;
	}

	public int getMax_capacity() {
		return max_capacity;
	}

	public void setMax_capacity(int max_capacity) {
		this.max_capacity = max_capacity;
	}

	@Override
	public String toString() {
		return "Guesthouse [gh_name=" + gh_name + ", mbti=" + mbti + ", price_weekday=" + price_weekday
				+ ", price_weekend=" + price_weekend + ", max_capacity=" + max_capacity + "]";
	}
}
