package myexpressionfriend_api.therapist.memo.repository;

import myexpressionfriend_api.therapist.memo.domain.TherapistMemo;
import myexpressionfriend_api.therapist.memo.domain.TherapistMemoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TherapistMemoRepository extends JpaRepository<TherapistMemo, UUID> {

    Optional<TherapistMemo> findByIdAndTherapistId(UUID id, UUID therapistId);

    /**
     * 치료사 메모 목록 조회 (week_of / status 선택 필터)
     * null 전달 시 해당 조건 무시
     */
    @Query("""
            SELECT m FROM TherapistMemo m
            WHERE m.child.childId = :childId
              AND m.therapistId   = :therapistId
              AND (:status  IS NULL OR m.status  = :status)
              AND (:weekOf  IS NULL OR m.weekOf  = :weekOf)
            ORDER BY m.weekOf DESC, m.createdAt DESC
            """)
    Page<TherapistMemo> findWithFilters(
            @Param("childId") UUID childId,
            @Param("therapistId") UUID therapistId,
            @Param("status") TherapistMemoStatus status,
            @Param("weekOf") LocalDate weekOf,
            Pageable pageable
    );

    /**
     * 보호자 공개용: 발행됨 + 보호자 공개 설정된 메모만 반환
     */
    Page<TherapistMemo> findByChild_ChildIdAndIsVisibleToParentTrueAndStatusOrderByWeekOfDesc(
            UUID childId, TherapistMemoStatus status, Pageable pageable
    );
}
