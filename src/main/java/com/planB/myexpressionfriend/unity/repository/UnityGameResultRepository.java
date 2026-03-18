package com.planB.myexpressionfriend.unity.repository;

import com.planB.myexpressionfriend.unity.domain.UnityGameResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Unity 게임 결과 저장소
 */
@Repository
public interface UnityGameResultRepository extends JpaRepository<UnityGameResult, Long> {

    /**
     * 아동의 최근 Unity 결과를 조회합니다.
     */
    List<UnityGameResult> findTop5ByGameSession_Child_ChildIdOrderByCreatedAtDesc(UUID childId);
}
