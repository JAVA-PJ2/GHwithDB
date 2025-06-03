package com.gh.vo;

public class Manager {
	private String managerID; // 칼럼에는 manager_id
	private String managerPassword; // 칼럼에는 manager_password
	
	public Manager() {}

	public Manager(String managerID, String managerPassword) {
		this.managerID = managerID;
		this.managerPassword = managerPassword;
	}

	public String getManagerID() {
		return managerID;
	}

	public void setManagerID(String managerID) {
		this.managerID = managerID;
	}

	public String getManagerPassword() {
		return managerPassword;
	}

	public void setManagerPassword(String managerPassword) {
		this.managerPassword = managerPassword;
	}

	@Override
	public String toString() {
		return "Manager [managerID=" + managerID + ", managerPassword=" + managerPassword + "]";
	}
	
	
}
