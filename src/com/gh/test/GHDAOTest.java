package com.gh.test;

import java.time.LocalDate;

import com.gh.dao.impl.GHDAOImpl;
import com.gh.vo.Guesthouse;

public class GHDAOTest {
	public static void main(String[] args) {
		try {
	        GHDAOImpl dao = GHDAOImpl.getInstance(); // 싱글톤 DAO

	        boolean result = dao.canBook(new Guesthouse("감성하우스", 'E', 5800, 7800, 4), LocalDate.of(2025, 7, 1), 3, 4);

	        if (result) {
	            System.out.println("예약 가능합니다!");
	        } else {
	            System.out.println("예약 불가입니다 😢");
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}
