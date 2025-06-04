package com.gh.dao.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

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
	private void applyTier(Client c) {
		
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

	private boolean canBook(String gh_name, LocalDate checkIn, int nights, int people) {
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

	private boolean checkClient(Client client) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getConnect();
			String query = "SELECT client_id FROM client WHERE client_id=?";
			ps = conn.prepareStatement(query);
			ps.setString(1, client.getId());
			rs = ps.executeQuery();
			if (rs.next()) {
				return true;
			} else return false;
		} finally {
			closeAll(rs, ps, conn);
		}
	}
	
	@Override
	public void reserveBooking(Client client, Booking booking) throws SQLException  {
		Connection conn = null;
		PreparedStatement ps = null;
		if (checkClient(client)) {
			try {
				conn = getConnect();
				String uuid = UUID.randomUUID().toString();
				if (canBook(booking.getBookingId(), booking.getcheckInDate(), booking.getNights(), booking.getPeopleCnt())) {
					String query = "INSERT INTO booking VALUES (?, ?, ?, ?, ?, ?, ?)";
					ps = conn.prepareStatement(query);
					ps.setString(1, uuid);
					ps.setString(2, client.getId());
					ps.setString(3, booking.getGh_name());
					ps.setInt(4, booking.getPeopleCnt());
					ps.setString(5, booking.getcheckInDate().toString());
					ps.setInt(6, booking.getNights());
					ps.setInt(7, booking.getTotalPrice());
					System.out.println(ps.executeUpdate() + "개의 예약이 완료되었습니다.");
				}else {
					System.out.println("예약할 수 없습니다.");
				}
			} finally {
				closeAll(ps, conn);
			}
		}else {
			System.out.println("등록된 ID가 아닙니다.");
		}
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

	@Override
	public double calcDiscountByTier(Client c) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean canBook(Guesthouse gh, LocalDate checkIn, int nights, int people) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getDayBetweenBooking(LocalDate previousCheckIn) {
		// TODO Auto-generated method stub
		return 0;
	}
}
