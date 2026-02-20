package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.mission.MissionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MissionPhotoRepository extends JpaRepository<MissionPhoto, UUID> {

    @Query("""
        SELECT p FROM MissionPhoto p
        JOIN FETCH p.mission m
        JOIN FETCH m.child c
        WHERE p.photoId = :photoId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = c.childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Optional<MissionPhoto> findByIdWithAuth(@Param("photoId") UUID photoId, @Param("userId") UUID userId);

    @Query("""
        SELECT p FROM MissionPhoto p
        JOIN p.mission m
        JOIN m.child c
        WHERE m.missionId = :missionId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = c.childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        ORDER BY p.createdAt ASC
        """)
    List<MissionPhoto> findByMissionIdWithAuth(@Param("missionId") UUID missionId, @Param("userId") UUID userId);
}
