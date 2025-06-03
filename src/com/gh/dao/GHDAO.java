package com.gh.dao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import com.gh.vo.Booking;
import com.gh.vo.Client;
import com.gh.vo.Guesthouse;

public interface GHDAO {
	
	/*
	 * Client
	 */
	double calcDiscountByTier(Client c);
	boolean canBook(Guesthouse gh, LocalDate checkIn, int nights, int people);
	int getDayBetweenBooking(LocalDate previousCheckIn);
	void login(String id, String password);
	void logout();
	void reserveBooking(Client client, Booking booking);
	ArrayList<Guesthouse> recommendGH(int price, char mbti);
	void printMyInfo(Client c);
	
	/*
	 * Manager
	 */
	Map<String, Integer> getWeeklyVisitorCount(LocalDate checkIn, LocalDate checkOut);
	Map<String, Integer> getWeeklySales(LocalDate checkIn, LocalDate checkOut);
	Guesthouse getMostBookedGH(LocalDate checkIn, LocalDate checkOut);
	double calAverageVisitInterval(Client client);
	Map<String, Double> calAverageStayByTier();
	double calCancelRate();
	Client getClientById(String id);
	ArrayList<Client> getAllClients();
	ArrayList<Booking> getAllBookings();
}
