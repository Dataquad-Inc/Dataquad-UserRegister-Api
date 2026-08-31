package com.dataquadinc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class DataquadUserRegisterApiApplication {

	public static void main(String[] args) {

		SpringApplication.run(DataquadUserRegisterApiApplication.class, args);
	}
}

