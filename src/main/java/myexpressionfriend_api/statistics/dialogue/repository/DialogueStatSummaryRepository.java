package myexpressionfriend_api.statistics.dialogue.repository;

import jakarta.persistence.LockModeType;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.statistics.dialogue.domain.DialogueStatSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DialogueStatSummaryRepository extends JpaRepository<DialogueStatSummary, UUID> {

    Optional<DialogueStatSummary> findByChild_ChildIdAndTheme(UUID childId, PeersTheme theme);

    /** 오프라인 결과 기록용: 동시 업데이트 경쟁 방지를 위해 비관적 쓰기 락 사용 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DialogueStatSummary s WHERE s.child.childId = :childId AND s.theme = :theme")
    Optional<DialogueStatSummary> findByChild_ChildIdAndThemeForUpdate(
            @Param("childId") UUID childId, @Param("theme") PeersTheme theme);

    List<DialogueStatSummary> findByChild_ChildId(UUID childId);

    void deleteByChild_ChildId(UUID childId);
}
