package com.gh.dao.impl;

import java.sql.Connection;
import java.sql.Date;
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
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}

	public static GHDAOImpl getInstance() {
		return dao;
	}

	// 공통로직
	private Connection getConnect() throws SQLException {
		Connection conn = DriverManager.getConnection(ServerInfo.URL, ServerInfo.USER, ServerInfo.PASS);
		return conn;
	}

	private void closeAll(PreparedStatement ps, Connection conn) throws SQLException {
		if (ps != null)
			ps.close();
		if (conn != null)
			conn.close();
	}

	private void closeAll(ResultSet rs, PreparedStatement ps, Connection conn) throws SQLException {
		if (rs != null)
			rs.close();
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
			String query = "SELECT COUNT(*) AS cnt\r\n" + "FROM booking b\r\n"
					+ "JOIN booking_detail bd ON b.gh_name = bd.gh_name AND b.check_in = bd.booking_date\r\n"
					+ "WHERE b.client_id = ? AND bd.booking_status = 'S';";
			ps = conn.prepareStatement(query);

			ps.setString(1, c.getId());

			rs = ps.executeQuery();

			if (rs.next()) {
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

	private double calcDiscountByTier(Client c) throws SQLException {
		double dc = 0.0;

		if (c.getTier() == null) {
			applyTier(c);
		}

		Character tier = c.getTier();

		switch (tier) {
		case 'B':
			dc = 0.05;
			break;
		case 'S':
			dc = 0.1;
			break;
		case 'G':
			dc = 0.15;
			break;
		default:
			dc = 0.0;
		}

		return dc;
	}

	public boolean canBook(Guesthouse gh, LocalDate checkIn, int nights, int people) throws SQLException {
	    // 2025.05.05 ~ 2025.06.29 사이만 예약 가능
		LocalDate minDate = LocalDate.of(2025, 5, 5);
	    LocalDate maxDate = LocalDate.of(2025, 6, 29);
	    
	    LocalDate lastDate = checkIn.plusDays(nights -1);
	    
	    if(checkIn.isBefore(minDate) || lastDate.isAfter(maxDate)) {
	    	System.out.println("[2025.05.05 ~ 2025.06.29 사이만 예약 가능합니다]");
	    	return false;
	    }
		
		// DB에서 예약 확인 후 예약 자리가 남았는지 확인
		Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        conn = getConnect();

	        String query =
	            "SELECT t.booking_date, SUM(t.people) AS reserved, g.max_capacity " +
	            "FROM ( " +
	            "   SELECT DISTINCT b.booking_id, b.people, bd.booking_date " +
	            "   FROM booking b " +
	            "   JOIN booking_detail bd ON b.gh_name = bd.gh_name " +
	            "     AND bd.booking_date BETWEEN b.check_in AND DATE_ADD(b.check_in, INTERVAL b.nights - 1 DAY) " +
	            "   WHERE b.gh_name = ? " +
	            "     AND bd.booking_status IN ('R', 'S') " +
	            "     AND bd.booking_date BETWEEN ? AND DATE_ADD(?, INTERVAL ? - 1 DAY) " +
	            ") t " +
	            "JOIN guesthouse g ON g.gh_name = ? " +
	            "GROUP BY t.booking_date";

	        ps = conn.prepareStatement(query);
	        ps.setString(1, gh.getName());
	        ps.setDate(2, Date.valueOf(checkIn));
	        ps.setDate(3, Date.valueOf(checkIn));
	        ps.setInt(4, nights);
	        ps.setString(5, gh.getName());

	        rs = ps.executeQuery();

	        boolean empty = true;

	        while (rs.next()) {
	            empty = false;
	            int reserved = rs.getInt("reserved");
	            int max = rs.getInt("max_capacity");

	            if (reserved + people > max) {
	                return false;
	            }
	        }
	        
	        // 그 날 이 게하에 예약이 아예 없다면 로직 처리
	        if (empty) {
	            return people <= gh.getMaxCapacity();
	        }

	        // 예약 가능
	        return true;
	        
	    } finally {
	        closeAll(rs, ps, conn);
	    }
	}


	@Override
	private int getDayBetweenBooking(LocalDate previousCheckIn) {

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
