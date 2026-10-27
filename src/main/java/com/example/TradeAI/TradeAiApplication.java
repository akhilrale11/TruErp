/*
 * Copyright (c) 2026 Akhil Gitaram Rale. All Rights Reserved.
 * Owner & Lead Developer: Akhil Gitaram Rale
 * Mobile: +91 7719080018 | Email: akhilrale5577@gmail.com
 */
package com.example.TradeAI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradeAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeAiApplication.class, args);
	}

}

