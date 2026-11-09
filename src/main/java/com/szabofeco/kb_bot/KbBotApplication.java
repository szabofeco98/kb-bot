package com.szabofeco.kb_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KbBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(KbBotApplication.class, args);
	}

}
