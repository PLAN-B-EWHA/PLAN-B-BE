package myexpressionfriend_api.player.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.player.domain.GamePlayerSelection;
import myexpressionfriend_api.player.dto.GamePlayerSelectionResponseDTO;
import myexpressionfriend_api.player.repository.GamePlayerSelectionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamePlayerSelectionService {

    private final GamePlayerSelectionRepository selectionRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;

    @Transactional
    public GamePlayerSelectionResponseDTO selectChild(UUID userId, UUID childId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("Child not found"));

        if (!child.hasPermission(userId, ChildPermissionType.PLAY_GAME)) {
            throw new AccessDeniedException("You do not have PLAY_GAME permission for this child");
        }

        GamePlayerSelection selection = selectionRepository.findByUser_UserId(userId)
                .map(existing -> {
                    existing.changeChild(child);
                    return existing;
                })
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found"));
                    return GamePlayerSelection.builder()
                            .user(user)
                            .child(child)
                            .build();
                });

        GamePlayerSelection saved = selectionRepository.save(selection);
        return GamePlayerSelectionResponseDTO.from(saved);
    }

    public GamePlayerSelectionResponseDTO getSelectedChild(UUID userId) {
        GamePlayerSelection selection = selectionRepository.findByUserIdWithChild(userId)
                .orElseThrow(() -> new EntityNotFoundException("Selected game child not found"));
        return GamePlayerSelectionResponseDTO.from(selection);
    }

    public Child getSelectedPlayableChild(UUID userId) {
        GamePlayerSelection selection = selectionRepository.findByUserIdWithChild(userId)
                .orElseThrow(() -> new EntityNotFoundException("Selected game child not found"));

        Child child = selection.getChild();
        if (!child.hasPermission(userId, ChildPermissionType.PLAY_GAME)) {
            throw new AccessDeniedException("Selected child is no longer playable by this user");
        }
        return child;
    }

    public Optional<Child> findSelectedPlayableChild(UUID userId) {
        return selectionRepository.findByUserIdWithChild(userId)
                .map(GamePlayerSelection::getChild)
                .map(child -> {
                    if (!child.hasPermission(userId, ChildPermissionType.PLAY_GAME)) {
                        throw new AccessDeniedException("Selected child is no longer playable by this user");
                    }
                    return child;
                });
    }
}
