package com.firsttech.insurance.odmchecking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OdmPostTesterService {

	private final static Logger logger = LoggerFactory.getLogger(OdmPostTesterService.class);

	@Autowired
	private Environment environment;

//	@Scheduled(cron = "0 0/3 * * * ?")
	public void doCronTest () {
		doTest("20240901", "20241114");
	}
	
	/**
	 * 
	 * @param startDate (yyyyMMdd)
	 * @param endDate (yyyyMMdd)
	 */
	public void doTest(String startDate, String endDate) {
		logger.info("[OdmPostTesterService] preparing to do ODM reuslt checking .....");

		TestManager testODM = new TestManager(environment);

		boolean testflag = testODM.getTestFlag();
		String env = testODM.getEnv();

		if (!testflag) {
			logger.info("Test is off");
			return;
		}

		if ("dev".equals(env)) {
			testODM.initTest();

			Runnable NBtest = () -> {
				testODM.createTest("nb", startDate, endDate);
			};
			Runnable TAtest = () -> {
				testODM.createTest("ta", startDate, endDate);
			};

			testODM.executeTest(NBtest);
			testODM.executeTest(TAtest);
			testODM.closeTest();

		}
		logger.info("-------------------------");
	}

}
