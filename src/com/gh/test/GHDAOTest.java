package com.gh.test;

import config.ServerInfo;

public class GHDAOTest {
	public static void main(String[] args) {
		
	}
	
	static {
		try {
			Class.forName(ServerInfo.DRIVER_NAME);
		}catch(ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
	}
}
