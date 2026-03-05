package com.planB.myexpressionfriend.common.scheduler;

import com.planB.myexpressionfriend.common.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 *
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     *
     * 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("============= RefreshToken 정리 시작 =============");

        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = refreshTokenRepository.deleteExpiredTokens(now);

            log.info("만료된 RefreshToken 정리 완료: {}건", deletedCount);

        } catch (Exception e) {
            log.error("만료된 RefreshToken 정리 중 오류 발생: {}", e.getMessage(), e);
        }

        log.info("============= RefreshToken 정리 완료 =============");
    }

    /**
     *
     */
    @Scheduled(cron = "0 0 4 * * SUN")
    @Transactional
    public void cleanupOldTokens() {
        log.info("============= 만료된 RefreshToken 정리 시작 =============");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            int deletedCount = refreshTokenRepository.deleteOldTokens(cutoffTime);

            log.info("30일 이상 지난 RefreshToken 삭제 완료: {}건", deletedCount);

        } catch (Exception e) {
            log.error("만료된 RefreshToken 정리 중 오류 발생: {}", e.getMessage(), e);
        }

        log.info("============= 만료된 RefreshToken 정리 완료 =============");
    }
}
