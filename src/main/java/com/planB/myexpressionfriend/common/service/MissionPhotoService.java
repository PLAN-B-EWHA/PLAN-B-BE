package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.mission.AssignedMission;
import com.planB.myexpressionfriend.common.domain.mission.MissionPhoto;
import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.dto.mission.MissionPhotoDTO;
import com.planB.myexpressionfriend.common.event.MissionPhotoUploadedEvent;
import com.planB.myexpressionfriend.common.repository.AssignedMissionRepository;
import com.planB.myexpressionfriend.common.repository.MissionPhotoRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MissionPhotoService {

    private final MissionPhotoRepository missionPhotoRepository;
    private final AssignedMissionRepository assignedMissionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.storage.base-path:uploads}")
    private String storageBasePath;

    @Transactional
    public MissionPhotoDTO uploadPhoto(UUID missionId, MultipartFile file, UUID userId) {
        validateUploadFile(file);

        AssignedMission mission = assignedMissionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않습니다."));

        if (!canManageEvidence(mission, userId)) {
            throw new AccessDeniedException("미션 증빙 사진 업로드 권한이 없습니다.");
        }

        try {
            MissionPhoto photo = MissionPhoto.createPhoto(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    mission.getChild().getChildId(),
                    missionId
            );

            saveFile(file, photo.getFileUrl());
            mission.addPhoto(photo);

            MissionPhoto saved = missionPhotoRepository.save(photo);

            eventPublisher.publishEvent(new MissionPhotoUploadedEvent(
                    mission.getMissionId(),
                    saved.getPhotoId(),
                    mission.getTherapist().getUserId(),
                    userId
            ));

            return MissionPhotoDTO.from(saved);
        } catch (IOException e) {
            log.error("미션 사진 저장 실패 - missionId: {}, userId: {}", missionId, userId, e);
            throw new RuntimeException("사진 저장 중 오류가 발생했습니다.", e);
        }
    }

    public List<MissionPhotoDTO> getPhotos(UUID missionId, UUID userId) {
        return missionPhotoRepository.findByMissionIdWithAuth(missionId, userId)
                .stream()
                .map(MissionPhotoDTO::from)
                .toList();
    }

    @Transactional
    public void deletePhoto(UUID photoId, UUID userId) {
        MissionPhoto photo = missionPhotoRepository.findByIdWithAuth(photoId, userId)
                .orElseThrow(() -> new AccessDeniedException("사진 조회 권한이 없거나 존재하지 않습니다."));

        AssignedMission mission = photo.getMission();
        if (!canManageEvidence(mission, userId)) {
            throw new AccessDeniedException("미션 증빙 사진 삭제 권한이 없습니다.");
        }

        try {
            deleteFile(photo.getFileUrl());
        } catch (IOException e) {
            log.error("미션 사진 파일 삭제 실패 - photoId: {}", photoId, e);
        }

        mission.removePhoto(photo);
        missionPhotoRepository.delete(photo);
    }

    public Path getPhotoPath(UUID photoId, UUID userId) {
        MissionPhoto photo = missionPhotoRepository.findByIdWithAuth(photoId, userId)
                .orElseThrow(() -> new AccessDeniedException("사진 조회 권한이 없거나 존재하지 않습니다."));

        Path path = Paths.get(storageBasePath, photo.getFileUrl());
        if (!path.normalize().startsWith(Paths.get(storageBasePath).normalize())) {
            throw new SecurityException("잘못된 파일 경로입니다.");
        }
        if (!Files.exists(path)) {
            throw new IllegalStateException("파일이 존재하지 않습니다.");
        }
        return path;
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어 있습니다.");
        }
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }
    }

    private boolean canManageEvidence(AssignedMission mission, UUID userId) {
        boolean hasWriteNote = mission.getChild().hasPermission(userId, ChildPermissionType.WRITE_NOTE);
        MissionStatus status = mission.getStatus();
        boolean editableStatus = status == MissionStatus.IN_PROGRESS || status == MissionStatus.COMPLETED;
        return hasWriteNote && editableStatus;
    }

    private void saveFile(MultipartFile file, String relativePath) throws IOException {
        Path filePath = Paths.get(storageBasePath, relativePath);
        Files.createDirectories(filePath.getParent());

        if (!filePath.normalize().startsWith(Paths.get(storageBasePath).normalize())) {
            throw new SecurityException("잘못된 파일 경로입니다.");
        }
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
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

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(storageBasePath));
        } catch (IOException e) {
            throw new RuntimeException("스토리지 디렉터리 초기화에 실패했습니다.", e);
        }
    }
}
