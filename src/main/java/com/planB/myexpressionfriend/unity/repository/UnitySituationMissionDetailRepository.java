package com.planB.myexpressionfriend.unity.repository;

import com.planB.myexpressionfriend.unity.domain.UnitySituationMissionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitySituationMissionDetailRepository extends JpaRepository<UnitySituationMissionDetail, Long> {
}