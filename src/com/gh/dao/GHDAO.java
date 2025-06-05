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
	void login(String id, String password, String type) throws SQLException;
	Client login(String id, String password) throws SQLException;
	void reserveBooking(Client client, Booking booking) throws SQLException;
	ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt) throws SQLException;
	ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt, int price, char mbti) throws SQLException;
	void printMyInfo(Client c);
	void cancelBooking(Client client, String bookingId) throws SQLException;
	void updateBooking(Client client, Booking booking) throws SQLException;
	Guesthouse getGuesthouse(String ghName) throws SQLException;
	/*
	 * Manager
	 */
	Map<String, Integer> getWeeklyVisitorCount(LocalDate checkIn, LocalDate checkOut) throws SQLException;
	Map<String, Integer> getWeeklySales(LocalDate checkIn, LocalDate checkOut) throws SQLException;
	Guesthouse getMostBookedGH(LocalDate checkIn, LocalDate checkOut) throws SQLException;
	double calAverageVisitInterval(Client client) throws SQLException;
	Map<String, Double> calAverageStayByTier() throws DMLException, SQLException;
	double calCancelRate() throws SQLException;
	Client getClientById(String id) throws SQLException;
	ArrayList<Client> getAllClients() throws SQLException;
	ArrayList<Booking> getBookings(String clientId) throws SQLException;
	ArrayList<Booking> getAllBookings() throws SQLException;
	String analzeTendencyByTier(Client c) throws SQLException;
	ArrayList<Guesthouse> recommendGH(int price, char mbti);
	
}
