package myexpressionfriend_api;

import myexpressionfriend_api.common.config.JWTProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties(JWTProperties.class)
public class MyexpressionfriendApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyexpressionfriendApiApplication.class, args);
	}

}
