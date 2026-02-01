package com.planB.myexpressionfriend.common.domain.note;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 노트 타입
 * - THERAPIST_NOTE: 치료사가 작성한 전문가 소견
 * - PARENT_NOTE: 부모가 작성한 관찰 일지
 * - SYSTEM: 시스템이 자동 생성한 노트 (게임 완료, 마일스톤 등)
 */
@Getter
@RequiredArgsConstructor
public enum NoteType {

    THERAPIST_NOTE("치료사 소견", "전문가가 작성한 임상적 분석과 가이드"),
    PARENT_NOTE("관찰 일지", "부모가 기록한 가정 내 아동의 행동 관찰"),
    SYSTEM("시스템 기록", "게임 완료, 마일스톤 달성 등 자동 생성된 기록");

    private final String displayName;
    private final String description;

    /**
     * 사용자가 직접 작성 가능한 타입인지 확인
     */
    public boolean isUserWritable() {

        return this == THERAPIST_NOTE || this == PARENT_NOTE;
    }

    /**
     * 시스템 자동 생성 타입인지 확인
     */
    public boolean isSystemGenerated() {
        return this == SYSTEM;
    }
}
