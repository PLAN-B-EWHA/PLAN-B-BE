package com.planB.myexpressionfriend.unity.repository;

import com.planB.myexpressionfriend.unity.domain.UnityGameResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Unity 게임 결과 저장소
 */
@Repository
public interface UnityGameResultRepository extends JpaRepository<UnityGameResult, Long> {
}