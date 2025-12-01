package com.predictifylabs.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PredictifylabsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PredictifylabsBackendApplication.class, args);
	}

}
