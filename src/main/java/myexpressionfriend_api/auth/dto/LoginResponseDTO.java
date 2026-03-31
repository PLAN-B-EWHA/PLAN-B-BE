package myexpressionfriend_api.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDTO {

    private String accessToken;
    private String grantType;
    private long expiresIn;  // ms 단위

}
