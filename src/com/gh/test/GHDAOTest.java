package com.gh.test;

import com.gh.dao.impl.GHDAOImpl;

public class GHDAOTest {
	public static void main(String[] args) {
		try {
            GHDAOImpl dao = GHDAOImpl.getInstance(); // 싱글톤 DAO

            String clientId = "user1"; // 테스트할 client_id를 입력
            int days = dao.getDayBetweenBooking(clientId);

            if (days >= 0) {
                System.out.println(days + "일 만에 예약하셨습니다.");
            } else {
                System.out.println("이전에 예약한 기록이 없습니다.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
