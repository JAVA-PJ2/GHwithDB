package com.gh.test;

import java.time.LocalDate;
import java.util.ArrayList;

import com.gh.dao.impl.GHDAOImpl;
import com.gh.vo.Guesthouse;

public class GHDAOTest {
	public static void main(String[] args) {
		try {
            GHDAOImpl dao = GHDAOImpl.getInstance();

            LocalDate checkIn = LocalDate.of(2025, 6, 5);
            int nights = 2;
            int peopleCnt = 2;
            int maxPrice = 8000;
            char clientMbti = 'E'; // 또는 'I'

            ArrayList<Guesthouse> list = dao.searchAvailableGH(checkIn, nights, peopleCnt, maxPrice, clientMbti);

            System.out.println("[예약 가능한 게스트하우스 목록 - 필터 적용]");
            for (Guesthouse gh : list) {
                System.out.printf("이름: %s, MBTI: %s, 주중가격: %,d, 주말가격: %,d, 최대수용인원: %d%n",
                        gh.getName(),
                        gh.getMbti() != null ? gh.getMbti() : "없음",
                        gh.getPriceWeekday(),
                        gh.getPriceWeekend(),
                        gh.getMaxCapacity());
            }

            if (list.isEmpty()) {
                System.out.println("조건에 맞는 예약 가능한 게스트하우스가 없습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
