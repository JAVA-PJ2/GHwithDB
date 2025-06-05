package com.gh.test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.gh.dao.InsertDummyData;
import com.gh.dao.impl.GHDAOImpl;
import com.gh.vo.Guesthouse;

public class GHDAOTest {
    public static void main(String[] args) {
        GHDAOImpl dao = GHDAOImpl.getInstance();
        Scanner sc = new Scanner(System.in);
        
        // 사용자 더미 데이터 추가
//        try {
//            InsertDummyData inserter = new InsertDummyData();
//            inserter.insertDummyClients(); // 더미 데이터 삽입 실행
//        } catch (Exception e) {
//            System.out.println("데이터 삽입 중 오류 발생: " + e.getMessage());
//            e.printStackTrace();
//        }
        

		try {
	        System.out.print("아이디 입력: ");
	        String id = sc.nextLine();

	        System.out.print("비밀번호 입력: ");
	        String password = sc.nextLine();

			dao.login(id, password);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
        
        // 검색 조회 (실시간)
        LocalDate checkIn = LocalDate.of(2025, 6, 5);
        int nights = 2;
        int peopleCnt = 2;
        int maxPrice = 8000;
        char mbti = 'E';

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> runningTask = null;

        Runnable simpleTask = () -> {
            try {
                if (Thread.currentThread().isInterrupted()) return;

                List<Guesthouse> list = dao.searchAvailableGH(checkIn, nights, peopleCnt);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초");
                System.out.println("=== [전체 조회 결과 - " + LocalDateTime.now().format(formatter) + "] ===");
                if (list.isEmpty()) {
                    System.out.println("예약 가능한 게스트하우스가 없습니다.");
                } else {
                    for (Guesthouse gh : list) {
                        System.out.printf("이름: %s, MBTI: %s, 주중가격: %,d, 주말가격: %,d, 최대수용: %d\n",
                                gh.getName(),
                                gh.getMbti() != null ? gh.getMbti() : "없음",
                                gh.getPriceWeekday(),
                                gh.getPriceWeekend(),
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
                if (Thread.currentThread().isInterrupted()) return;

                List<Guesthouse> list = dao.searchAvailableGH(checkIn, nights, peopleCnt, maxPrice, mbti);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초");
                System.out.println("=== [조건 조회 결과 - " + LocalDateTime.now().format(formatter) + "] ===");
                if (list.isEmpty()) {
                    System.out.println("예약 가능한 게스트하우스가 없습니다.");
                } else {
                    for (Guesthouse gh : list) {
                        System.out.printf("이름: %s, MBTI: %s, 주중가격: %,d, 주말가격: %,d, 최대수용: %d\n",
                                gh.getName(),
                                gh.getMbti() != null ? gh.getMbti() : "없음",
                                gh.getPriceWeekday(),
                                gh.getPriceWeekend(),
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
        
        System.out.println("1: 전체 조회 / 2: 조건 조회 / q: 종료");

        while (true) {
            String input = sc.nextLine();

            if (input.equals("1")) {
                if (runningTask != null && !runningTask.isCancelled()) {
                    runningTask.cancel(true);
                }
                runningTask = scheduler.scheduleAtFixedRate(simpleTask, 0, 10, TimeUnit.SECONDS);

            } else if (input.equals("2")) {
                if (runningTask != null && !runningTask.isCancelled()) {
                    runningTask.cancel(true);
                }
                runningTask = scheduler.scheduleAtFixedRate(conditionTask, 0, 10, TimeUnit.SECONDS);

            } else if (input.equalsIgnoreCase("q")) {
                System.out.println("스케줄러 종료 중...");
                if (runningTask != null) {
                    runningTask.cancel(true);
                }
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.out.println("5초 내 종료 실패. 강제 종료.");
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

        sc.close();
    }
}
