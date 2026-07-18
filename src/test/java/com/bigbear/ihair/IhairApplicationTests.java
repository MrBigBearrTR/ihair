package com.bigbear.ihair;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.config.import=",
		"spring.profiles.active=dev",
		"spring.datasource.url=jdbc:h2:mem:ihair-test;DB_CLOSE_DELAY=-1"
})
class IhairApplicationTests {

	@Test
	void contextLoads() {
	}

}
