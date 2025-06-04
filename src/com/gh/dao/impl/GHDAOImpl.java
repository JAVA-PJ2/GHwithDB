package com.gh.dao.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

	private boolean canBook(String ghName, LocalDate checkIn, int nights, int people) throws SQLException {
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
	        ps.setString(1, ghName);
	        ps.setDate(2, Date.valueOf(checkIn));
	        ps.setDate(3, Date.valueOf(checkIn));
	        ps.setInt(4, nights);
	        ps.setString(5, ghName);

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
	        	String findQuery = "SELECT max_capacity FROM guesthouse WHERE gh_name=?";
	        	ps = conn.prepareStatement(findQuery);
	        	ps.setString(1, ghName);
	        	rs = ps.executeQuery();
	        	
	        	if(rs.next()) {
	        		int maxCapacity = rs.getInt("max_capacity");
	        		return people <= maxCapacity;
	        	} else {
	        		// 게스트하우스 이름이 존재하지 않을 경우
	        		System.out.println("[" + ghName + " 은/는 존재하지 않는 게스트하우스입니다]");
	        		return false;
	        	}
	        }

	        // 예약 가능
	        return true;
	        
	    } finally {
	        closeAll(rs, ps, conn);
	    }
	}

	public int getDayBetweenBooking(String clientId) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Date beforeDate = null;
		
		try {
			conn = getConnect();
			String query = "SELECT max(b.check_in) AS bdate FROM booking b "
					+ "JOIN booking_detail bd ON b.booking_id = bd.booking_id"
					+ "WHERE client_id = ? AND bd.booking_status = 'S'";
			ps = conn.prepareStatement(query);
			ps.setString(1, clientId);
			rs = ps.executeQuery();
			
			if(rs.next()) 
				 beforeDate = rs.getDate("bdate");
				
		}finally {
			closeAll(rs, ps, conn);
		}
		
		if(beforeDate == null) {
			System.out.println("이전 방문한 기록이 없습니다.");
			return -1;
		}
		
		LocalDate lastCheckIn = beforeDate.toLocalDate();
		LocalDate today = LocalDate.now();
		return (int) ChronoUnit.DAYS.between(lastCheckIn, today);
	}
	// 사용 방법 : 0보다 작으면(-1) 이전에 예약한 기록이 없습니다, 크면 getDayBetweenBooking()일만에 예약하셧습니다 하면 됩니다

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
}
