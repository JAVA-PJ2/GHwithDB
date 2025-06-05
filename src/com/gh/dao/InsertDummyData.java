package com.gh.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import config.ServerInfo;

public class InsertDummyData {
	
	private Connection getConnect() throws SQLException {
		Connection conn = DriverManager.getConnection(ServerInfo.URL, ServerInfo.USER, ServerInfo.PASS);
		return conn;
	}
	
	
	public void insertDummyClients() throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		
		String[][] clients = {
				{"user1", "pass1234", "소유나", "E", "U"},
		        {"user2", "pass2345", "김선호", "I", "U"},
		        {"user3", "pass3456", "양준용", "E", "U"},
		        {"user4", "pass4567", "함동윤", "I", "U"},
		    };
		
		try {
			conn = getConnect();
			String sql = "INSERT INTO client (client_id, client_password, client_name, mbti, tier) VALUES (?, ?, ?, ?, ?)";
			
			ps = conn.prepareStatement(sql);

	        for (String[] client : clients) {
	            ps.setString(1, client[0]); 
	            ps.setString(2, PasswordUtil.encrypt(client[1])); // 암호화
	            ps.setString(3, client[2]);
	            if (client[3] != null)
	                ps.setString(4, client[3]); 
	            else
	                ps.setNull(4, java.sql.Types.CHAR);
	            ps.setString(5, client[4]); 


	            ps.executeUpdate();
	        }

	        System.out.println("클라이언트 더미 데이터 삽입 완료");
		} finally {
			conn.close();
		}
		
	}
	
	public void insertDummyManagers() throws SQLException {
		Connection conn = null;
		PreparedStatement ps = null;
		
		String[][] managers = {
				{"mgr1", "pass1234"},
				{"mgr2", "pass2345"},
				{"mgr3", "pass3456"},
				{"mgr4", "pass4567"},
		   
		    };
		
		try {
			conn = getConnect();
			String sql = "INSERT INTO manager (manager_id, manager_password) VALUES (?, ?)";
			
			ps = conn.prepareStatement(sql);

	        for (String[] manager : managers) {
	            ps.setString(1, manager[0]); 
	            ps.setString(2, PasswordUtil.encrypt(manager[1])); // 암호화

	            ps.executeUpdate();
	        }

	        System.out.println("매니저 더미 데이터 삽입 완료");
		} finally {
			conn.close();
		}
		
	}

}
