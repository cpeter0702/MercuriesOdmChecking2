package com.firsttech.insurance.odmchecking.service.cronJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import com.firsttech.insurance.odmchecking.service.SmsService;

@Service
public class SmsTestJob {
	private final static Logger logger = LoggerFactory.getLogger(SmsTestJob.class);

	@Autowired
	private SmsService smsService;
	
	@Scheduled(cron = "0 0/10 * * * ?")
	public void sendSMSTesting() {
		boolean isSuccess = smsService.sendSMS();
		logger.info("[Test Job] SMS sending result: " + (isSuccess ? "SUCCESSFUL" : "FAIL"));
	}
}
