package com.oaoing.pmflow;

import org.springframework.boot.SpringApplication;

public class TestPmflowApplication {

	public static void main(String[] args) {
		SpringApplication.from(PmflowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
