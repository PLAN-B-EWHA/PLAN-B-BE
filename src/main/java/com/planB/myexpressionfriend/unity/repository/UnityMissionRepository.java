package com.planB.myexpressionfriend.unity.repository;

import com.planB.myexpressionfriend.unity.domain.UnityMission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Unity 미션 저장소
 */
@Repository
public interface UnityMissionRepository extends JpaRepository<UnityMission, Long> {

    /**
     * 생성일 기준 최신 Unity 미션을 페이지로 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return Page<UnityMission>
     */
    Page<UnityMission> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * missionId별 최신 정렬 기준으로 전체 Unity 미션을 조회합니다.
     *
     * @return List<UnityMission>
     */
    List<UnityMission> findAllByOrderByMissionIdAscCreatedAtDesc();
}