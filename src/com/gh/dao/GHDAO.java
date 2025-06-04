package com.gh.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import com.gh.exception.DMLException;
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
	ArrayList<Guesthouse> searchAvailableGH(String checkIn, String checkout, int peopleCnt);
	ArrayList<Guesthouse> searchAvailableGH(String checkIn, String checkout, int peopleCnt, int price, char mbti);
	void printMyInfo(Client c);
	void cancleBooking(Client client, String bookingId) throws SQLException;
	void updateBooking(Client client, Booking booking) throws SQLException;
	/*
	 * Manager
	 */
	Map<String, Integer> getWeeklyVisitorCount(LocalDate checkIn, LocalDate checkOut) throws SQLException;
	Map<String, Integer> getWeeklySales(LocalDate checkIn, LocalDate checkOut) throws SQLException;
	Guesthouse getMostBookedGH(LocalDate checkIn, LocalDate checkOut) throws SQLException;
	double calAverageVisitInterval(Client client);
	Map<String, Double> calAverageStayByTier() throws DMLException, SQLException;
	double calCancelRate();
	Client getClientById(String id);
	ArrayList<Client> getAllClients();
	ArrayList<Booking> getAllBookings();
	String analzeTendencyByTier(Client c) throws SQLException;
	ArrayList<Guesthouse> recommendGH(int price, char mbti);
}
