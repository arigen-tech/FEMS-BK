package com.dmsBackend;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication
@EntityScan
public class DmsApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(DmsApplication.class, args);
//		System.out.println(System.getProperty("java.library.path"));


	}
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(DmsApplication.class);
	}


	@PostConstruct
	public void init() {
		log.info("========== DMS APPLICATION STARTED ==========");
	}



}
