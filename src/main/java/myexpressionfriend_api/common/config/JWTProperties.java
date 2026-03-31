package myexpressionfriend_api.common.config;

import jakarta.servlet.http.Cookie;
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

    private CookieProperties cookie = new CookieProperties();

    @Getter
    @Setter
    public static class CookieProperties {
        private String name;
        private int maxAge;
        private String path;
        private boolean secure;
        private boolean httpOnly;
        private String sameSite;
    }
}
