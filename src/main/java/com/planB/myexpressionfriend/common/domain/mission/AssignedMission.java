package com.planB.myexpressionfriend.common.domain.mission;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 할당된 미션 엔티티
 *
 * 기능:
 * - 치료사가 아동에게 미션 할당
 * - 부모가 오프라인 활동 완료 후 체크 및 사진 등록
 * - 치료사가 완료 검증
 * - 미션 완료 시 시스템 노트 자동 생성
 *
 * 상태 전이:
 * ASSIGNED → IN_PROGRESS → COMPLETED → VERIFIED
 * (어느 단계에서든 CANCELLED 가능)
 *
 * 권한:
 * - 할당: THERAPIST만
 * - 시작/완료: PARENT만
 * - 검증: THERAPIST만
 * - 조회: THERAPIST, PARENT 모두
 */
@Entity
@Table(name = "assigned_missions", indexes = {
        @Index(name = "idx_missions_child", columnList = "child_id"),
        @Index(name = "idx_missions_therapist", columnList = "therapist_id"),
        @Index(name = "idx_missions_template", columnList = "template_id"),
        @Index(name = "idx_missions_status", columnList = "status"),
        @Index(name = "idx_missions_assigned", columnList = "assigned_at"),
        @Index(name = "idx_missions_deleted", columnList = "is_deleted"),
        @Index(name = "idx_missions_note", columnList = "system_note_id")
})
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE assigned_missions SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE mission_id = ?")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"child", "therapist", "template", "systemNote", "photos"})
@EntityListeners(AuditingEntityListener.class)
public class AssignedMission {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "mission_id", updatable = false, nullable = false)
    private UUID missionId;

    /**
     * 대상 아동
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    /**
     * 할당한 치료사
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    /**
     * 미션 템플릿
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MissionTemplate template;

    /**
     * 미션 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MissionStatus status = MissionStatus.ASSIGNED;

    /**
     * 할당일시
     */
    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    /**
     * 목표 완료일 (선택)
     */
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    /**
     * 시작일시 (부모가 시작 표시)
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 완료일시 (부모가 완료 표시)
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 검증일시 (치료사가 확인)
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * 부모 코멘트
     */
    @Column(columnDefinition = "TEXT")
    private String parentNote;

    /**
     * 치료사 피드백
     */
    @Column(name = "therapist_feedback", columnDefinition = "TEXT")
    private String therapistFeedback;

    /**
     * 연동된 시스템 노트 (자동 생성)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_note_id")
    private ChildNote systemNote;

    /**
     * 완료 증빙 사진 목록
     */
    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MissionPhoto> photos = new ArrayList<>();

    /**
     * Soft Delete
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============= 상태 전이 메서드 =============

    /**
     * 미션 시작
     * ASSIGNED → IN_PROGRESS
     */
    public void start() {
        if (!this.status.canTransitionTo(MissionStatus.IN_PROGRESS)) {
            throw new IllegalStateException(
                    String.format("'%s' 상태에서는 시작할 수 없습니다", this.status.getDisplayName())
            );
        }
        this.status = MissionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 미션 완료
     * IN_PROGRESS → COMPLETED
     */
    public void complete(String parentNote) {
        if (!this.status.canTransitionTo(MissionStatus.COMPLETED)) {
            throw new IllegalStateException(
                    String.format("'%s' 상태에서는 완료할 수 없습니다", this.status.getDisplayName())
            );
        }
        this.status = MissionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.parentNote = parentNote;
    }

    /**
     * 미션 검증
     * COMPLETED → VERIFIED
     */
    public void verify(String therapistFeedback) {
        if (!this.status.canTransitionTo(MissionStatus.VERIFIED)) {
            throw new IllegalStateException(
                    String.format("'%s' 상태에서는 검증할 수 없습니다", this.status.getDisplayName())
            );
        }
        this.status = MissionStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.therapistFeedback = therapistFeedback;
    }

    /**
     * 미션 취소
     * 모든 상태 → CANCELLED
     */
    public void cancel() {
        if (!this.status.canTransitionTo(MissionStatus.CANCELLED)) {
            throw new IllegalStateException(
                    String.format("'%s' 상태에서는 취소할 수 없습니다", this.status.getDisplayName())
            );
        }
        this.status = MissionStatus.CANCELLED;
    }

    public void setStatus(MissionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("상태는 비어있을 수 없습니다.");
        }
        this.status = status;
    }

    // ============= 권한 확인 메서드 =============

    /**
     * 할당한 치료사인지 확인
     */
    public boolean isTherapist(UUID userId) {
        return this.therapist.getUserId().equals(userId);
    }

    /**
     * 미션 시작 권한 확인 (WRITE_NOTE 권한이 있는 부모)
     */
    public boolean canStart(UUID userId) {
        return this.status == MissionStatus.ASSIGNED &&
                this.child.hasPermission(userId, ChildPermissionType.WRITE_NOTE);
    }

    /**
     * 미션 완료 권한 확인 (WRITE_NOTE 권한이 있는 부모)
     */
    public boolean canComplete(UUID userId) {
        return this.status == MissionStatus.IN_PROGRESS &&
                this.child.hasPermission(userId, ChildPermissionType.WRITE_NOTE);
    }

    /**
     * 미션 검증 권한 확인 (할당한 치료사만)
     */
    public boolean canVerify(UUID userId) {
        return this.status == MissionStatus.COMPLETED &&
                isTherapist(userId);
    }

    /**
     * 미션 취소 권한 확인 (할당한 치료사만)
     */
    public boolean canCancel(UUID userId) {
        return isTherapist(userId) &&
                this.status != MissionStatus.VERIFIED &&
                this.status != MissionStatus.CANCELLED;
    }

    /**
     * 미션 조회 권한 확인 (VIEW_REPORT 권한)
     */
    public boolean canView(UUID userId) {
        return isTherapist(userId) ||
                this.child.hasPermission(userId, ChildPermissionType.VIEW_REPORT);
    }

    // ============= 사진 관리 메서드 =============

    /**
     * 사진 추가
     * 양방향 연관관계 편의 메서드
     */
    public void addPhoto(MissionPhoto photo) {
        if (photo == null) {
            throw new IllegalArgumentException("사진 정보는 필수입니다");
        }

        // 최대 사진 개수 제한
        if (this.photos.size() >= 10) {
            throw new IllegalStateException("미션당 최대 10개까지 사진을 첨부할 수 있습니다");
        }

        this.photos.add(photo);
        if (photo.getMission() != this) {
            photo.setMission(this);
        }
    }

    /**
     * 사진 제거
     */
    public void removePhoto(MissionPhoto photo) {
        if (photo == null) {
            return;
        }
        this.photos.remove(photo);
    }

    /**
     * 모든 사진 제거
     */
    public void clearPhotos() {
        this.photos.clear();
    }

    // ============= 기타 메서드 =============

    /**
     * 시스템 노트 연결
     */
    public void linkSystemNote(ChildNote systemNote) {
        this.systemNote = systemNote;
    }

    /**
     * 목표 완료일 설정
     */
    public void setDueDate(LocalDateTime dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("목표 완료일은 현재 시간 이후여야 합니다");
        }
        this.dueDate = dueDate;
    }

    /**
     * 부모 코멘트 수정
     */
    public void changeParentNote(String parentNote) {
        if (parentNote != null && parentNote.length() > 5000) {
            throw new IllegalArgumentException("부모 코멘트는 5,000자를 초과할 수 없습니다");
        }
        this.parentNote = parentNote;
    }

    /**
     * 치료사 피드백 수정
     */
    public void changeTherapistFeedback(String therapistFeedback) {
        if (therapistFeedback != null && therapistFeedback.length() > 5000) {
            throw new IllegalArgumentException("치료사 피드백은 5,000자를 초과할 수 없습니다");
        }
        this.therapistFeedback = therapistFeedback;
    }

    /**
     * 마감일이 지났는지 확인
     */
    public boolean isOverdue() {
        return this.dueDate != null &&
                LocalDateTime.now().isAfter(this.dueDate) &&
                this.status != MissionStatus.COMPLETED &&
                this.status != MissionStatus.VERIFIED;
    }

    // ============= Soft Delete =============

    /**
     * Soft Delete
     * 관련 사진도 함께 삭제됨 (cascade)
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}