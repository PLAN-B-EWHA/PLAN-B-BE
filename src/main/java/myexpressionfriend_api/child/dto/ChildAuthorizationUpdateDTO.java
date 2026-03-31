package myexpressionfriend_api.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.child.domain.ChildPermissionType;

import java.util.Set;

/**
 * 아동 접근 권한 수정 요청 DTO
 *
 * <p>주보호자 변경은 별도 API(transferPrimaryParent)로 처리</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildAuthorizationUpdateDTO {

    /**
     * 변경할 권한 목록 (전체 교체 방식)
     */
    private Set<ChildPermissionType> permissions;

    /**
     * 권한 활성화 여부
     */
    private Boolean isActive;
}
