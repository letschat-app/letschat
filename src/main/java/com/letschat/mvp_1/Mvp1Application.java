package com.letschat.mvp_1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Mvp1Application {

	public static void main(String[] args) {
		SpringApplication.run(Mvp1Application.class, args);
	}

}
