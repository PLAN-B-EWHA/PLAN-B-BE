package com.planB.myexpressionfriend.common.dto.game;

import com.planB.myexpressionfriend.common.domain.game.GameSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게임 세션 응답 DTO
 */
@Getter
@Builder
public class GameSessionDTO {

    private UUID sessionId;
    private String sessionToken;
    private UUID childId;
    private String childName;
    private UUID authenticatedBy;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static GameSessionDTO from(GameSession session) {
        if (session == null) {
            return null;
        }

        return GameSessionDTO.builder()
                .sessionId(session.getSessionId())
                .sessionToken(session.getSessionToken())
                .childId(session.getChild().getChildId())
                .childName(session.getChild().getName())
                .authenticatedBy(session.getAuthenticatedBy().getUserId())
                .expiresAt(session.getExpiresAt())
                .isActive(session.getIsActive())
                .createdAt(session.getCreatedAt())
                .build();
    }
}