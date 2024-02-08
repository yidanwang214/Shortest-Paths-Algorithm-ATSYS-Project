package com.adl.path;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(basePackages = "com.adl.path.dao")
@SpringBootApplication
public class PathApplication {

	public static void main(String[] args) {
		SpringApplication.run(PathApplication.class, args);
	}

}
