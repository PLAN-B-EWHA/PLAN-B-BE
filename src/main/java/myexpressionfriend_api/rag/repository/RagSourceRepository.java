package myexpressionfriend_api.rag.repository;

import myexpressionfriend_api.rag.domain.RagSource;
import myexpressionfriend_api.rag.domain.RagSourceStatus;
import myexpressionfriend_api.rag.domain.RagSourceType;
import myexpressionfriend_api.rag.domain.RagUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RagSourceRepository extends JpaRepository<RagSource, UUID> {

    Page<RagSource> findBySourceTypeAndStatusOrderByCreatedAtDesc(
            RagSourceType sourceType,
            RagSourceStatus status,
            Pageable pageable
    );

    List<RagSource> findByChild_ChildIdAndUseCaseAndStatusOrderByCreatedAtDesc(
            UUID childId,
            RagUseCase useCase,
            RagSourceStatus status
    );
}
