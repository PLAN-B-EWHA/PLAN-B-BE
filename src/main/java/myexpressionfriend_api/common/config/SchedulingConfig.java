package myexpressionfriend_api.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화 설정
 * - 알림 스케줄러(주간 요약, 아동 미접속)를 구동하기 위해 @EnableScheduling 적용
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
