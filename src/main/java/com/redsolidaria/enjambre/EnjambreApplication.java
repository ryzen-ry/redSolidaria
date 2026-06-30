package com.redsolidaria.enjambre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EnjambreApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnjambreApplication.class, args);
	}

}

