package com.gh.dao.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.gh.dao.GHDAO;
import com.gh.dao.PasswordUtil;
import com.gh.exception.DMLException;
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

		LocalDate lastDate = checkIn.plusDays(nights - 1);

		if (checkIn.isBefore(minDate) || lastDate.isAfter(maxDate)) {
			System.out.println("[2025.05.05 ~ 2025.06.29 사이만 예약 가능합니다]");
			return false;
		}

		// DB에서 예약 확인 후 예약 자리가 남았는지 확인
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnect();

			String query = "SELECT t.booking_date, SUM(t.people) AS reserved, g.max_capacity " + "FROM ( "
					+ "   SELECT DISTINCT b.booking_id, b.people, bd.booking_date " + "   FROM booking b "
					+ "   JOIN booking_detail bd ON b.gh_name = bd.gh_name "
					+ "     AND bd.booking_date BETWEEN b.check_in AND DATE_ADD(b.check_in, INTERVAL b.nights - 1 DAY) "
					+ "   WHERE b.gh_name = ? " + "     AND bd.booking_status IN ('R', 'S') "
					+ "     AND bd.booking_date BETWEEN ? AND DATE_ADD(?, INTERVAL ? - 1 DAY) " + ") t "
					+ "JOIN guesthouse g ON g.gh_name = ? " + "GROUP BY t.booking_date";

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

				if (rs.next()) {
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
					+ "JOIN booking_detail bd ON b.booking_id = bd.booking_id "
					+ "WHERE client_id = ? AND bd.booking_status = 'S'";
			ps = conn.prepareStatement(query);
			ps.setString(1, clientId);
			rs = ps.executeQuery();

			if (rs.next())
				beforeDate = rs.getDate("bdate");

		} finally {
			closeAll(rs, ps, conn);
		}

		if (beforeDate == null) {
			return -1;
		}

		LocalDate lastCheckIn = beforeDate.toLocalDate();
		LocalDate today = LocalDate.now();
		return (int) ChronoUnit.DAYS.between(lastCheckIn, today);
	}
	// 사용 방법 : 0보다 작으면(-1) 이전에 방문한 기록이 없습니다, 크면 getDayBetweenBooking()일만에 예약하셧습니다 하면
	// 됩니다

	@Override
	public void login(String id, String password, String type) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnect();

			String query = null;
			String column = null;
			
			if ("client".equalsIgnoreCase(type)) {
				query = "SELECT client_password FROM client WHERE client_id=?";
				column = "client_password";
			} else if ("manager".equalsIgnoreCase(type)) {
				query = "SELECT manager_password FROM manager WHERE manager_id=?";
				column = "manager_password";
			} else {
				System.out.println("유효하지 않은 사용자 유형입니다 (client 또는 manager)");
				return;
			}

			ps = conn.prepareStatement(query);
			ps.setString(1, id);
			rs = ps.executeQuery();

			if (rs.next()) {
				String storedHash = rs.getString(column);
				String inputHash = PasswordUtil.encrypt(password);

				if (storedHash.equalsIgnoreCase(inputHash))
					System.out.println(id + "님 로그인 성공 ! ");
				else
					System.out.println("비밀번호가 일치하지 않습니다.");
			} else {
				System.out.println("해당 ID는 존재하지 않습니다.");
			}

		} finally {
			closeAll(rs, ps, conn);
		}

	}

	private boolean checkId(Client client) throws SQLException {
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
			} else
				return false;
		} finally {
			closeAll(rs, ps, conn);
		}
	}

	private int calcTotalPrice(LocalDate checkIn, int nights, int weekdayPrice, int weekendPrice) {
		int total = 0;
		for (int i = 0; i < nights; i++) {
			LocalDate current = checkIn.plusDays(i);
			DayOfWeek day = current.getDayOfWeek();
			if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
				total += weekendPrice;
			} else {
				total += weekdayPrice;
			}
		}
		return total;
	}
	
	@Override
	public Guesthouse getGuesthouse(String ghName) throws SQLException {
		Guesthouse gh = null;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getConnect();
			String query = "SELECT gh_name, mbti, price_weekday, price_weekend, max_capacity from guesthouse WHERE gh_name=?";
			ps = conn.prepareStatement(query);
			ps.setString(1, ghName);
			rs = ps.executeQuery();
			if (rs.next()) {
				gh = new Guesthouse(rs.getString("gh_name"),
									rs.getString("mbti").charAt(0),
									rs.getInt("price_weekday"),
									rs.getInt("price_weekend"),
									rs.getInt("max_capacity"));
			} else {
				System.out.println("찾으시는 게스트 하우스가 없습니다.");
			}
		} finally {
			closeAll(rs, ps, conn);
		}
		return gh;
	}
	
	@Override
	public void reserveBooking(Client client, Booking booking) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			String uuid = UUID.randomUUID().toString();
			booking.setBookingId(uuid);
			if (canBook(booking.getBookingId(), booking.getCheckInDate(), booking.getNights(),
					booking.getPeopleCnt())) {
				Guesthouse gh = getGuesthouse(booking.getGhName());
				if (gh == null) {
					System.out.println("찾으시는 게스트하우스는 존재하지 않습니다.");
					return ;
				}
				int totalPrice = calcTotalPrice(booking.getCheckInDate(), booking.getNights(),
												gh.getPriceWeekday(), gh.getPriceWeekend());
				conn = getConnect();
				String query = "INSERT INTO booking VALUES (?, ?, ?, ?, ?, ?, ?)";
				ps = conn.prepareStatement(query);
				ps.setString(1, uuid);
				ps.setString(2, client.getId());
				ps.setString(3, booking.getGhName());
				ps.setInt(4, booking.getPeopleCnt());
				ps.setString(5, booking.getCheckInDate().toString());
				ps.setInt(6, booking.getNights());
				if (client.getTier().equals('G'))
					ps.setInt(7, (int)(totalPrice * 0.85));
				else if (client.getTier().equals('S'))
					ps.setInt(7, (int)(totalPrice * 0.9));
				else if (client.getTier().equals('B'))
					ps.setInt(7, (int)(totalPrice * 0.95));
				else
					ps.setInt(7, totalPrice);
				ps.executeUpdate();
				conn = getConnect();
				query = "INSERT INTO booking_detail (gh_name, booking_date, booking_status, booking_id) VALUES (?, ?, ?, ?)";
				ps = conn.prepareStatement(query);
				ps.setString(1, booking.getGhName());
				ps.setString(2, booking.getCheckInDate().toString());
				ps.setString(3, "R");
				ps.setString(4, uuid);
				System.out.println(ps.executeUpdate() + "개 예약 완료되었습니다.");
			} else {
				System.out.println("예약할 수 없습니다.");
			}
		} finally {
			closeAll(ps, conn);
		}
	}

	private boolean checkBookingStatus(String bookingId) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getConnect();
			String query = "SELECT booking_status FROM booking_detail WHERE booking_id=?";
			ps = conn.prepareStatement(query);
			ps.setString(1, bookingId);
			rs = ps.executeQuery();
			if (rs.next()) {
				if (rs.getString("booking_status").equals("R"))
					return true;
				else
					return false;
			} else
				return false;
		} finally {
			closeAll(rs, ps, conn);
		}
	}

	private String checkId(String bookingId) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getConnect();
			String query = "SELECT client_id FROM booking WHERE booking_id=?";
			ps = conn.prepareStatement(query);
			ps.setString(1, bookingId);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getString("client_id");
			} else
				return null;
		} finally {
			closeAll(rs, ps, conn);
		}
	}

	@Override
	public ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt) throws SQLException {
		ArrayList<Guesthouse> availableList = new ArrayList<>();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnect();

			String query = "SELECT * FROM guesthouse";
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				String ghName = rs.getString("gh_name");
				String mbtiStr = rs.getString("mbti");
				Character mbti = (mbtiStr != null && !mbtiStr.isEmpty()) ? mbtiStr.charAt(0) : null;
				int priceWeekday = rs.getInt("price_weekday");
				int priceWeekend = rs.getInt("price_weekend");
				int maxCapacity = rs.getInt("max_capacity");

				Guesthouse gh = new Guesthouse(ghName, mbti, priceWeekday, priceWeekend, maxCapacity);

				if (canBook(ghName, checkIn, night, peopleCnt)) {
					availableList.add(gh);
				}
			}

		} finally {
			closeAll(rs, ps, conn);
		}

		return availableList;
	}

	@Override
	public ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt, int price, char mbti)
			throws SQLException {
		ArrayList<Guesthouse> availableList = new ArrayList<>();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnect();

			String query = "SELECT g.*, " + "  IFNULL(cancel_count, 0) / IFNULL(total_count, 1) AS cancel_rate "
					+ "FROM guesthouse g " + "LEFT JOIN ( " + "    SELECT gh_name, "
					+ "           SUM(CASE WHEN booking_status = 'C' THEN 1 ELSE 0 END) AS cancel_count, "
					+ "           COUNT(*) AS total_count " + "    FROM booking_detail " + "    GROUP BY gh_name "
					+ ") stats ON g.gh_name = stats.gh_name " + "WHERE g.price_weekday <= ? AND g.price_weekend <= ? "
					+ "ORDER BY " + "  CASE WHEN ? = 'E' AND g.mbti = 'E' THEN 0 "
					+ "       WHEN ? = 'I' AND g.mbti = 'I' THEN 0 ELSE 1 END, " + "  cancel_rate ASC";

			ps = conn.prepareStatement(query);
			ps.setInt(1, price);
			ps.setInt(2, price);
			ps.setString(3, String.valueOf(mbti));
			ps.setString(4, String.valueOf(mbti));

			rs = ps.executeQuery();

			while (rs.next()) {
				String ghName = rs.getString("gh_name");
				String mbtiStr = rs.getString("mbti");
				Character mbtiChar = (mbtiStr != null && !mbtiStr.isEmpty()) ? mbtiStr.charAt(0) : null;
				int priceWeekday = rs.getInt("price_weekday");
				int priceWeekend = rs.getInt("price_weekend");
				int maxCapacity = rs.getInt("max_capacity");

				Guesthouse gh = new Guesthouse(ghName, mbtiChar, priceWeekday, priceWeekend, maxCapacity);

				if (canBook(ghName, checkIn, night, peopleCnt)) {
					availableList.add(gh);
				}
			}

		} finally {
			closeAll(rs, ps, conn);
		}

		return availableList;
	}


	@Override
	public void updateBooking(Client client, Booking booking) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		if (client.getId().equals(checkId(booking.getBookingId()))) {
			try {
				if (canBook(booking.getGhName(), booking.getCheckInDate(), booking.getNights(), booking.getPeopleCnt())) {
					Guesthouse gh = getGuesthouse(booking.getGhName());
					int totalPrice = calcTotalPrice(booking.getCheckInDate(), booking.getNights(),
							gh.getPriceWeekday(), gh.getPriceWeekend());
					conn = getConnect();
					String query = "UPDATE booking SET check_in=?, people=?, nights=?, total_price=? WHERE booking_id=? AND client_id=?";
					ps = conn.prepareStatement(query);
					ps.setString(1, booking.getCheckInDate().toString());
					ps.setInt(2, booking.getPeopleCnt());
					ps.setInt(3, booking.getNights());
					if (client.getTier().equals('G'))
						ps.setInt(4, (int)(totalPrice * 0.85));
					else if (client.getTier().equals('S'))
						ps.setInt(4, (int)(totalPrice * 0.9));
					else if (client.getTier().equals('B'))
						ps.setInt(4, (int)(totalPrice * 0.95));
					else
						ps.setInt(4, totalPrice);
					ps.setString(5, booking.getBookingId());
					ps.setString(6, client.getId());
					if (ps.executeUpdate() == 1) {
						conn = getConnect();
						query = "UPDATE booking_detail SET booking_date=? WHERE booking_id =?";
						ps = conn.prepareStatement(query);
						ps.setString(1, booking.getCheckInDate().toString());
						ps.setString(2, booking.getBookingId());
						System.out.println(ps.executeUpdate() + "개 예약이 변경되었습니다.");
					} else 
						System.out.println("변경하실 수 없습니다.");
				} else {
					System.out.println("변경할 수 없는 날짜입니다.");
				}
			} finally {
				closeAll(ps, conn);
			}
		}else
			System.out.println("잘못된 사용자입니다.");
	}

	@Override
	public void cancelBooking(Client client, String bookingId) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		if (client.getId().equals(checkId(bookingId))) {
			if (checkBookingStatus(bookingId)) {
				try {
					conn = getConnect();
					String query = "UPDATE booking_detail SET booking_status=? WHERE booking_id=?";
					ps = conn.prepareStatement(query);
					ps.setString(1, "C");
					ps.setString(2, bookingId);
					if(ps.executeUpdate() == 1) {
						conn = getConnect();
						query = "UPDATE booking SET total_price=0 WHERE booking_id=?";
						ps = conn.prepareStatement(query);
						ps.setString(1, bookingId);
						ps.executeUpdate();
						System.out.println("예약이 취소되었습니다.");
					} else {
						System.out.println("잘못된 입력입니다.");
					}
				} finally {
					closeAll(ps, conn);
				}
			} else {
				System.out.println("취소할 수 있는 상태의 예약이 아닙니다.");
			}
		} else {
			System.out.println("잘못된 사용자입니다.");
		}
	}

	@Override
	public ArrayList<Guesthouse> recommendGH(int price, char mbti) {
		// TODO Auto-generated method stub
		return null;
	}

	// 내 정보 조회
	/**
	 * @author 양준용
	 */
	@Override
	public void printMyInfo(Client c) {
		if (c == null) {
			System.out.println("고객 정보가 없습니다.");
			return;
		}

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnect();

			// 1) 고객 등급 조회
			System.out.println("=== 내 정보 조회 ===");
			System.out.println("고객명: " + c.getName());
			System.out.println("고객 등급: " + c.getTier());

			// 2) 예약 횟수 조회 (전체 예약 횟수)
			String bookingCount = "SELECT COUNT(*) FROM booking WHERE client_id=?";
			ps = conn.prepareStatement(bookingCount);
			ps.setString(1, c.getId());
			rs = ps.executeQuery();
			if (rs.next()) {
				System.out.println("고객의 총 예약 횟수: " + rs.getInt(1));
			}

			// 예약 및 예약 상세 내역 조회 (조인 활용)
			// 조인 없이 사용을 한다면 데이터베이스에 쿼리 호출 및 횟수가 늘어나므로 성능상 주의가 필요
			String query = "SELECT b.booking_id, b.people, b.check_in, b.nights, b.total_price, "
					+ "bd.booking_detail_id, bd.gh_name, bd.booking_date, bd.booking_status " + "FROM booking b "
					+ "LEFT JOIN booking_detail bd ON b.booking_id = bd.booking_id " + "WHERE b.client_id = ? "
					+ "ORDER BY bd.booking_date DESC";

			ps = conn.prepareStatement(query);
			ps.setString(1, c.getId());
			rs = ps.executeQuery();

			StringBuilder checkedIn = new StringBuilder();
			StringBuilder upcoming = new StringBuilder();
			StringBuilder canceled = new StringBuilder();

			while (rs.next()) {
				// Booking 정보
				String bookingId = rs.getString("booking_id");
				int people = rs.getInt("people");
				String checkIn = rs.getString("check_in");
				int nights = rs.getInt("nights");
				int totalPrice = rs.getInt("total_price");

				// BookingDetail 정보
				String bookingDetailId = rs.getString("booking_detail_id");
				String ghName = rs.getString("gh_name");
				String bookingDate = rs.getString("booking_date");
				String bookingStatus = rs.getString("booking_status");

				String info = String.format("예약ID: %s, 인원: %d, 체크인: %s, 숙박일수: %d, 총금액: %d, 게스트하우스: %s, 예약일: %s\n",
						bookingId, people, checkIn, nights, totalPrice, ghName != null ? ghName : "-",
						bookingDate != null ? bookingDate : "-");

				if (bookingStatus == null)
					bookingStatus = "알 수 없음";

				switch (bookingStatus.toLowerCase()) {
				case "checked-in":
					checkedIn.append(info);
					break;
				case "upcoming":
					upcoming.append(info);
					break;
				case "canceled":
					canceled.append(info);
					break;
				default:
					// 기타 상태 처리 추가 가능
					break;
				}
			}

			System.out.println("\n[체크인 완료 내역]");
			System.out.println(checkedIn.length() > 0 ? checkedIn.toString() : "없음");

			System.out.println("[체크인 예정 일정]");
			System.out.println(upcoming.length() > 0 ? upcoming.toString() : "없음");

			System.out.println("[취소된 일정]");
			System.out.println(canceled.length() > 0 ? canceled.toString() : "없음");

		} catch (SQLException e) {
			System.out.println("DB 오류: " + e.getMessage());
		} finally {
			try {
				closeAll(rs, ps, conn);
			} catch (SQLException e) {
				System.out.println("자원 해제 오류: " + e.getMessage());
			}
		}
	}

	public Map<String, Integer> getWeeklyVisitorCount(LocalDate checkIn, LocalDate checkOut) throws SQLException {
		Map<String, Integer> result = new LinkedHashMap<>();

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnect();

			// 주별 방문 고객 수 집계
			String query = "SELECT DISTINCT " + " YEAR(check_in) AS year, " + " MONTH(check_in) AS month, "
					+ " WEEK(check_in, 1) AS week_num, "
					+ " COUNT(client_id) OVER (PARTITION BY YEAR(check_in), MONTH(check_in), WEEK(check_in, 1)) AS visitor_count "
					+ "FROM booking " + "WHERE check_in BETWEEN ? AND ? " + "ORDER BY year, month, week_num";

			ps = conn.prepareStatement(query);
			ps.setDate(1, java.sql.Date.valueOf(checkIn));
			ps.setDate(2, java.sql.Date.valueOf(checkOut));
			rs = ps.executeQuery();

			while (rs.next()) {
				int year = rs.getInt("year");
				int month = rs.getInt("month");
				int week = rs.getInt("week_num");
				int count = rs.getInt("visitor_count");

				String key = String.format("%d년 %02d월 %d주차", year, month, week);
				result.put(key, count);
			}

		} finally {
			closeAll(rs, ps, conn);
		}

		return result;
	}

	// 주별 매출 집계
	@Override
	public Map<String, Integer> getWeeklySales(LocalDate checkIn, LocalDate checkOut) throws SQLException {
	    Map<String, Integer> result = new LinkedHashMap<>();

	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        conn = getConnect();
	        
	        // 1. gh 테이블에서 요금 정보 가져오기
	        String ghquery = "SELECT gh_name, price_weekday, price_weekend FROM gh";
	        ps = conn.prepareStatement(ghquery);
	        rs = ps.executeQuery();
	        
	        Map<String, Integer> priceMap = new HashMap<>();
	        while (rs.next()) {
	            String ghName = rs.getString("gh_name");
	            int price = rs.getInt("price_weekday") + rs.getInt("price_weekend");
	            priceMap.put(ghName, price);
	        }
	        closeAll(rs, ps, null);

	        // 2. booking에서 주별, gh_name별 예약 인원 합계 계산
	        String query =
	            "SELECT " +
	            " YEAR(check_in) AS year, " +
	            " MONTH(check_in) AS month, " +
	            " WEEK(check_in, 1) AS week_num, " +
	            " gh_name, " +
	            " SUM(people) AS total_people " +
	            "FROM booking " + 
	            "WHERE check_in BETWEEN ? AND ? " +
	            "GROUP BY year, month, week_num, gh_name " +
	            "ORDER BY year, month, week_num";

	        ps = conn.prepareStatement(query);
	        ps.setDate(1, java.sql.Date.valueOf(checkIn));
	        ps.setDate(2, java.sql.Date.valueOf(checkOut));
	        rs = ps.executeQuery();

	        while (rs.next()) {
	            int year = rs.getInt("year");
	            int month = rs.getInt("month");
	            int week = rs.getInt("week_num");
	            String ghName = rs.getString("gh_name");
	            int people = rs.getInt("total_people");
	            
	            int price = priceMap.getOrDefault(ghName, 0);
	            int sale = people * price;
	            
	            String key = String.format("%d년 %02d월 %d주차", year, month, week);
	            result.put(key, result.getOrDefault(key, 0) + sale);
	        }

	    } finally {
	        closeAll(rs, ps, conn);
	    }

	    return result;
	}

	@Override
	public String analzeTendencyByTier(Client c) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		PreparedStatement psTier = null;
		ResultSet rs = null;
		ResultSet rsTier = null;
		StringBuilder result = new StringBuilder();

		char[] tiers = { 'b', 's', 'g' };

		try {
			conn = getConnect();

			// 1. DB에서 distinct tier 값들 먼저 가져오기
			String tierQuery = "SELECT DISTINCT tier FROM client WHERE tier IN ('b', 's', 'g')";
			psTier = conn.prepareStatement(tierQuery);
			rsTier = psTier.executeQuery();

			// 2. 대표 성향 쿼리 준비
			String query = "SELECT " + "CASE c.tier " + "    WHEN 'b' THEN 'bronze' " + "    WHEN 's' THEN 'silver' "
					+ "    WHEN 'g' THEN 'gold' " + "    ELSE '알 수 없음' " + "END AS tier_name, "
					+ "c.mbti, COUNT(*) AS cnt " + "FROM client c " + "JOIN booking b ON c.client_id = b.client_id "
					+ "WHERE c.tier = ? AND c.mbti IN ('E', 'I') " + "GROUP BY tier_name, c.mbti "
					+ "ORDER BY cnt DESC " + "LIMIT 1";

			ps = conn.prepareStatement(query);

			for (char tier : tiers) {
				System.out.println("현재 tier: " + tier);
				ps.setString(1, String.valueOf(tier));
				rs = ps.executeQuery();

				String tierStr = switch (tier) {
				case 'b' -> "bronze";
				case 's' -> "silver";
				case 'g' -> "gold";
				default -> "null";
				};

				if (rs.next()) {
					String mbti = rs.getString("mbti");
					System.out.println("쿼리 결과 mbti: " + mbti);
					result.append(String.format("%s 등급의 대표 성향: %s\n", tierStr, mbti));
				} else {
					System.out.println("쿼리 결과 없음");
					result.append(String.format("%s 등급의 대표 성향: 알 수 없음\n", tierStr));
				}

				rs.close();
			}
		} finally {
			closeAll(rsTier, psTier, null);
			closeAll(null, ps, conn);
		}

		return result.toString();
	}

	@Override
	public Guesthouse getMostBookedGH(LocalDate checkIn, LocalDate checkOut) throws SQLException {
		Guesthouse gh = null;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getConnect();
			String query = "SELECT gh_name, COUNT(*) " + "FROM booking_detail " + "WHERE booking_date BETWEEN ? AND ? "
					+ "GROUP BY gh_name " + "ORDER BY COUNT(*) DESC " + "LIMIT 1";
			ps = conn.prepareStatement(query);
			ps.setString(1, checkIn.toString());
			ps.setString(2, checkOut.toString());
			rs = ps.executeQuery();
			String ghName = null;
			if (rs.next()) {
				ghName = rs.getString("gh_name");
			} else {
				System.out.println("등록된 예약이 없습니다.");
				return null;
			}
			conn = getConnect();
			query = "SELECT * FROM guesthouse WHERE gh_name = ?";
			ps = conn.prepareStatement(query);
			ps.setString(1, ghName);
			rs = ps.executeQuery();
			if (rs.next()) {
				gh = new Guesthouse(rs.getString("gh_name"), rs.getString("mbti").charAt(0), rs.getInt("price_weekday"),
						rs.getInt("price_weekend"), rs.getInt("max_capacity"));
			} else {
				System.out.println("잘못된 입력입니다.");
			}
		} finally {
			closeAll(rs, ps, conn);
		}
		return gh;
	}

	@Override
	public double calAverageVisitInterval(Client client) throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		double totalInterval = 0;
		int count = 0;
		try {
			conn = getConnect();
			String query = "SELECT booking_id, check_in, "
					+ "DATEDIFF(check_in, LAG(check_in) OVER (PARTITION BY client_id ORDER BY check_in)) AS visit_interval "
					+ "FROM booking " + "WHERE client_id = ? " + "ORDER BY check_in";
			ps = conn.prepareStatement(query);
			ps.setString(1, client.getId());
			rs = ps.executeQuery();
			while (rs.next()) {
				int interval = rs.getInt("visit_interval");
				if (!rs.wasNull()) {
					totalInterval += interval;
					count++;
				}
			}
		} catch (SQLException e) {
			e.getMessage();
		} finally {
			closeAll(rs, ps, conn);
		}
		return count > 0 ? totalInterval/count : 0;
	}

	@Override
	public Map<String, Double> calAverageStayByTier() throws DMLException, SQLException {
		Map<String, Double> result = new LinkedHashMap<>();

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getConnect();

			String query = "SELECT " + "CASE c.tier " + "    WHEN 'b' THEN 'bronze' " + "    WHEN 's' THEN 'silver' "
					+ "    WHEN 'g' THEN 'gold' " + "    ELSE '알 수 없음' " + "END AS tier_name, "
					+ "AVG(b.nights) AS avg_nights " + // 숙박 일수 평균
					"FROM client c " + "JOIN booking b ON c.client_id = b.client_id " + "GROUP BY c.tier "
					+ "ORDER BY avg_nights DESC";
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				String tierName = rs.getString("tier_name");
				double avgNights = rs.getDouble("avg_nights");
				result.put(tierName, avgNights);
			}

		} catch (SQLException e) {
			throw new DMLException("등급별 숙박 일수 평균을 구하는 중 오류 발생: " + e.getMessage());
		} finally {
			closeAll(rs, ps, conn);
		}

		return result;
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

	private HashMap<String, ArrayList<Booking>> groupedBookingsByClient() throws SQLException {
		HashMap<String, ArrayList<Booking>> map = new HashMap<String, ArrayList<Booking>>();
		ArrayList<Booking> allBookings = getAllBookings();
		if (allBookings.size() == 0)
			return map;
		String clientId = null;
		for (Booking b : allBookings) {
			if (!b.getClientId().equals(clientId)) {
				clientId = b.getClientId();
				map.put(clientId, new ArrayList<Booking>());
			}
			map.get(clientId).add(b);
		}
		return map;
	}

	@Override
	public ArrayList<Client> getAllClients() throws SQLException {
		ArrayList<Client> clients = new ArrayList<Client>();
		HashMap<String, ArrayList<Booking>> map = groupedBookingsByClient();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getConnect();
			String query = "SELECT client_id, client_password, client_name, ifnull(mbti, 'X') mbti, ifnull(tier, 'U') tier FROM client";
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			while (rs.next()) {
				String clientId = rs.getString("client_id");
				clients.add(new Client(clientId, rs.getString("client_password"), rs.getString("client_name"),
						rs.getString("mbti").charAt(0), rs.getString("tier").charAt(0), map.get(clientId)));
			}
		} finally {
			closeAll(rs, ps, conn);
		}
		return clients;
	}

	public ArrayList<Booking> getBookings(String clientId) throws SQLException {
		ArrayList<Booking> bookings = new ArrayList<Booking>();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = getConnect();
			String query = "SELECT * FROM booking WHERE client_id=?";
			ps = conn.prepareStatement(query);
			ps.setString(1, clientId);
			rs = ps.executeQuery();
			while (rs.next()) {
				bookings.add(
						new Booking(rs.getString("booking_id"), clientId, rs.getString("gh_name"), rs.getInt("people"),
								rs.getDate("check_in").toLocalDate(), rs.getInt("nights"), rs.getInt("total_price")));
			}
		} finally {
			closeAll(rs, ps, conn);
		}
		return bookings;
	}

	@Override
	public ArrayList<Booking> getAllBookings() throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Booking> list = new ArrayList<Booking>();

		try {
			conn = getConnect();
			String query = "SELECT booking_id, client_id, people, " + "check_in, nights,  total_price,  gh_name "
					+ "FROM booking " + "ORDER BY client_id";
			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();
			while (rs.next()) {
				list.add(new Booking(rs.getString("booking_id"), rs.getString("client_id"), rs.getString("gh_name"),
						rs.getInt("people"), rs.getDate("check_in").toLocalDate(), rs.getInt("nights"),
						rs.getInt("total_price")));
			}
		} finally {
			closeAll(rs, ps, conn);
		}
		return list;
	}
}
