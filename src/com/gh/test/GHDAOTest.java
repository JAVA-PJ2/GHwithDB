package com.gh.test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.gh.dao.impl.GHDAOImpl;
import com.gh.vo.Guesthouse;

public class GHDAOTest {
	public static void main(String[] args) {
		GHDAOImpl dao = GHDAOImpl.getInstance();
		
		// ScheduledExecutorService 실시간으로 정보 가져오기
		LocalDate checkIn = LocalDate.of(2025, 6, 5);
		int nights = 2;
		int peopleCnt = 2;
		int maxPrice = 8000;
		char mbti = 'E';
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		
		Runnable task = () -> {
			try {
                List<Guesthouse> list = dao.searchAvailableGH(checkIn, nights, peopleCnt, maxPrice, mbti);
                System.out.println("=== [자동 조회 결과 - " + LocalDateTime.now() + "] ===");
                if(list.isEmpty()) {
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
			} catch(Exception e) {
				System.out.println("조회 중 오류 발생: " + e.getMessage());
				e.printStackTrace();
			}
		};
		
		scheduler.scheduleAtFixedRate(task, 0, 60, TimeUnit.SECONDS);
	}
}
