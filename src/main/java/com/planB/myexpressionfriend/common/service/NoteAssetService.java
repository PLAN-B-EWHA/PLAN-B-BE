package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteAsset;
import com.planB.myexpressionfriend.common.dto.note.NoteAssetDTO;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.NoteAssetRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * NoteAsset Service
 *
 * 책임:
 * - 파일 업로드/다운로드
 * - 파일 저장소 관리
 * - 첨부파일 CRUD
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NoteAssetService {

    private final NoteAssetRepository assetRepository;
    private final ChildNoteRepository noteRepository;

    @Value("${app.storage.base-path:uploads}")
    private String storageBasePath;

    // ============= 파일 업로드 =============

    /**
     * 파일 업로드
     * 권한: 노트 작성자만 파일 업로드 가능
     *
     * @param noteId 노트 ID
     * @param file 업로드할 파일
     * @param userId 업로드 요청 사용자 ID
     * @return 업로드된 파일 DTO
     */
    @Transactional
    public NoteAssetDTO uploadFile(UUID noteId, MultipartFile file, UUID userId) {
        log.info("파일 업로드 시작 - noteId: {}, fileName: {}, size: {}",
                noteId, file.getOriginalFilename(), file.getSize());

        // 1. 파일 유효성 검증
        validateFile(file);

        // 2. 노트 조회 (권한 검증 포함)
        ChildNote note = noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다"));

        // 3. 업로드 권한 검증: 작성자만
        if (!note.isAuthor(userId)) {
            log.warn("파일 업로드 권한 없음 - noteId: {}, userId: {}, authorId: {}",
                    noteId, userId, note.getAuthor().getUserId());
            throw new AccessDeniedException("노트 작성자만 파일을 업로드할 수 있습니다");
        }

        // 4. 첨부파일 개수 제한 확인 (엔티티에서 검증됨)
        try {
            // 5. NoteAsset 엔티티 생성
            NoteAsset asset = NoteAsset.createAsset(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    note.getChild().getChildId(),
                    noteId
            );

            // 6. 파일 저장
            saveFile(file, asset.getFileUrl());

            // 7. 노트에 첨부파일 추가
            note.addAsset(asset);
            asset.setNote(note);

            // 8. 저장
            NoteAsset savedAsset = assetRepository.save(asset);
            log.info("파일 업로드 완료 - assetId: {}, storedFileName: {}",
                    savedAsset.getAssetId(), savedAsset.getStoredFileName());

            return NoteAssetDTO.from(savedAsset);

        } catch (IOException e) {
            log.error("파일 저장 실패 - noteId: {}, fileName: {}", noteId, file.getOriginalFilename(), e);
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일은 필수입니다");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다");
        }

        // 추가 검증은 AssetType enum에서 수행됨
    }

    /**
     * 파일 저장 (로컬 스토리지)
     *
     * @param file 업로드 파일
     * @param relativePath 상대 경로 (DB 저장값)
     */
    private void saveFile(MultipartFile file, String relativePath) throws IOException {
        // 절대 경로 생성
        Path filePath = Paths.get(storageBasePath, relativePath);

        // 디렉토리 생성
        Files.createDirectories(filePath.getParent());

        // Directory Traversal 공격 방어
        if (!filePath.normalize().startsWith(Paths.get(storageBasePath).normalize())) {
            throw new SecurityException("잘못된 파일 경로입니다");
        }

        // 파일 저장
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("파일 저장 완료 - path: {}", filePath);
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(storageBasePath));
            log.info("Storage directory initialized: {}", storageBasePath);
        } catch (IOException e) {
            log.error("Could not create storage directory: {}", storageBasePath, e);
            throw new RuntimeException("Failed to initialize storage directory", e);
        }
    }

    // ============= 파일 조회 =============

    /**
     * 노트의 모든 첨부파일 조회
     *
     * @param noteId 노트 ID
     * @param userId 조회 요청 사용자 ID
     * @return 첨부파일 목록
     */
    public List<NoteAssetDTO> getAssetsByNote(UUID noteId, UUID userId) {
        log.debug("노트 첨부파일 조회 - noteId: {}, userId: {}", noteId, userId);

        // 권한 검증: 노트 조회 가능한지 확인
        noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다"));

        List<NoteAsset> assets = assetRepository.findByNoteId(noteId);
        return assets.stream()
                .map(NoteAssetDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 첨부파일 상세 조회
     *
     * @param assetId 첨부파일 ID
     * @param userId 조회 요청 사용자 ID
     * @return 첨부파일 DTO
     */
    public NoteAssetDTO getAsset(UUID assetId, UUID userId) {
        log.debug("첨부파일 조회 - assetId: {}, userId: {}", assetId, userId);

        NoteAsset asset = assetRepository.findByIdWithAuth(assetId, userId)
                .orElseThrow(() -> new AccessDeniedException("첨부파일 조회 권한이 없거나 존재하지 않는 파일입니다"));

        return NoteAssetDTO.from(asset);
    }

    /**
     * 파일 다운로드를 위한 절대 경로 반환
     *
     * @param assetId 첨부파일 ID
     * @param userId 조회 요청 사용자 ID
     * @return 파일 절대 경로
     */
    public Path getFilePath(UUID assetId, UUID userId) {
        NoteAsset asset = assetRepository.findByIdWithAuth(assetId, userId)
                .orElseThrow(() -> new AccessDeniedException("첨부파일 조회 권한이 없거나 존재하지 않는 파일입니다"));

        Path filePath = Paths.get(storageBasePath, asset.getFileUrl());

        // Directory Traversal 공격 방어
        if (!filePath.normalize().startsWith(Paths.get(storageBasePath).normalize())) {
            throw new SecurityException("잘못된 파일 경로입니다");
        }

        if (!Files.exists(filePath)) {
            log.error("파일이 존재하지 않음 - assetId: {}, path: {}", assetId, filePath);
            throw new IllegalStateException("파일이 존재하지 않습니다");
        }

        return filePath;
    }

    // ============= 파일 삭제 =============

    /**
     * 첨부파일 삭제
     * 권한: 노트 작성자 또는 주보호자
     *
     * @param assetId 첨부파일 ID
     * @param userId 삭제 요청 사용자 ID
     */
    @Transactional
    public void deleteAsset(UUID assetId, UUID userId) {
        log.info("첨부파일 삭제 시작 - assetId: {}, userId: {}", assetId, userId);

        // 1. 첨부파일 조회 (권한 검증 포함)
        NoteAsset asset = assetRepository.findByIdWithAuth(assetId, userId)
                .orElseThrow(() -> new AccessDeniedException("첨부파일 조회 권한이 없거나 존재하지 않는 파일입니다"));

        ChildNote note = asset.getNote();

        // 2. 삭제 권한 검증: 노트 작성자 또는 주보호자
        if (!note.canDelete(userId)) {
            log.warn("첨부파일 삭제 권한 없음 - assetId: {}, userId: {}", assetId, userId);
            throw new AccessDeniedException("노트 작성자 또는 주보호자만 첨부파일을 삭제할 수 있습니다");
        }

        // 3. 물리 파일 삭제
        try {
            deleteFile(asset.getFileUrl());
        } catch (IOException e) {
            log.error("파일 삭제 실패 - assetId: {}, path: {}", assetId, asset.getFileUrl(), e);
            // 물리 파일 삭제 실패해도 DB 레코드는 삭제
        }

        // 4. DB 삭제
        note.removeAsset(asset);
        assetRepository.delete(asset);
        log.info("첨부파일 삭제 완료 - assetId: {}", assetId);
    }

    /**
     * 파일 삭제 (로컬 스토리지)
     */
    private void deleteFile(String relativePath) throws IOException {
        Path filePath = Paths.get(storageBasePath, relativePath);

        // Directory Traversal 공격 방어
        if (!filePath.normalize().startsWith(Paths.get(storageBasePath).normalize())) {
            throw new SecurityException("잘못된 파일 경로입니다");
        }

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.debug("파일 삭제 완료 - path: {}", filePath);
        }
    }

    // ============= 통계 =============

    /**
     * 아동의 총 파일 크기 조회
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @return 총 파일 크기 (bytes)
     */
    public long getTotalFileSize(UUID childId, UUID userId) {
        return assetRepository.sumFileSizeByChildIdWithAuth(childId, userId);
    }
}