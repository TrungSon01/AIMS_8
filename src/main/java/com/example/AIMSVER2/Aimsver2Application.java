package com.example.AIMSVER2;

import com.example.AIMSVER2.config.PayPalConfig;
import com.example.AIMSVER2.config.VietQRConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({PayPalConfig.class, VietQRConfig.class})
public class Aimsver2Application {

	public static void main(String[] args) {
		SpringApplication.run(Aimsver2Application.class, args);
	}

}
