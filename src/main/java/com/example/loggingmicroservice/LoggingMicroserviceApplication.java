package com.example.loggingmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@EnableAsync
public class LoggingMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoggingMicroserviceApplication.class, args);

	}

}
