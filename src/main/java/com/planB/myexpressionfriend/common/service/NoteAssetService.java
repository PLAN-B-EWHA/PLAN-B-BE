package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteAsset;
import com.planB.myexpressionfriend.common.dto.note.NoteAssetDTO;
import com.planB.myexpressionfriend.common.event.NoteAssetUploadedEvent;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.NoteAssetRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NoteAssetService {

    private final NoteAssetRepository assetRepository;
    private final ChildNoteRepository noteRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.storage.base-path:uploads}")
    private String storageBasePath;

    @Transactional
    public NoteAssetDTO uploadFile(UUID noteId, MultipartFile file, UUID userId) {
        log.info("File upload start - noteId: {}, fileName: {}, size: {}",
                noteId, file.getOriginalFilename(), file.getSize());

        validateFile(file);

        ChildNote note = noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다."));

        if (!note.isAuthor(userId)) {
            throw new AccessDeniedException("노트 작성자만 파일을 업로드할 수 있습니다.");
        }

        try {
            NoteAsset asset = NoteAsset.createAsset(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    note.getChild().getChildId(),
                    noteId
            );

            saveFile(file, asset.getFileUrl());

            note.addAsset(asset);
            asset.setNote(note);

            NoteAsset savedAsset = assetRepository.save(asset);
            log.info("File upload completed - assetId: {}, storedFileName: {}",
                    savedAsset.getAssetId(), savedAsset.getStoredFileName());

            eventPublisher.publishEvent(new NoteAssetUploadedEvent(
                    note.getChild().getChildId(),
                    noteId,
                    savedAsset.getAssetId(),
                    userId
            ));

            return NoteAssetDTO.from(savedAsset);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일은 필수입니다.");
        }
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }
    }

    private void saveFile(MultipartFile file, String relativePath) throws IOException {
        Path filePath = Paths.get(storageBasePath, relativePath);
        Files.createDirectories(filePath.getParent());

        if (!filePath.normalize().startsWith(Paths.get(storageBasePath).normalize())) {
            throw new SecurityException("잘못된 파일 경로입니다.");
        }

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(storageBasePath));
            log.info("Storage directory initialized: {}", storageBasePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    public List<NoteAssetDTO> getAssetsByNote(UUID noteId, UUID userId) {
        noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다."));

        List<NoteAsset> assets = assetRepository.findByNoteId(noteId);
        return assets.stream()
                .map(NoteAssetDTO::from)
                .collect(Collectors.toList());
    }

    public NoteAssetDTO getAsset(UUID assetId, UUID userId) {
        NoteAsset asset = assetRepository.findByIdWithAuth(assetId, userId)
                .orElseThrow(() -> new AccessDeniedException("첨부파일 조회 권한이 없거나 존재하지 않는 파일입니다."));
        return NoteAssetDTO.from(asset);
    }

    public Path getFilePath(UUID assetId, UUID userId) {
        NoteAsset asset = assetRepository.findByIdWithAuth(assetId, userId)
                .orElseThrow(() -> new AccessDeniedException("첨부파일 조회 권한이 없거나 존재하지 않는 파일입니다."));

        Path filePath = Paths.get(storageBasePath, asset.getFileUrl());
        if (!filePath.normalize().startsWith(Paths.get(storageBasePath).normalize())) {
            throw new SecurityException("잘못된 파일 경로입니다.");
        }
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("파일을 찾을 수 없습니다.");
        }
        return filePath;
    }

    @Transactional
    public void deleteAsset(UUID assetId, UUID userId) {
        NoteAsset asset = assetRepository.findByIdWithAuth(assetId, userId)
                .orElseThrow(() -> new AccessDeniedException("첨부파일 조회 권한이 없거나 존재하지 않는 파일입니다."));

        ChildNote note = asset.getNote();
        if (!note.canDelete(userId)) {
            throw new AccessDeniedException("노트 작성자 또는 부모만 첨부파일을 삭제할 수 있습니다.");
        }

        try {
            deleteFile(asset.getFileUrl());
        } catch (IOException e) {
            log.error("Physical file delete failed - assetId: {}, path: {}", assetId, asset.getFileUrl(), e);
        }

        note.removeAsset(asset);
        assetRepository.delete(asset);
    }

    private void deleteFile(String relativePath) throws IOException {
        Path filePath = Paths.get(storageBasePath, relativePath);
        if (!filePath.normalize().startsWith(Paths.get(storageBasePath).normalize())) {
            throw new SecurityException("잘못된 파일 경로입니다.");
        }
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    public long getTotalFileSize(UUID childId, UUID userId) {
        return assetRepository.sumFileSizeByChildIdWithAuth(childId, userId);
    }
}
