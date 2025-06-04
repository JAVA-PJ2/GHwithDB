package com.gh.dao;

import java.sql.SQLException;
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
	void login(String id, String password);
	void logout();
	void reserveBooking(Client client, Booking booking) throws SQLException;
	ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt) throws SQLException;
	ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt, int price, char mbti) throws SQLException;
	void printMyInfo(Client c);
	
	
	/*
	 * Manager
	 */
	Map<String, Integer> getWeeklyVisitorCount(LocalDate checkIn, LocalDate checkOut) throws SQLException;
	Map<String, Integer> getWeeklySales(LocalDate checkIn, LocalDate checkOut) throws SQLException;
	Guesthouse getMostBookedGH(LocalDate checkIn, LocalDate checkOut);
	double calAverageVisitInterval(Client client);
	Map<String, Double> calAverageStayByTier();
	double calCancelRate();
	Client getClientById(String id);
	ArrayList<Client> getAllClients();
	ArrayList<Booking> getAllBookings();
	String analzeTendencyByTier(Client c) throws SQLException;
	ArrayList<Guesthouse> recommendGH(int price, char mbti);
}
