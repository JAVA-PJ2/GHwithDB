package com.gh.vo;

public class Guesthouse {
	private String name; // 칼럼에는 gh_name
	private char mbti;
	private int priceWeekday; // 칼럼에는 price_weekday
	private int priceWeekend; // 칼럼에는 price_weekend
	private int maxCapacity; // 칼럼에는 max_capacity
	
	public Guesthouse(){}
	
	public Guesthouse(String name, char mbti, int priceWeekday, int priceWeekend, int maxCapacity) {
		super();
		this.name = name;
		this.mbti = mbti;
		this.priceWeekday = priceWeekday;
		this.priceWeekend = priceWeekend;
		this.maxCapacity = maxCapacity;
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
	
	@Override
	public String toString() {
		return "Guesthouse [name=" + name + ", mbti=" + mbti + ", priceWeekday=" + priceWeekday + ", priceWeekend="
				+ priceWeekend + ", maxCapacity=" + maxCapacity + "]";
	}
}
