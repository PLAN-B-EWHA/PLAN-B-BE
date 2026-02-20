package com.planB.myexpressionfriend.common.domain.mission;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MissionTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MissionStatus status = MissionStatus.ASSIGNED;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(columnDefinition = "TEXT")
    private String parentNote;

    @Column(name = "therapist_feedback", columnDefinition = "TEXT")
    private String therapistFeedback;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_note_id")
    private ChildNote systemNote;

    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MissionPhoto> photos = new ArrayList<>();

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

    public void start() {
        if (!this.status.canTransitionTo(MissionStatus.IN_PROGRESS)) {
            throw new IllegalStateException("'" + this.status.getDisplayName() + "' 상태에서는 시작할 수 없습니다.");
        }
        this.status = MissionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete(String parentNote) {
        if (!this.status.canTransitionTo(MissionStatus.COMPLETED)) {
            throw new IllegalStateException("'" + this.status.getDisplayName() + "' 상태에서는 완료할 수 없습니다.");
        }
        this.status = MissionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.parentNote = parentNote;
    }

    public void verify(String therapistFeedback) {
        if (!this.status.canTransitionTo(MissionStatus.VERIFIED)) {
            throw new IllegalStateException("'" + this.status.getDisplayName() + "' 상태에서는 검증할 수 없습니다.");
        }
        this.status = MissionStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.therapistFeedback = therapistFeedback;
    }

    public void cancel() {
        if (!this.status.canTransitionTo(MissionStatus.CANCELLED)) {
            throw new IllegalStateException("'" + this.status.getDisplayName() + "' 상태에서는 취소할 수 없습니다.");
        }
        this.status = MissionStatus.CANCELLED;
    }

    public boolean isTherapist(UUID userId) {
        return this.therapist.getUserId().equals(userId);
    }

    public boolean canStart(UUID userId) {
        return this.status == MissionStatus.ASSIGNED
                && this.child.hasPermission(userId, ChildPermissionType.WRITE_NOTE);
    }

    public boolean canComplete(UUID userId) {
        return this.status == MissionStatus.IN_PROGRESS
                && this.child.hasPermission(userId, ChildPermissionType.WRITE_NOTE);
    }

    public boolean canVerify(UUID userId) {
        return this.status == MissionStatus.COMPLETED && isTherapist(userId);
    }

    public boolean canCancel(UUID userId) {
        return isTherapist(userId)
                && this.status != MissionStatus.VERIFIED
                && this.status != MissionStatus.CANCELLED;
    }

    public boolean canView(UUID userId) {
        return isTherapist(userId)
                || this.child.hasPermission(userId, ChildPermissionType.VIEW_REPORT);
    }

    public void addPhoto(MissionPhoto photo) {
        if (photo == null) {
            throw new IllegalArgumentException("사진 정보는 필수입니다.");
        }
        if (this.photos.size() >= 10) {
            throw new IllegalStateException("미션당 사진은 최대 10개까지 첨부할 수 있습니다.");
        }
        this.photos.add(photo);
        if (photo.getMission() != this) {
            photo.setMission(this);
        }
    }

    public void removePhoto(MissionPhoto photo) {
        if (photo == null) {
            return;
        }
        this.photos.remove(photo);
    }

    public void clearPhotos() {
        this.photos.clear();
    }

    public void linkSystemNote(ChildNote systemNote) {
        this.systemNote = systemNote;
    }

    public void setDueDate(LocalDateTime dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("목표 완료일은 현재 시각 이후여야 합니다.");
        }
        this.dueDate = dueDate;
    }

    public void changeParentNote(String parentNote) {
        if (parentNote != null && parentNote.length() > 5000) {
            throw new IllegalArgumentException("부모 코멘트는 5,000자를 초과할 수 없습니다.");
        }
        this.parentNote = parentNote;
    }

    public void changeTherapistFeedback(String therapistFeedback) {
        if (therapistFeedback != null && therapistFeedback.length() > 5000) {
            throw new IllegalArgumentException("치료사 피드백은 5,000자를 초과할 수 없습니다.");
        }
        this.therapistFeedback = therapistFeedback;
    }

    public boolean isOverdue() {
        return this.dueDate != null
                && LocalDateTime.now().isAfter(this.dueDate)
                && this.status != MissionStatus.COMPLETED
                && this.status != MissionStatus.VERIFIED;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}
