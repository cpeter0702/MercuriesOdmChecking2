package com.firsttech.insurance.odmchecking.domain;

import java.util.Date;

public class Policy {
	public Date keep_date_time;
	private String policy_no;
	private String case_in;


	public Date getKeep_date_time() {
		return keep_date_time;
	}


	public void setKeep_date_time(Date keep_date_time) {
		this.keep_date_time = keep_date_time;
	}


	public String getPolicy_no() {
		return policy_no;
	}


	public void setPolicy_no(String policy_no) {
		this.policy_no = policy_no;
	}


	public String getCase_in() {
		return case_in;
	}


	public void setCase_in(String case_in) {
		this.case_in = case_in;
	}

	public Policy () {
		
	}
	public Policy(Date keep_date_time, String policy_no, String case_in) {
		this.keep_date_time = keep_date_time;
		this.policy_no = policy_no;
		this.case_in = case_in;
	}
}
