package com.planB.myexpressionfriend.common.domain;


/**
 * 아동별 사용자 권한 타입
 */
public enum ChildPermissionType {

    /**
     * 게임 플레이 권한 (Unity)
     */
    PLAY_GAME,

    /**
     * 리포트 조회 권한
     */
    VIEW_REPORT,

    /**
     * 치료 노트 작성 권한
     */
    WRITE_NOTE,

    /**
     * 홈 트레이닝 미션 할당 권한 (치료사)
     */
    ASSIGN_MISSION,

    /**
     * 아동 정보 관리 권한 (수정/삭제는 주보호자만)
     */
    MANAGE
}
