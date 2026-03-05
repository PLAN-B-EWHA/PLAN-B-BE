package com.planB.myexpressionfriend.common.domain.note;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 노트 유형
 * - THERAPIST_NOTE: 치료사가 작성한 분석과 가이드
 * - PARENT_NOTE: 부모가 작성한 관찰 기록
 * - SYSTEM: 시스템이 자동 생성한 기록
 */
@Getter
@RequiredArgsConstructor
public enum NoteType {

    THERAPIST_NOTE("치료 노트", "치료사가 작성한 분석과 가이드"),
    PARENT_NOTE("관찰 기록", "부모가 작성한 관찰 기록"),
    SYSTEM("시스템 기록", "게임 완료, 리포트 생성 등 시스템 자동 기록");

    private final String displayName;
    private final String description;

    public boolean isUserWritable() {
        return this == THERAPIST_NOTE || this == PARENT_NOTE;
    }

    public boolean isSystemGenerated() {
        return this == SYSTEM;
    }
}
