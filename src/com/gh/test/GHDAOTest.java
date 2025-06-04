package com.gh.test;

import java.time.LocalDate;
import java.util.ArrayList;

import com.gh.dao.impl.GHDAOImpl;
import com.gh.vo.Guesthouse;

public class GHDAOTest {
	public static void main(String[] args) {
		  try {
	            GHDAOImpl dao = GHDAOImpl.getInstance();

	            // 테스트용 파라미터
	            LocalDate checkIn = LocalDate.of(2025, 6, 1);
	            int nights = 2;
	            int people = 3;

	            // 예약 가능한 게스트하우스 리스트 조회
	            ArrayList<Guesthouse> result = dao.searchAvailableGH(checkIn, nights, people);

	            // 결과 출력
	            if (result.isEmpty()) {
	                System.out.println("예약 가능한 게스트하우스가 없습니다.");
	            } else {
	                System.out.println("[예약 가능한 게스트하우스 목록]");
	                for (Guesthouse gh : result) {
	                    System.out.println("이름: " + gh.getName() +
	                                       ", MBTI: " + gh.getMbti() +
	                                       ", 주중가격: " + gh.getPriceWeekday() +
	                                       ", 주말가격: " + gh.getPriceWeekend() +
	                                       ", 최대수용인원: " + gh.getMaxCapacity());
	                }
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	}
}
