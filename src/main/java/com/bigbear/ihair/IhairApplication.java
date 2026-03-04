package com.bigbear.ihair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class IhairApplication {

	public static void main(String[] args) {
		SpringApplication.run(IhairApplication.class, args);
	}

}
