package com.firsttech.insurance.odmchecking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class OdmCheckingApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(OdmCheckingApplication.class, args);
	}

}
