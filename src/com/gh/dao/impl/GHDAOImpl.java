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
import com.gh.exception.BookCancelledException;
import com.gh.exception.DMLException;
import com.gh.exception.RecordNotFoundException;
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
		int count = 0;
		char newTier = 'U';
		String query = "SELECT COUNT(*) AS cnt\r\n" + "FROM booking b\r\n"
				+ "JOIN booking_detail bd ON b.gh_name = bd.gh_name AND b.check_in = bd.booking_date\r\n"
				+ "WHERE b.client_id = ? AND bd.booking_status = 'S';";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query);) {
			// 방문 횟수 계산
			ps.setString(1, c.getId());
			try (ResultSet rs = ps.executeQuery();) {
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
				// 객체에 티어 적용
				c.setTier(newTier);
			}
			// DB에 티어 업데이트 
			String updateQuery = "UPDATE client SET tier = ? WHERE client_id =?";
				try (PreparedStatement ps2 = conn.prepareStatement(updateQuery)) {
					ps2.setString(1, String.valueOf(newTier));
					ps2.setString(2, c.getId());
					ps2.executeUpdate();
					System.out.println("티어 계산 및 DB 반영 완료 ! -> 현재 티어 : " + newTier);
			}
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
		String query = "SELECT t.booking_date, SUM(t.people) AS reserved, g.max_capacity " + "FROM ( "
				+ "   SELECT DISTINCT b.booking_id, b.people, bd.booking_date " + "   FROM booking b "
				+ "   JOIN booking_detail bd ON b.gh_name = bd.gh_name "
				+ "     AND bd.booking_date BETWEEN b.check_in AND DATE_ADD(b.check_in, INTERVAL b.nights - 1 DAY) "
				+ "   WHERE b.gh_name = ? " + "     AND bd.booking_status IN ('R', 'S') "
				+ "     AND bd.booking_date BETWEEN ? AND DATE_ADD(?, INTERVAL ? - 1 DAY) " + ") t "
				+ "JOIN guesthouse g ON g.gh_name = ? " + "GROUP BY t.booking_date";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query);) {
			ps.setString(1, ghName);
			ps.setDate(2, Date.valueOf(checkIn));
			ps.setDate(3, Date.valueOf(checkIn));
			ps.setInt(4, nights);
			ps.setString(5, ghName);
			try (ResultSet rs = ps.executeQuery();) {
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
					try (PreparedStatement ps2 = conn.prepareStatement(findQuery);) {
						ps2.setString(1, ghName);
						try (ResultSet rs2 = ps2.executeQuery();) {
							if (rs2.next()) {
								int maxCapacity = rs2.getInt("max_capacity");
								return people <= maxCapacity;
							} else {
								// 게스트하우스 이름이 존재하지 않을 경우
								System.out.println("[" + ghName + " 은/는 존재하지 않는 게스트하우스입니다]");
								return false;
							}
						}
					}
				}
			}
			// 예약 가능
			return true;
		}
	}

	public int getDayBetweenBooking(String clientId) throws RecordNotFoundException {
		Date beforeDate = null;
		String query = "SELECT max(b.check_in) AS bdate FROM booking b "
				+ "JOIN booking_detail bd ON b.booking_id = bd.booking_id "
				+ "WHERE client_id = ? AND bd.booking_status = 'S'";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, clientId);
			try(ResultSet rs = ps.executeQuery();) {
				if (rs.next())
					beforeDate = rs.getDate("bdate");
			}
		} catch(SQLException e) {
			throw new RecordNotFoundException();
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
	public boolean login(String id, String password, String type) throws RecordNotFoundException {
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
	        return false;
	    }

	    try (Connection conn = getConnect(); PreparedStatement ps = conn.prepareStatement(query)) {
	        ps.setString(1, id);

	        try (ResultSet rs = ps.executeQuery()) {
	            if (rs.next()) {
	                String storedHash = rs.getString(column);
	                String inputHash = PasswordUtil.encrypt(password);

	                if (storedHash.equalsIgnoreCase(inputHash)) {
	                    System.out.println(id + "님 로그인 성공 ! ");
	                    return true;
	                } else {
	                    System.out.println("비밀번호가 일치하지 않습니다.");
	                    return false;
	                }
	            } else {
	                System.out.println("해당 ID는 존재하지 않습니다.");
	                return false;
	            }
	        }
	    } catch (SQLException e) {
	        throw new RecordNotFoundException("로그인 중 오류 발생: " + e.getMessage());
	    }
	}

	
	public ArrayList<Booking> getBookingsByClientId(String clientId) throws RecordNotFoundException {
	    ArrayList<Booking> list = new ArrayList<>();
	    String sql = "SELECT * FROM booking WHERE client_id = ?";
	    try (Connection conn = getConnect();
	    		PreparedStatement ps = conn.prepareStatement(sql);) {
	        ps.setString(1, clientId);
	        try (ResultSet rs = ps.executeQuery();) {
		        while (rs.next()) {
		            String bookingId = rs.getString("booking_id");
		            String ghName = rs.getString("gh_name"); // 예시: guesthouse 이름
		            LocalDate checkIn = rs.getDate("check_in").toLocalDate();
		            int nights = rs.getInt("nights");
		            int people = rs.getInt("people");
		            int totalPrice = rs.getInt("total_price");
		            
		            // Booking 객체 생성자에 맞춰 수정하세요
		            Booking booking = new Booking(bookingId, clientId, ghName, people, checkIn, nights, totalPrice);
		            list.add(booking);
		        }
	        }

	    }catch (SQLException e) {
	    	throw new RecordNotFoundException();
	    }
	    return list;
	}

	@Override
	public Client login(String id, String password) throws RecordNotFoundException{
	    String query = "SELECT * FROM client WHERE client_id=?";
	    try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query);) {
	        ps.setString(1, id);
	        try (ResultSet rs = ps.executeQuery();) {
		        if (rs.next()) {
		            String storedHash = rs.getString("client_password");
		            String inputHash = PasswordUtil.encrypt(password);
		            if (storedHash.equalsIgnoreCase(inputHash)) {
		                System.out.println(id + "님 로그인 성공!");
		                // 예약 정보 가져오기
		                ArrayList<Booking> bookings = getBookingsByClientId(id);
		                return new Client(
		                    rs.getString("client_id"),                
		                    storedHash,                               
		                    rs.getString("client_name"),             
		                    rs.getString("mbti") != null ? rs.getString("mbti").charAt(0) : '\0',
		                    rs.getString("tier") != null ? rs.getString("tier").charAt(0) : null, 
		                    bookings                                 
		                );
		            } else {
		                System.out.println("비밀번호가 일치하지 않습니다.");
		            }
		            
		        } else {
		            System.out.println("해당 ID는 존재하지 않습니다.");
		        }
	        }
	    } catch (SQLException e) {
	    	throw new RecordNotFoundException("가입되지 않은 사용자입니다.");
	    }
	    return null;
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
	public Guesthouse getGuesthouse(String ghName) throws RecordNotFoundException {
		Guesthouse gh = null;
		
		String query = "SELECT gh_name, mbti, price_weekday, price_weekend, max_capacity from guesthouse WHERE gh_name=?";
		try (Connection conn = getConnect(); PreparedStatement ps = conn.prepareStatement(query);) {
			ps.setString(1, ghName);
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					gh = new Guesthouse(rs.getString("gh_name"),
										rs.getString("mbti").charAt(0),
										rs.getInt("price_weekday"),
										rs.getInt("price_weekend"),
										rs.getInt("max_capacity"));
				} else {
					System.out.println("찾으시는 게스트 하우스가 없습니다.");
				}
			}
		}catch(SQLException e) {
			throw new RecordNotFoundException("올바른 게스트하우스 이름이 아닙니다.");
		}
		return gh;
	}
	
	@Override
	public void reserveBooking(Client client, Booking booking) throws BookCancelledException, RecordNotFoundException {
		String query = "INSERT INTO booking VALUES (?, ?, ?, ?, ?, ?, ?)";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query);) {
			String uuid = UUID.randomUUID().toString();
			booking.setBookingId(uuid);
			if (canBook(booking.getGhName(),booking.getCheckInDate(), booking.getNights(),
					booking.getPeopleCnt())) {
				Guesthouse gh = getGuesthouse(booking.getGhName());
				if (gh == null) {
					System.out.println("찾으시는 게스트하우스는 존재하지 않습니다.");
					return ;
				}
				int totalPrice = calcTotalPrice(booking.getCheckInDate(), booking.getNights(),
												gh.getPriceWeekday(), gh.getPriceWeekend());
				double discount = calcDiscountByTier(client);
				ps.setString(1, uuid);
				ps.setString(2, client.getId());
				ps.setString(3, booking.getGhName());
				ps.setInt(4, booking.getPeopleCnt());
				ps.setString(5, booking.getCheckInDate().toString());
				ps.setInt(6, booking.getNights());
				ps.setInt(7, (int)(totalPrice * (1 - discount)));
				ps.executeUpdate();
				query = "INSERT INTO booking_detail (gh_name, booking_date, booking_status, booking_id) VALUES (?, ?, ?, ?)";
				try (PreparedStatement ps2 = conn.prepareStatement(query);) {
					ps2.setString(1, booking.getGhName());
					ps2.setString(2, booking.getCheckInDate().toString());
					ps2.setString(3, "R");
					ps2.setString(4, uuid);
					System.out.println(ps2.executeUpdate() + "개 예약 완료되었습니다.");
				}
			} else {
				System.out.println("예약할 수 없습니다.");
			}
		}catch (SQLException e) {
			throw new BookCancelledException("올바른 예약이 아닙니다.");
		}
	}

	private boolean checkBookingStatus(String bookingId) throws RecordNotFoundException {
		String query = "SELECT booking_status FROM booking_detail WHERE booking_id=?";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query);) {
			ps.setString(1, bookingId);
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					if (rs.getString("booking_status").equals("R"))
						return true;
					else
						return false;
				}else
					return false;
			}
		}catch (SQLException e) {
			throw new RecordNotFoundException("예약번호가 올바르지 않습니다.");
		}
	}

	private String checkId(String bookingId) throws RecordNotFoundException {
		ResultSet rs = null;
		String query = "SELECT client_id FROM booking WHERE booking_id=?";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query);) {
			ps.setString(1, bookingId);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getString("client_id");
			} else
				return null;
		}catch (SQLException e) {
			throw new RecordNotFoundException("올바른 사용자가 아닙니다.");
		}
	}

	@Override
	public ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt) throws RecordNotFoundException {
		ArrayList<Guesthouse> availableList = new ArrayList<>();
		String query = "SELECT * FROM guesthouse";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query);) {
			try (ResultSet rs = ps.executeQuery();) {
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
			}
		}catch(SQLException e) {
			throw new RecordNotFoundException("찾으시는 게스트하우스가 없습니다.");
		}
		return availableList;
	}

	@Override
	public ArrayList<Guesthouse> searchAvailableGH(LocalDate checkIn, int night, int peopleCnt, int price, char mbti)
			throws RecordNotFoundException {
		ArrayList<Guesthouse> availableList = new ArrayList<>();
		String query = "SELECT g.*, " + "  IFNULL(cancel_count, 0) / IFNULL(total_count, 1) AS cancel_rate "
				+ "FROM guesthouse g " + "LEFT JOIN ( " + "    SELECT gh_name, "
				+ "           SUM(CASE WHEN booking_status = 'C' THEN 1 ELSE 0 END) AS cancel_count, "
				+ "           COUNT(*) AS total_count " + "    FROM booking_detail " + "    GROUP BY gh_name "
				+ ") stats ON g.gh_name = stats.gh_name " + "WHERE g.price_weekday <= ? AND g.price_weekend <= ? "
				+ "ORDER BY " + "  CASE WHEN ? = 'E' AND g.mbti = 'E' THEN 0 "
				+ "       WHEN ? = 'I' AND g.mbti = 'I' THEN 0 ELSE 1 END, " + "  cancel_rate ASC";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query);) {	
			ps.setInt(1, price);
			ps.setInt(2, price);
			ps.setString(3, String.valueOf(mbti));
			ps.setString(4, String.valueOf(mbti));
			try(ResultSet rs = ps.executeQuery();){
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
			}
		}catch(SQLException e) {
			throw new RecordNotFoundException("찾으시는 게스트하우스가 없습니다.");
		}
		return availableList;
	}

	@Override
	public void updateBooking(Client client, Booking booking) throws RecordNotFoundException {
		if (client.getId().equals(checkId(booking.getBookingId()))) {
			String query = "UPDATE booking SET check_in=?, people=?, nights=?, total_price=? WHERE booking_id=? AND client_id=?";
			try (Connection conn = getConnect();PreparedStatement 	ps = conn.prepareStatement(query);) {
				if (canBook(booking.getGhName(), booking.getCheckInDate(), booking.getNights(), booking.getPeopleCnt())) {
					Guesthouse gh = getGuesthouse(booking.getGhName());
					int totalPrice = calcTotalPrice(booking.getCheckInDate(), booking.getNights(),
							gh.getPriceWeekday(), gh.getPriceWeekend());
					double discount = calcDiscountByTier(client);
					ps.setString(1, booking.getCheckInDate().toString());
					ps.setInt(2, booking.getPeopleCnt());
					ps.setInt(3, booking.getNights());
					ps.setInt(4, (int)(totalPrice * (1 - discount)));
					ps.setString(5, booking.getBookingId());
					ps.setString(6, client.getId());
					if (ps.executeUpdate() == 1) {
						query = "UPDATE booking_detail SET booking_date=? WHERE booking_id =?";
						try (PreparedStatement ps2 = conn.prepareStatement(query);){
							ps2.setString(1, booking.getCheckInDate().toString());
							ps2.setString(2, booking.getBookingId());
							System.out.println(ps2.executeUpdate() + "개 예약이 변경되었습니다.");
						}
					} else 
						System.out.println("변경하실 수 없습니다.");
				} else {
					System.out.println("변경할 수 없는 날짜입니다.");
				}
			}catch(SQLException e) {
				throw new RecordNotFoundException("없는예약 입니다.");
			}
		}else
			System.out.println("잘못된 사용자입니다.");
	}

	@Override
	public void cancelBooking(Client client, String bookingId) throws RecordNotFoundException {
		
		if (client.getId().equals(checkId(bookingId))) {
			if (checkBookingStatus(bookingId)) {
				String query = "UPDATE booking_detail SET booking_status=? WHERE booking_id=?";
				try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query);) {
					ps.setString(1, "C");
					ps.setString(2, bookingId);
					if(ps.executeUpdate() == 1) {
						query = "UPDATE booking SET total_price=0 WHERE booking_id=?";
						try (PreparedStatement ps2 = conn.prepareStatement(query);) {
							ps2.setString(1, bookingId);
							ps2.executeUpdate();
							System.out.println("예약이 취소되었습니다.");
						}
					} else {
						System.out.println("잘못된 입력입니다.");
					}
				}catch (SQLException e) {
					throw new RecordNotFoundException("찾으시는 예약번호가 없습니다.");
				}
			}
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
	public void printMyInfo(Client c) throws RecordNotFoundException {
		if (c == null) {
			System.out.println("고객 정보가 없습니다.");
			return;
		}
		String bookingCount = "SELECT COUNT(*) FROM booking WHERE client_id=?";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(bookingCount);) {
			// 1) 고객 등급 조회
			System.out.println("=== 내 정보 조회 ===");
			System.out.println("고객명: " + c.getName());
			System.out.println("고객 등급: " + c.getTier());
			// 2) 예약 횟수 조회 (전체 예약 횟수)
			ps.setString(1, c.getId());
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					System.out.println("고객의 총 예약 횟수: " + rs.getInt(1));
				}
			}
				// 예약 및 예약 상세 내역 조회 (조인 활용)
				// 조인 없이 사용을 한다면 데이터베이스에 쿼리 호출 및 횟수가 늘어나므로 성능상 주의가 필요
			String query = "SELECT b.booking_id, b.people, b.check_in, b.nights, b.total_price, "
					+ "bd.booking_detail_id, bd.gh_name, bd.booking_date, bd.booking_status " + "FROM booking b "
					+ "LEFT JOIN booking_detail bd ON b.booking_id = bd.booking_id " + "WHERE b.client_id = ? "
					+ "ORDER BY bd.booking_date DESC";
			try (PreparedStatement ps2 = conn.prepareStatement(query);) {
				ps2.setString(1, c.getId());
				try (ResultSet rs2 = ps2.executeQuery();) {
					StringBuilder checkedIn = new StringBuilder();
					StringBuilder upcoming = new StringBuilder();
					StringBuilder canceled = new StringBuilder();
					while (rs2.next()) {
						// Booking 정보
						String bookingId = rs2.getString("booking_id");
						int people = rs2.getInt("people");
						String checkIn = rs2.getString("check_in");
						int nights = rs2.getInt("nights");
						int totalPrice = rs2.getInt("total_price");
						// BookingDetail 정보
						String bookingDetailId = rs2.getString("booking_detail_id");
						String ghName = rs2.getString("gh_name");
						String bookingDate = rs2.getString("booking_date");
						String bookingStatus = rs2.getString("booking_status");
						String info = String.format("예약ID: %s, 인원: %d, 체크인: %s, 숙박일수: %d, 총금액: %d, 게스트하우스: %s, 예약일: %s\n",
								bookingId, people, checkIn, nights, totalPrice, ghName != null ? ghName : "-",
								bookingDate != null ? bookingDate : "-");
						if (bookingStatus == null)
							bookingStatus = "알 수 없음";
						switch (bookingStatus) {
							case "S":
								checkedIn.append(info);
								break;
							case "R":
								upcoming.append(info);
								break;
							case "C":
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
				}
			}
		} catch (SQLException e) {
			throw new RecordNotFoundException("DB 오류: " + e.getMessage());
		}
	}
	
	public Map<String, Integer> getWeeklyVisitorCount(LocalDate checkIn, LocalDate checkOut) throws RecordNotFoundException {
		Map<String, Integer> result = new LinkedHashMap<>();
		String query = 
	            "SELECT " +
	            " check_in, " +
	            " COUNT(client_id) OVER (PARTITION BY YEAR(check_in), MONTH(check_in), WEEK(check_in, 1)) AS visitor_count " +
	            "FROM booking " +
	            "WHERE check_in BETWEEN ? AND ? " +
	            "ORDER BY check_in";
		try (Connection conn = getConnect();PreparedStatement ps = conn.prepareStatement(query)) {
			// 주별 방문 고객 수 집계
			ps.setDate(1, java.sql.Date.valueOf(checkIn));
			ps.setDate(2, java.sql.Date.valueOf(checkOut));
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					LocalDate visitDate = rs.getDate("check_in").toLocalDate();
		            int year = visitDate.getYear();
		            int month = visitDate.getMonthValue();
		            int relativeWeek = (int) ChronoUnit.WEEKS.between(checkIn, visitDate) + 1;
		            int count = rs.getInt("visitor_count");
	
		            String key = String.format("%d년 %02d월 %d주차", year, month, relativeWeek);
		            result.put(key, count);
				}
			}
		}catch (SQLException e) {
			throw new RecordNotFoundException("찾으시는 예약이 없습니다.");
		}
		return result;
	}

	// 주별 매출 집계
	@Override
	public Map<String, Integer> getWeeklySales(LocalDate checkIn, LocalDate checkOut) throws RecordNotFoundException, DMLException {
		Map<String, Integer> result = new LinkedHashMap<>();
		// 1. gh 테이블에서 요금 정보 가져오기
	    String ghQuery = "SELECT gh_name, price_weekday, price_weekend FROM guesthouse";
	    try (Connection conn = getConnect();
	    		PreparedStatement ps = conn.prepareStatement(ghQuery);
	    		ResultSet rs = ps.executeQuery();) {
	        // 데일리 매출을 계산하기 위해서는 평일과 주말 배열로 생성해서 변수로 만들어야 총 합산 계산 가능
	        Map<String, int[]> priceMap = new HashMap<>(); // [0] = 평일, [1] = 주말
	        while (rs.next()) {
	            String ghName = rs.getString("gh_name");
	            int weekdayPrice = rs.getInt("price_weekday");
	            int weekendPrice = rs.getInt("price_weekend");
	            priceMap.put(ghName, new int[]{weekdayPrice, weekendPrice});
	        }
	        // 2. booking에서 예약 정보 가져오기 (예약 취소 제외를 total_price > 0으로 지정)
	        String bookingQuery =
	            "SELECT gh_name, check_in, nights, people, total_price " +
	            "FROM booking " +
	            "WHERE check_in BETWEEN ? AND ? AND total_price > 0";
	        try (PreparedStatement ps2 = conn.prepareStatement(bookingQuery);) {
		        ps2.setDate(1, java.sql.Date.valueOf(checkIn));
		        ps2.setDate(2, java.sql.Date.valueOf(checkOut));
		        try (ResultSet rs2 = ps2.executeQuery();) {
			        
			        
			        // 예약이 하나도 없는 경우 예외 발생
			        if (!rs2.isBeforeFirst() ) {
			        	throw new RecordNotFoundException("지정된 기간에 예약이 없습니다.");
			        }
		
			        while (rs2.next()) {
			            String ghName = rs2.getString("gh_name");
			            LocalDate startDate = rs2.getDate("check_in").toLocalDate();
			            int nights = rs2.getInt("nights");
			            int people = rs2.getInt("people");
		
			            // 평일 과 주말의 매출을 저장하기 위한 변수 생성
			            int[] prices = priceMap.getOrDefault(ghName, new int[]{0, 0});
			            int weekdayPrice = prices[0];
			            int weekendPrice = prices[1];
		
			            for (int i = 0; i < nights; i++) {
			                LocalDate currentDate = startDate.plusDays(i);
			                int year = currentDate.getYear();
			                int month = currentDate.getMonthValue();
			                int relativeWeek = (int) ChronoUnit.WEEKS.between(checkIn, currentDate) + 1;
		
			                // 토요일과 일요일이 주말인 것을 자동 계산해서 데일리 계산에 적용
			                DayOfWeek day = currentDate.getDayOfWeek();
			                boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
		
			                int dailyPrice = isWeekend ? weekendPrice : weekdayPrice;
			                int dailySale = dailyPrice * people;
		
			                String key = String.format("%d년 %02d월 %d주차", year, month, relativeWeek);
			                result.put(key, result.getOrDefault(key, 0) + dailySale);
			            }
			        }
		        }
	        }
	    } catch (SQLException e) {
	    	throw new DMLException("데이터를 가져오지 못했습니다.");
	    }
	    return result;
	}

	@Override
	public String analzeTendencyByTier() throws DMLException {
		StringBuilder result = new StringBuilder();

		char[] tiers = { 'b', 's', 'g' };
		// 1. DB에서 distinct tier 값들 먼저 가져오기
		String tierQuery = "SELECT DISTINCT tier FROM client WHERE tier IN ('b', 's', 'g')";
		try (Connection conn = getConnect();
				PreparedStatement psTier = conn.prepareStatement(tierQuery);
				ResultSet rsTier = psTier.executeQuery();) {
			// 2. 대표 성향 쿼리 준비
			String query = "SELECT " + "CASE c.tier " + "    WHEN 'b' THEN 'bronze' " + "    WHEN 's' THEN 'silver' "
					+ "    WHEN 'g' THEN 'gold' " + "    ELSE '알 수 없음' " + "END AS tier_name, "
					+ "c.mbti, COUNT(*) AS cnt " + "FROM client c " + "JOIN booking b ON c.client_id = b.client_id "
					+ "WHERE c.tier = ? AND c.mbti IN ('E', 'I') " + "GROUP BY tier_name, c.mbti "
					+ "ORDER BY cnt DESC " + "LIMIT 1";
			try (PreparedStatement ps = conn.prepareStatement(query);) {
				for (char tier : tiers) {
					System.out.println("현재 tier: " + tier);
					ps.setString(1, String.valueOf(tier));
					try (ResultSet rs = ps.executeQuery();) {
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
					}
				}
			}
		}catch (SQLException e) {
			throw new DMLException("데이터를 가져오지 못했습니다.");
		}
		return result.toString();
	}

	@Override
	public Guesthouse getMostBookedGH(LocalDate checkIn, LocalDate checkOut) throws RecordNotFoundException {
		Guesthouse gh = null;
		String ghName = null;
		String mostBookedQuery = "SELECT gh_name, COUNT(*) AS cnt " + "FROM booking_detail "
				+ "WHERE booking_date BETWEEN ? AND ? " + "GROUP BY gh_name " + "ORDER BY cnt DESC " + "LIMIT 1";
		try (Connection conn = getConnect(); PreparedStatement ps1 = conn.prepareStatement(mostBookedQuery)) {
			ps1.setString(1, checkIn.toString());
			ps1.setString(2, checkOut.toString());
			try (ResultSet rs1 = ps1.executeQuery()) {
				if (rs1.next()) {
					ghName = rs1.getString("gh_name");
				} else {
					// 예약이 없는 경우
					throw new RecordNotFoundException("해당 기간에 예약이 없습니다.");
				}
			}
			String guesthouseQuery = "SELECT * FROM guesthouse WHERE gh_name = ?";
			try (PreparedStatement ps2 = conn.prepareStatement(guesthouseQuery)) {
				ps2.setString(1, ghName);
				try (ResultSet rs2 = ps2.executeQuery()) {
					if (rs2.next()) {
						gh = new Guesthouse(rs2.getString("gh_name"), rs2.getString("mbti").charAt(0),
								rs2.getInt("price_weekday"), rs2.getInt("price_weekend"), rs2.getInt("max_capacity"));
					} else {
						throw new RecordNotFoundException("Guesthouse 정보가 존재하지 않습니다.");
					}
				}
			}
		} catch (SQLException e) {
			throw new RecordNotFoundException("DB 오류가 발생했습니다: " + e.getMessage());
		}
		return gh;
	}

	@Override
	public double calcAverageVisitInterval(String clientId) throws RecordNotFoundException {
		double totalInterval = 0;
		int count = 0;
		String query = "SELECT booking_id, check_in, "
				+ "DATEDIFF(check_in, LAG(check_in) OVER (PARTITION BY client_id ORDER BY check_in)) AS visit_interval "
				+ "FROM booking " + "WHERE client_id = ? " + "ORDER BY check_in";
		try (Connection conn = getConnect();
		PreparedStatement ps = conn.prepareStatement(query);) {
			ps.setString(1, clientId);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					int interval = rs.getInt("visit_interval");
					if (!rs.wasNull()) {
						totalInterval += interval;
						count++;
					}
				}
			}
		} catch (SQLException e) {
			throw new RecordNotFoundException("올바른 사용자가 아닙니다.");
		}
		return count > 0 ? totalInterval/count : 0;
	}

	@Override
	public Map<String, Double> calAverageStayByTier() throws DMLException {
		Map<String, Double> result = new LinkedHashMap<>();
		String query = "SELECT " + "CASE c.tier " + "    WHEN 'b' THEN 'bronze' " + "    WHEN 's' THEN 'silver' "
						+ "    WHEN 'g' THEN 'gold' " + "    ELSE '알 수 없음' " + "END AS tier_name, "
						+ "AVG(b.nights) AS avg_nights " + // 숙박 일수 평균
						"FROM client c " + "JOIN booking b ON c.client_id = b.client_id " + "GROUP BY c.tier "
						+ "ORDER BY avg_nights DESC";
		try(Connection conn = getConnect(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery();) {
			while (rs.next()) {
				String tierName = rs.getString("tier_name");
				double avgNights = rs.getDouble("avg_nights");
				result.put(tierName, avgNights);
			}
		} catch (SQLException e) {
			throw new DMLException("등급별 숙박 일수 평균을 구하는 중 오류 발생: " + e.getMessage());
		} 
		return result;
	}

	@Override
	public double calCancelRate() throws RecordNotFoundException {
		
		double cancellationRate = 0.0;
		String totalQuery = "SELECT COUNT(*) FROM booking_detail";
		try (Connection conn = getConnect();
				PreparedStatement ps = conn.prepareStatement(totalQuery);
				ResultSet rs = ps.executeQuery();) {
			// 1️. 전체 예약 수
			int totalBookings = 0;
			if (rs.next()) {
				totalBookings = rs.getInt(1);
			}
			if (totalBookings == 0) {
				// 예약이 없는 경우 취소율은 0
				return 0.0;
			}
			// 2️.취소된 예약 수
			String canceledQuery = "SELECT COUNT(*) FROM booking_detail WHERE booking_status='C'";
			try (PreparedStatement ps2 = conn.prepareStatement(canceledQuery);ResultSet rs2 = ps2.executeQuery();) {
				int canceledBookings = 0;
				if (rs2.next()) {
					canceledBookings = rs2.getInt(1);
				}
				// 3️. 취소율 계산
				cancellationRate = (canceledBookings / (double) totalBookings) * 100.0;
			}
		}catch(SQLException e) {
			throw new RecordNotFoundException("예약을 찾지 못했습니다.");
		}
		return cancellationRate;
	}



	@Override
	public Client getClientById(String id) throws RecordNotFoundException {
		Client cl = null;

		String query = "SELECT client_id, client_password, client_name, mbti, tier FROM client WHERE client_id=?";
		try (Connection conn = getConnect(); PreparedStatement ps = conn.prepareStatement(query);) {
			ps.setString(1, id);
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					String clientId = rs.getString("client_id");
					String mbtiStr = rs.getString("mbti");
					char mbti = (mbtiStr != null && !mbtiStr.isEmpty()) ? mbtiStr.charAt(0) : ' ';
					String tierStr = rs.getString("tier");
					Character tier = (tierStr != null && !tierStr.isEmpty()) ? tierStr.charAt(0) : null;
					cl = new Client(clientId, rs.getString("client_password"), rs.getString("client_name"), mbti, tier,
							getBookings(clientId));
				}
			}
			return cl;
		} catch (SQLException e) {
			throw new RecordNotFoundException("올바른 사용자가 아닙니다.");
		}
	}

	private HashMap<String, ArrayList<Booking>> groupedBookingsByClient() throws RecordNotFoundException {
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
	public ArrayList<Client> getAllClients() throws RecordNotFoundException {
		ArrayList<Client> clients = new ArrayList<Client>();
		HashMap<String, ArrayList<Booking>> map = groupedBookingsByClient();
		String query = "SELECT client_id, client_password, client_name, ifnull(mbti, 'X') mbti, ifnull(tier, 'U') tier FROM client";
		try (Connection conn = getConnect();
				PreparedStatement ps = conn.prepareStatement(query);
				ResultSet rs = ps.executeQuery();) {
			while (rs.next()) {
				String clientId = rs.getString("client_id");
				clients.add(new Client(clientId, rs.getString("client_password"), rs.getString("client_name"),
						rs.getString("mbti").charAt(0), rs.getString("tier").charAt(0), map.get(clientId)));
			}
		} catch (SQLException e) {
			throw new RecordNotFoundException("데이터가 존재하지 않습니다.");
		}
		return clients;
	}

	public ArrayList<Booking> getBookings(String clientId) throws RecordNotFoundException {
		ArrayList<Booking> bookings = new ArrayList<Booking>();
		String query = "SELECT * FROM booking WHERE client_id=? AND total_price<>0";
		try (Connection conn = getConnect(); PreparedStatement ps = conn.prepareStatement(query);) {
			ps.setString(1, clientId);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					bookings.add(new Booking(rs.getString("booking_id"), clientId, rs.getString("gh_name"),
							rs.getInt("people"), rs.getDate("check_in").toLocalDate(), rs.getInt("nights"),
							rs.getInt("total_price")));
				}
				return bookings;
			}
		} catch (SQLException e) {
			throw new RecordNotFoundException("올바른 사용자가 아닙니다.");
		}
	}

	@Override
	public ArrayList<Booking> getAllBookings() throws RecordNotFoundException {
		ArrayList<Booking> list = new ArrayList<Booking>();

		String query = "SELECT booking_id, client_id, people, check_in, nights, total_price, gh_name " +
				"FROM booking " +
				"WHERE total_price<>0 " +
				"ORDER BY client_id";
		try (Connection conn = getConnect();
				PreparedStatement ps = conn.prepareStatement(query);
				ResultSet rs = ps.executeQuery();) {
			while (rs.next()) {
				list.add(new Booking(rs.getString("booking_id"), rs.getString("client_id"), rs.getString("gh_name"),
						rs.getInt("people"), rs.getDate("check_in").toLocalDate(), rs.getInt("nights"),
						rs.getInt("total_price")));
			}
		} catch (SQLException e) {
			throw new RecordNotFoundException("예약을 찾지 못했습니다.");
		}
		return list;
	}
}
