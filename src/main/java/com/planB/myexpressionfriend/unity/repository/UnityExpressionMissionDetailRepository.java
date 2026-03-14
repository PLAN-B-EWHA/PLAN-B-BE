package com.planB.myexpressionfriend.unity.repository;

import com.planB.myexpressionfriend.unity.domain.UnityExpressionMissionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnityExpressionMissionDetailRepository extends JpaRepository<UnityExpressionMissionDetail, Long> {
}