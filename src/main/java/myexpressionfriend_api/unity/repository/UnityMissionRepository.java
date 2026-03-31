package myexpressionfriend_api.unity.repository;

import myexpressionfriend_api.unity.domain.UnityMission;
import myexpressionfriend_api.unity.domain.UnityMissionApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface UnityMissionRepository extends JpaRepository<UnityMission, Long> {

    Page<UnityMission> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<UnityMission> findAllByOrderByMissionIdAscCreatedAtDesc();

    List<UnityMission> findByChild_ChildIdAndApprovalStatusAndMissionDate(
            UUID childId, UnityMissionApprovalStatus approvalStatus, LocalDate missionDate);

    List<UnityMission> findByChild_ChildIdAndApprovalStatus(
            UUID childId, UnityMissionApprovalStatus approvalStatus);
}
