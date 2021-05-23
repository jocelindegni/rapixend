package com.saankaa.rapidxend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class RapidxendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RapidxendApplication.class, args);
	}

}
