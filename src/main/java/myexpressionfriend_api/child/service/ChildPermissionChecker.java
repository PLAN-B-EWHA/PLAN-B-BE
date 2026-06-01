package myexpressionfriend_api.child.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.exception.AuthenticationFailedException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChildPermissionChecker {

    private final ChildRepository childRepository;

    public Child checkViewReport(UUID userId, UUID childId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동 정보를 찾을 수 없습니다."));
        if (!child.hasPermission(userId, ChildPermissionType.VIEW_REPORT)) {
            throw new AuthenticationFailedException("리포트 조회 권한이 없습니다.");
        }
        return child;
    }

    public Child checkAccess(UUID userId, UUID childId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("?꾨룞 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎."));
        if (!child.canAccess(userId)) {
            throw new AuthenticationFailedException("?대떦 ?꾨룞???묎렐 沅뚰븳???놁뒿?덈떎.");
        }
        return child;
    }
}
