package myexpressionfriend_api.statistics.dialogue.repository;

import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.statistics.dialogue.domain.DialogueStatSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DialogueStatSummaryRepository extends JpaRepository<DialogueStatSummary, UUID> {

    Optional<DialogueStatSummary> findByChild_ChildIdAndTheme(UUID childId, PeersTheme theme);

    List<DialogueStatSummary> findByChild_ChildId(UUID childId);

    void deleteByChild_ChildId(UUID childId);
}
