package com.thali;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ThaliApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThaliApplication.class, args);
	}

}
