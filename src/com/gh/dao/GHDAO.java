package com.gh.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import com.gh.exception.BookCancelledException;
import com.gh.exception.DMLException;
import com.gh.exception.RecordNotFoundException;
import com.gh.vo.Booking;
import com.gh.vo.Client;
import com.gh.vo.Guesthouse;

public interface GHDAO {
	
	/*
	 * Client
	 */
	boolean login(String id, String password, String type) throws RecordNotFoundException;
	Client login(String id, String password) throws RecordNotFoundException;
	void reserveBooking(Client client, Booking booking) throws BookCancelledException, RecordNotFoundException;
	ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt) throws RecordNotFoundException;
	ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt, int price, char mbti) throws RecordNotFoundException;
	void printMyInfo(Client c) throws RecordNotFoundException;
	void cancelBooking(Client client, String bookingId) throws RecordNotFoundException;
	void updateBooking(Client client, Booking booking) throws RecordNotFoundException;
	Guesthouse getGuesthouse(String ghName) throws RecordNotFoundException;
	/*
	 * Manager
	 */
	Map<String, Integer> getWeeklyVisitorCount(LocalDate checkIn, LocalDate checkOut) throws RecordNotFoundException;
	Map<String, Integer> getWeeklySales(LocalDate checkIn, LocalDate checkOut) throws RecordNotFoundException, DMLException;
	Guesthouse getMostBookedGH(LocalDate checkIn, LocalDate checkOut) throws RecordNotFoundException;
	double calcAverageVisitInterval(String clientId) throws RecordNotFoundException;
	Map<String, Double> calAverageStayByTier() throws DMLException;
	ArrayList<Booking> getBookingsByClientId(String clientId) throws RecordNotFoundException;
	double calCancelRate() throws RecordNotFoundException;
	Client getClientById(String id) throws RecordNotFoundException;
	ArrayList<Client> getAllClients() throws RecordNotFoundException;
	ArrayList<Booking> getBookings(String clientId) throws RecordNotFoundException;
	ArrayList<Booking> getAllBookings() throws RecordNotFoundException;
	String analzeTendencyByTier() throws DMLException;
	ArrayList<Guesthouse> recommendGH(int price, char mbti);
	
}
