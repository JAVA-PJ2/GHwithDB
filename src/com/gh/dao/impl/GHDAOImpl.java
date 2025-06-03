package com.gh.dao.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gh.dao.GHDAO;

import config.ServerInfo;

public class GHDAOImpl implements GHDAO {
	// 싱글톤
	private static GHDAOImpl dao = new GHDAOImpl("127.0.0.1");
	
	private GHDAOImpl(String serverIp) {
		try {
			Class.forName(ServerInfo.DRIVER_NAME);
		}catch(ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static GHDAOImpl getInstance() {
		return dao;
	}
	
	//공통로직
	private Connection getConnect() throws SQLException {
		Connection conn = DriverManager.getConnection(ServerInfo.URL, ServerInfo.USER, ServerInfo.PASS);
		return conn;
	}
	
	private void closeAll(PreparedStatement ps, Connection conn) throws SQLException {
		if(ps != null) ps.close();
		if(conn != null) conn.close();
	}
	
	private void closeAll(ResultSet rs, PreparedStatement ps, Connection conn) throws SQLException {
		if(rs != null) rs.close();
		closeAll(ps, conn);
	}
}
