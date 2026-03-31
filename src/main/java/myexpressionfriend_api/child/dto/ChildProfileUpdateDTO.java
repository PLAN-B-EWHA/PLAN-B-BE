package myexpressionfriend_api.child.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 아동 프로필 부분 수정 요청 DTO
 * - 진단 관련 정보를 제외한 기본 프로필 필드만 수정
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildProfileUpdateDTO {

    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "성별은 MALE, FEMALE, OTHER 중 하나여야 합니다.")
    private String gender;

    private String interests;

    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하이어야 합니다.")
    private String profileImageUrl;
}
