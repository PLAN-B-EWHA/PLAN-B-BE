package com.planB.myexpressionfriend;

import com.planB.myexpressionfriend.common.config.JWTProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(JWTProperties.class)
public class MyExpressionFriendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyExpressionFriendApplication.class, args);
	}

}
