package myexpressionfriend_api.child.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.dto.ChildDetailResponseDTO;
import myexpressionfriend_api.child.dto.ChildResponseDTO;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 아동 조회 서비스 (Read-only)
 *
 * <p>목록/상세 조회는 이 서비스에서 담당하고,
 * 생성/수정/삭제는 ChildService에서 담당</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildQueryService {

    private final ChildRepository childRepository;

    /**
     * 아동 상세 조회 (권한 사용자 목록 포함)
     * - 접근 권한이 없는 사용자는 조회 불가
     */
    public ChildDetailResponseDTO getChildDetail(UUID childId, UUID requestUserId) {
        log.info("아동 상세 조회 - childId: {}, requestUserId: {}", childId, requestUserId);

        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        if (!child.canAccess(requestUserId)) {
            throw new InvalidRequestException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        return ChildDetailResponseDTO.from(child);
    }

    /**
     * 내가 접근 가능한 아동 목록 조회 (주보호자 + 권한 부여된 사용자)
     */
    public List<ChildResponseDTO> getAccessibleChildren(UUID userId) {
        log.info("접근 가능한 아동 목록 조회 - userId: {}", userId);

        return childRepository.findAccessibleByUserId(userId).stream()
                .map(child -> ChildResponseDTO.from(child, userId))
                .toList();
    }

    /**
     * 내가 주보호자인 아동 목록 조회
     */
    public List<ChildResponseDTO> getMyPrimaryChildren(UUID userId) {
        log.info("주보호자 아동 목록 조회 - userId: {}", userId);

        return childRepository.findByPrimaryParentUserId(userId).stream()
                .map(child -> ChildResponseDTO.from(child, userId))
                .toList();
    }

    /**
     * PLAY_GAME 권한을 가진 아동 목록 조회
     */
    public List<ChildResponseDTO> getPlayableChildren(UUID userId) {
        log.info("게임 가능한 아동 목록 조회 - userId: {}", userId);

        return childRepository.findAccessibleByUserId(userId).stream()
                .filter(child -> child.hasPermission(userId, ChildPermissionType.PLAY_GAME))
                .map(child -> ChildResponseDTO.from(child, userId))
                .toList();
    }

    /**
     * 아동 단건 조회 (권한 체크 없이 내부 사용용)
     * - 다른 서비스에서 호출할 때 사용
     */
    public Child getChildOrThrow(UUID childId) {
        return childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));
    }
}
