package com.planB.myexpressionfriend.common.scheduler;

import com.planB.myexpressionfriend.common.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * RefreshToken 정리 스케줄러
 *
 * 만료된 RefreshToken을 주기적으로 삭제하여 DB 공간 절약
 *
 * ✅ 선택사항: 필요하면 사용
 * ⚠️ @EnableScheduling을 Application 클래스에 추가해야 함
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 만료된 RefreshToken 정리
     *
     * 매일 새벽 3시에 실행
     * Cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("============= RefreshToken 정리 시작 =============");

        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = refreshTokenRepository.deleteExpiredTokens(now);

            log.info("만료된 RefreshToken 삭제 완료: {}건", deletedCount);

        } catch (Exception e) {
            log.error("RefreshToken 정리 중 에러 발생: {}", e.getMessage(), e);
        }

        log.info("============= RefreshToken 정리 완료 =============");
    }

    /**
     * 오래된 RefreshToken 정리 (선택사항)
     *
     * 30일 이상 오래된 토큰 삭제
     * 매주 일요일 새벽 4시에 실행
     */
    @Scheduled(cron = "0 0 4 * * SUN")  // 매주 일요일 새벽 4시
    @Transactional
    public void cleanupOldTokens() {
        log.info("============= 오래된 RefreshToken 정리 시작 =============");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            int deletedCount = refreshTokenRepository.deleteOldTokens(cutoffTime);

            log.info("30일 이상 오래된 RefreshToken 삭제 완료: {}건", deletedCount);

        } catch (Exception e) {
            log.error("오래된 RefreshToken 정리 중 에러 발생: {}", e.getMessage(), e);
        }

        log.info("============= 오래된 RefreshToken 정리 완료 =============");
    }
}