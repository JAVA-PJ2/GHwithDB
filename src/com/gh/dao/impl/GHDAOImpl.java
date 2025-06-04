package com.gh.dao.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import com.gh.dao.GHDAO;
import com.gh.vo.Booking;
import com.gh.vo.Client;
import com.gh.vo.Guesthouse;

import config.ServerInfo;

public class GHDAOImpl implements GHDAO {
	// 싱글톤
	private static GHDAOImpl dao = new GHDAOImpl("127.0.0.1");
	
	private GHDAOImpl(String serverIp) {
		try {
			Class.forName(ServerInfo.DRIVER_NAME);
			System.out.println("드라이버 로딩 성공...");
		}catch(ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static GHDAOImpl getInstance() {
		return dao;
	}
	
	//공통로직
	private Connection getConnect() throws SQLException {
		Connection conn = DriverManager.getConnection(ServerInfo.URL, ServerInfo.USER, ServerInfo.PASS);
		return conn;
	}
	
	private void closeAll(PreparedStatement ps, Connection conn) throws SQLException {
		if(ps != null) ps.close();
		if(conn != null) conn.close();
	}
	
	private void closeAll(ResultSet rs, PreparedStatement ps, Connection conn) throws SQLException {
		if(rs != null) rs.close();
		closeAll(ps, conn);
	}
	
	// Tier 계산하는 메소드
	private void applyTier(Client c) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int count = 0;
		
		try {
			conn = getConnect();
			String query = "SELECT COUNT(*) AS cnt\r\n"
					+ "FROM booking b\r\n"
					+ "JOIN booking_detail bd ON b.gh_name = bd.gh_name AND b.check_in = bd.booking_date\r\n"
					+ "WHERE b.client_id = ? AND bd.booking_status = 'S';";
			ps = conn.prepareStatement(query);
			
			ps.setString(1, c.getId());
			
			rs = ps.executeQuery();
		
			
			if(rs.next()) {
				count = rs.getInt("cnt");
			}

			if (count >= 10) {
			    c.setTier('G');
			} else if (count >= 5) {
			    c.setTier('S');
			} else if (count >= 3) {
			    c.setTier('B');
			} else {
			    c.setTier('U');
			}

			System.out.println("티어 계산 완료 !");
			
		} finally {
			closeAll(rs, ps, conn);
		}
	}
	
	@Override
	private double calcDiscountByTier(Client c) {
		if(c.getTier() == null) {
			applyTier(c);
		}
		
		return 0;
	}
	
	@Override
	private double calcDiscountByTier(Client c) {

		return 0;
	}

	@Override
	private boolean canBook(Guesthouse gh, LocalDate checkIn, int nights, int people) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	private int getDayBetweenBooking(LocalDate previousCheckIn) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void login(String id, String password) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reserveBooking(Client client, Booking booking) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<Guesthouse> recommendGH(int price, char mbti) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void printMyInfo(Client c) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Integer> getWeeklyVisitorCount(LocalDate checkIn, LocalDate checkOut) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Integer> getWeeklySales(LocalDate checkIn, LocalDate checkOut) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Guesthouse getMostBookedGH(LocalDate checkIn, LocalDate checkOut) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double calAverageVisitInterval(Client client) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map<String, Double> calAverageStayByTier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double calCancelRate() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Client getClientById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Client> getAllClients() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Booking> getAllBookings() {
		// TODO Auto-generated method stub
		return null;
	}
}
