package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteType;
import com.planB.myexpressionfriend.common.domain.report.GeneratedReport;
import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import com.planB.myexpressionfriend.common.domain.report.ReportStatus;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.report.GeneratedReportDTO;
import com.planB.myexpressionfriend.common.repository.GeneratedReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GeneratedReportService {

    private final GeneratedReportRepository generatedReportRepository;
    private final ChildNoteService childNoteService;

    @Transactional
    public GeneratedReport createPendingReport(
            UUID userId,
            UUID targetChildId,
            ReportPreference preference,
            LocalDateTime periodStartAt,
            LocalDateTime periodEndAt
    ) {
        if (targetChildId == null) {
            throw new IllegalStateException("targetChildId is required for report generation");
        }

        GeneratedReport report = GeneratedReport.builder()
                .userId(userId)
                .targetChildId(targetChildId)
                .preferenceId(preference != null ? preference.getPreferenceId() : null)
                .status(ReportStatus.PENDING)
                .periodStartAt(periodStartAt)
                .periodEndAt(periodEndAt)
                .build();

        GeneratedReport saved = generatedReportRepository.save(report);
        log.info("Pending report created. reportId={}, userId={}, targetChildId={}",
                saved.getReportId(), saved.getUserId(), saved.getTargetChildId());
        return saved;
    }

    public GeneratedReport getUserReport(UUID userId, UUID reportId) {
        return generatedReportRepository.findAuthorizedByReportId(reportId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    public GeneratedReportDTO getUserReportDTO(UUID userId, UUID reportId) {
        return GeneratedReportDTO.from(getUserReport(userId, reportId));
    }

    public PageResponseDTO<GeneratedReport> getUserReports(UUID userId, Pageable pageable) {
        Page<GeneratedReport> page = generatedReportRepository.findAuthorizedReports(userId, pageable);
        return PageResponseDTO.from(page);
    }

    public PageResponseDTO<GeneratedReportDTO> getUserReportDTOs(UUID userId, Pageable pageable) {
        Page<GeneratedReport> page = generatedReportRepository.findAuthorizedReports(userId, pageable);
        return PageResponseDTO.from(page, GeneratedReportDTO::from);
    }

    public long countByStatus(UUID userId, ReportStatus status) {
        return generatedReportRepository.countByUserIdAndStatus(userId, status);
    }

    @Transactional
    public GeneratedReport markGenerated(
            UUID reportId,
            String title,
            String summary,
            String reportBody,
            String promptUsed,
            String modelName,
            LocalDateTime issuedAt
    ) {
        GeneratedReport report = generatedReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.markGenerated(title, summary, reportBody, promptUsed, modelName, issuedAt);
        GeneratedReport saved = generatedReportRepository.save(report);

        if (saved.getTargetChildId() != null) {
            ChildNote systemNote = childNoteService.createSystemNoteEntity(
                    saved.getTargetChildId(),
                    saved.getUserId(),
                    saved.getTitle(),
                    saved.getReportBody(),
                    NoteType.SYSTEM
            );
            saved.linkSystemNoteId(systemNote.getNoteId());
            saved = generatedReportRepository.save(saved);
            log.info("Report linked to system note. reportId={}, noteId={}", saved.getReportId(), systemNote.getNoteId());
        }

        log.info("Report generated. reportId={}, userId={}", saved.getReportId(), saved.getUserId());
        return saved;
    }

    @Transactional
    public GeneratedReport markFailed(UUID reportId, String reason) {
        GeneratedReport report = generatedReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.markFailed(reason);
        GeneratedReport saved = generatedReportRepository.save(report);
        log.warn("Report generation failed. reportId={}, reason={}", saved.getReportId(), reason);
        return saved;
    }

    @Transactional
    public GeneratedReport markSkipped(UUID reportId, String reason) {
        GeneratedReport report = generatedReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.markSkipped(reason);
        GeneratedReport saved = generatedReportRepository.save(report);
        log.info("Report skipped. reportId={}, reason={}", saved.getReportId(), reason);
        return saved;
    }
}
