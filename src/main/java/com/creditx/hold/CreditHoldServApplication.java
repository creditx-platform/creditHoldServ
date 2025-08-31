package com.creditx.hold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CreditHoldServApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreditHoldServApplication.class, args);
	}

}
