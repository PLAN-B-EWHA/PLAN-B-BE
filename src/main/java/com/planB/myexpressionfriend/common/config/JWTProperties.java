package com.planB.myexpressionfriend.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JWTProperties {

    private String secret;
    private int accessTokenExpireMinutes;
    private int refreshTokenExpireMinutes;

    private Cookie cookie = new Cookie();

    @Getter
    @Setter
    public static class Cookie {
        private String name;
        private int maxAge;
        private String path;
        private boolean secure;
        private boolean httpOnly;
        private String sameSite;
    }
}
