package com.gh.test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.gh.dao.impl.GHDAOImpl;
import com.gh.vo.Booking;
import com.gh.vo.Client;
import com.gh.vo.Guesthouse;

public class GHDAOTest {

	public static void main(String[] args) {

		GHDAOImpl dao = GHDAOImpl.getInstance();
		Scanner sc = new Scanner(System.in);

		// 해당 메소드 외부에 선언 또는 main() 위쪽에 선언
		LocalDate checkIn = LocalDate.of(2025, 6, 5);
		int nights = 2;
		int peopleCnt = 2;
		int maxPrice = 8000;
		char mbti = 'E';

		Runnable simpleTask = () -> {
			try {
				List<Guesthouse> list = dao.searchAvailableGH(checkIn, nights, peopleCnt);
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초");
				System.out.println("=== [전체 조회 결과 - " + LocalDateTime.now().format(formatter) + "] ===");
				if (list.isEmpty()) {
					System.out.println("예약 가능한 게스트하우스가 없습니다.");
				} else {
					for (Guesthouse gh : list) {
						System.out.printf("이름: %s, MBTI: %s, 주중가격: %,d, 주말가격: %,d, 최대수용: %d\n", gh.getName(),
								gh.getMbti() != null ? gh.getMbti() : "없음", gh.getPriceWeekday(), gh.getPriceWeekend(),
								gh.getMaxCapacity());
					}
				}
				System.out.println();
			} catch (Exception e) {
				if (!Thread.currentThread().isInterrupted()) {
					System.out.println("조회 중 오류 발생: " + e.getMessage());
					e.printStackTrace();
				}
			}
		};

		Runnable conditionTask = () -> {
			try {
				List<Guesthouse> list = dao.searchAvailableGH(checkIn, nights, peopleCnt, maxPrice, mbti);
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초");
				System.out.println("=== [조건 조회 결과 - " + LocalDateTime.now().format(formatter) + "] ===");
				if (list.isEmpty()) {
					System.out.println("예약 가능한 게스트하우스가 없습니다.");
				} else {
					for (Guesthouse gh : list) {
						System.out.printf("이름: %s, MBTI: %s, 주중가격: %,d, 주말가격: %,d, 최대수용: %d\n", gh.getName(),
								gh.getMbti() != null ? gh.getMbti() : "없음", gh.getPriceWeekday(), gh.getPriceWeekend(),
								gh.getMaxCapacity());
					}
				}
				System.out.println();
			} catch (Exception e) {
				if (!Thread.currentThread().isInterrupted()) {
					System.out.println("조회 중 오류 발생: " + e.getMessage());
					e.printStackTrace();
				}
			}
		};

//        // 사용자 더미 데이터 추가
//        try {
//            InsertDummyData inserter = new InsertDummyData();
//            inserter.insertDummyClients(); // 더미 데이터 삽입 실행
//        } catch (Exception e) {
//            System.out.println("데이터 삽입 중 오류 발생: " + e.getMessage());
//            e.printStackTrace();
//        }
//        
//        // 매니저 더미 데이터 추가
//        try {
//          InsertDummyData inserter = new InsertDummyData();
//          inserter.insertDummyManagers(); // 더미 데이터 삽입 실행
//      } catch (Exception e) {
//          System.out.println("데이터 삽입 중 오류 발생: " + e.getMessage());
//          e.printStackTrace();
//      }

		while (true) {
			System.out.println("=========== 로그인 메뉴 ===========");
			System.out.println("1. 사용자 로그인  2. 관리자 로그인  3. 종료");
			System.out.print("선택 : ");
			String choice = sc.nextLine();

			if (choice.equals("3")) {
				System.out.println("프로그램을 종료합니다.");
				break;
			}

			String type = null;

			if (choice.equals("1")) {
				type = "client";
			} else if (choice.equals("2")) {
				type = "manager";
			} else {
				System.out.println("올바른 메뉴를 선택하세요.");
				continue;
			}

			boolean success = false;
			String userId = null;

			// 로그인한 사용자 정보 !!!!!!!!
			Client c = null;

			while (!success) {
				try {
					System.out.print("아이디 입력: ");
					userId = sc.nextLine();

					System.out.print("비밀번호 입력: ");
					String password = sc.nextLine();

					if (type.equals("client")) {
						// Client용 로그인 처리
						c = dao.login(userId, password); // 로그인 성공 시 Client 객체 반환
						if (c != null) {
							success = true;
						}
					} else if (type.equals("manager")) {
						// Manager용 로그인 처리
						dao.login(userId, password, "manager"); // 반환값 없음
						success = true;
					}

				} catch (SQLException e) {
					System.out.println("로그인 오류: " + e.getMessage());
					e.printStackTrace();
				}
			}

			// 로그인 성공 시 선택 메뉴
			// 유저일 경우 - 유저 관련 메소드
			if ("client".equals(type)) {
				while (true) {
					ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
					ScheduledFuture<?> runningTask = null;

					System.out.println("===== 사용자 메뉴 =====");
					System.out.println("1. 예약하기");
					System.out.println("2. 예약 수정");
					System.out.println("3. 예약 취소");
					System.out.println("4. 예약 가능 게스트하우스 조회");
					System.out.println("5. 예약 가능 게스트하우스 조회");
					System.out.println("6. 내 정보 보기");
					System.out.println("0. 로그아웃");
					System.out.print("선택 : ");
					String clientChoice = sc.nextLine();

					switch (clientChoice) {
					case "1":
						System.out.println("예약하기 호출됨\n");
						Booking booking = new Booking(c.getId(), "힐링하우스", 2, LocalDate.of(2025, 06, 5), 3);

						try {
							dao.reserveBooking(c, booking);
						} catch (SQLException e) {
							System.out.println(e.getMessage());
						}
						break;
					case "2":
						System.out.println("예약 수정 호출됨\n");
						Booking b = new Booking("7bc43af7-8c8a-47a7-9aa0-7b0b7ce7e06e", c.getId(), "힐링하우스", 1,
								LocalDate.of(2025, 06, 5), 3, 17000);
						try {
							dao.updateBooking(c, b);
						} catch (SQLException e) {
							System.out.println(e.getMessage());
						}
						break;
					case "3":
						System.out.println("예약 취소 호출됨\n");

						try {
							dao.cancelBooking(c, "7bc43af7-8c8a-47a7-9aa0-7b0b7ce7e06e");
						} catch (SQLException e) {
							System.out.println(e.getMessage());
						}
						break;
					case "4":
						System.out.println("실시간 전체 조회 시작 (q 입력 시 종료)");
						runningTask = scheduler.scheduleAtFixedRate(simpleTask, 0, 10, TimeUnit.SECONDS);
						waitForQuit(sc, scheduler, runningTask);
						break;
					case "5":
						System.out.println("실시간 조건 조회 시작 (q 입력 시 종료)");
						runningTask = scheduler.scheduleAtFixedRate(conditionTask, 0, 10, TimeUnit.SECONDS);
						waitForQuit(sc, scheduler, runningTask);
						break;
					case "6":
						System.out.println("내 정보 보기 호출됨\n");
						dao.printMyInfo(c);
						break;
					case "0":
						System.out.println("로그아웃합니다.");
						break;
					default:
						System.out.println("올바른 번호를 선택하세요.");

					}

					if (clientChoice.equals("0"))
						break;
				}

			} else if ("manager".equals(type)) {
				while (true) {
					System.out.println("===== 관리자 메뉴 =====");
					System.out.println("1. 주간 방문자 수");
					System.out.println("2. 주간 매출");
					System.out.println("3. 최다 예약 게스트하우스");
					System.out.println("4. 티어별 성향 분석");
					System.out.println("5. 평균 방문 주기");
					System.out.println("6. 티어별 평균 숙박일수");
					System.out.println("7. 취소율 계산");
					System.out.println("8. 아이디로 사용자 조회");
					System.out.println("9. 전체 사용자 조회");
					System.out.println("10. 전체 예약 조회");
					System.out.println("0. 로그아웃");
					System.out.print("선택 : ");
					String managerChoice = sc.nextLine();

					switch (managerChoice) {
					case "1":
						System.out.println("주간 방문자 수 호출됨\n");
						try {
							LocalDate start = LocalDate.of(2025, 6, 1);
							LocalDate end = LocalDate.of(2025, 6, 30);

							Map<String, Integer> weeklyVisitors = dao.getWeeklyVisitorCount(start, end);

							System.out.println("=== 2025년 6월 주별 방문 고객 수 ===");

							if (weeklyVisitors.isEmpty()) {
								System.out.println("방문 기록이 없습니다.\n");
							} else {
								for (Map.Entry<String, Integer> entry : weeklyVisitors.entrySet()) {
									System.out.printf("%s : %d명\n", entry.getKey(), entry.getValue());
								}
							}

						} catch (SQLException e) {
							System.out.println("DB 오류: " + e.getMessage());
							e.printStackTrace();
						}
						break;
					case "2":
						System.out.println("주간 매출 호출됨\n");
						try {
							LocalDate start = LocalDate.of(2025, 06, 01);
							LocalDate end = LocalDate.of(2025, 06, 30);

							Map<String, Integer> weeklySales = dao.getWeeklySales(start, end);

							System.out.println("=== 2025년 6월 주별 매출 ===");

							if (weeklySales.isEmpty()) {
								System.out.println("매출 기록이 없습니다.\n");
							} else {
								for (Map.Entry<String, Integer> entry : weeklySales.entrySet()) {
									System.out.printf("%s: %d원\n", entry.getKey(), entry.getValue());
								}
							}

						} catch (SQLException e) {
							System.out.println(e.getMessage());
						}
						break;
					case "3":
						System.out.println("최다 예약 게스트하우스 호출됨\n");

						try {
							LocalDate start = LocalDate.of(2025, 6, 1);
							LocalDate end = LocalDate.of(2025, 6, 30);

							Guesthouse mostBooked = dao.getMostBookedGH(start, end);

							if (mostBooked != null) {
								System.out.println("=== 2025년 6월 최다 예약 게스트하우스 ===");
								System.out.printf("이름: %s, MBTI: %s, 주중가격: %,d원, 주말가격: %,d원, 최대수용인원: %d명\n\n",
										mostBooked.getName(), mostBooked.getMbti(), mostBooked.getPriceWeekday(),
										mostBooked.getPriceWeekend(), mostBooked.getMaxCapacity());
							}
						} catch (SQLException e) {
							System.out.println("오류: " + e.getMessage());
							e.printStackTrace();
						}
						break;
					case "4":
						System.out.println("티어별 성향 분석 호출됨\n");

						try {
							String analysis = dao.analzeTendencyByTier();
							System.out.println("=== 등급별 대표 성향 ===");
							System.out.println(analysis);
						} catch (SQLException e) {
							System.out.println("오류 발생: " + e.getMessage());
							e.printStackTrace();
						}
						break;
					case "5":
						System.out.println("평균 방문 주기 호출됨\n");

						try {
							double avgInterval = dao.calAverageVisitInterval("user1"); 
							System.out.printf("=== [%s님의 평균 방문 주기] ===\n", "유나");
							if (avgInterval > 0)
								System.out.printf("평균 방문 간격: %.2f일\n", avgInterval);
							else
								System.out.println("예약 이력이 부족하거나 방문 간격 데이터를 계산할 수 없습니다.");
						} catch (SQLException e) {
							System.out.println("오류 발생: " + e.getMessage());
							e.printStackTrace();
						}
						break;
					case "6":
						System.out.println("티어별 평균 숙박일수 호출됨\n");
						
						try {
							dao.calAverageStayByTier();
						} catch (Exception e) {
							System.out.println(e.getMessage());
						}
						break;
					case "7":
						System.out.println("취소율 계산 호출됨\n");
						
						break;
					case "8":
						System.out.println("아이디로 사용자 조회 호출됨\n");
						
						break;
					case "9":
						System.out.println("전체 사용자 조회 호출됨\n");
						
						   try {
						        ArrayList<Client> clients = dao.getAllClients();
						        if (clients.isEmpty()) {
						            System.out.println("등록된 사용자가 없습니다.");
						        } else {
						            for (Client client : clients) {
						                System.out.printf("ID: %s | 이름: %s | MBTI: %s | 등급: %s | 예약 수: %d\n",
						                		client.getId(), client.getName(), client.getMbti(), client.getTier(), client.getBookings() != null ? client.getBookings().size() : 0);
						                if (client.getBookings() != null) {
						                    for (Booking b : client.getBookings()) {
						                        System.out.printf("  - 예약ID: %s | 게스트하우스: %s | 인원: %d | 체크인: %s | 숙박일수: %d | 가격: %,d원\n",
						                            b.getBookingId(), b.getGhName(), b.getPeopleCnt(), b.getCheckInDate(),
						                            b.getNights(), b.getTotalPrice());
						                    }
						                }
						                System.out.println("------------------------------------------------------");
						            }
						        }
						    } catch (SQLException e) {
						        System.out.println("오류 발생: " + e.getMessage());
						        e.printStackTrace();
						    }
						break;
					case "10":
						System.out.println("전체 예약 조회 호출됨\n");
						
						break;
					case "0":
						System.out.println("로그아웃합니다.");
						break;
					default:
						System.out.println("올바른 번호를 선택하세요.");
					}

					if (managerChoice.equals("0"))
						break;
				}
			}
		}

		sc.close();
	}

	private static void waitForQuit(Scanner sc, ScheduledExecutorService scheduler, ScheduledFuture<?> task) {
		while (true) {
			String input = sc.nextLine();
			if (input.equalsIgnoreCase("q")) {
				System.out.println("스케줄러 종료 중...");
				if (task != null)
					task.cancel(true);
				scheduler.shutdown();
				try {
					if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
						scheduler.shutdownNow();
					}
				} catch (InterruptedException e) {
					scheduler.shutdownNow();
					Thread.currentThread().interrupt();
				}
				System.out.println("종료되었습니다.");
				break;
			}
		}
	}
}
