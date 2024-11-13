package com.firsttech.insurance.odmchecking.service.cronJob;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

public class MyJob {

	@Scheduled(cron = "0 0/3 * * * ?")
	public void testRunTwice () {
		System.out.println("AAAAAAAAAAAAAAAAAAAAAA");
	}
}
