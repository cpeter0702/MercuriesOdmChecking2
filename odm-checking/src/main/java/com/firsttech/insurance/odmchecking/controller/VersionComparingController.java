package com.firsttech.insurance.odmchecking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.firsttech.insurance.odmchecking.domain.DateRange;
import com.firsttech.insurance.odmchecking.service.VersionComparingService;
import com.firsttech.insurance.odmchecking.service.VersionComparingService2;

@RestController
@RequestMapping("/api")
public class VersionComparingController {
	private final static Logger logger = LoggerFactory.getLogger(VersionComparingController.class);
	@Autowired
	private VersionComparingService2 versionComparingService2;
	
    @GetMapping("/compare")
    public boolean callODMResultChecking(@RequestBody DateRange dateRange) {
    	logger.info("[API] start to do version comparing: {}", dateRange.show());
    	return versionComparingService2.doComparing(dateRange.getStartDate(), dateRange.getEndDate());
    }
}