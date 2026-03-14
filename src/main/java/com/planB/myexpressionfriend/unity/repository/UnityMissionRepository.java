package com.planB.myexpressionfriend.unity.repository;

import com.planB.myexpressionfriend.unity.domain.UnityMission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnityMissionRepository extends JpaRepository<UnityMission, Long> {

    @EntityGraph(attributePaths = {"expressionDetail", "situationDetail"})
    Page<UnityMission> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"expressionDetail", "situationDetail"})
    List<UnityMission> findAllByOrderByMissionIdAscCreatedAtDesc();
}