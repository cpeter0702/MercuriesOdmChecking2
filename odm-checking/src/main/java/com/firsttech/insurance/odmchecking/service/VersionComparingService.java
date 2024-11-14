package com.firsttech.insurance.odmchecking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class VersionComparingService {

	@Autowired
	private Environment environment;
	
	
}
