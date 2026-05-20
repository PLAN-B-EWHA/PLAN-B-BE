package myexpressionfriend_api.rag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.child.domain.Child;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rag_sources", indexes = {
        @Index(name = "idx_rag_sources_type_status", columnList = "source_type, status"),
        @Index(name = "idx_rag_sources_child_use_case", columnList = "child_id, use_case"),
        @Index(name = "idx_rag_sources_created_at", columnList = "created_at")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RagSource {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "source_id", updatable = false, nullable = false)
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private RagSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "use_case", nullable = false, length = 40)
    private RagUseCase useCase;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RagSourceStatus status = RagSourceStatus.PENDING;

    @Column(name = "chunk_count")
    @Builder.Default
    private int chunkCount = 0;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void markIndexing() {
        this.status = RagSourceStatus.INDEXING;
        this.failureMessage = null;
    }

    public void updateExtractedContent(String rawContent, String contentHash) {
        this.rawContent = rawContent;
        this.contentHash = contentHash;
    }

    public void markIndexed(int chunkCount) {
        this.status = RagSourceStatus.INDEXED;
        this.chunkCount = chunkCount;
        this.indexedAt = LocalDateTime.now();
        this.failureMessage = null;
    }

    public void markFailed(String failureMessage) {
        this.status = RagSourceStatus.FAILED;
        this.failureMessage = failureMessage;
    }
}
