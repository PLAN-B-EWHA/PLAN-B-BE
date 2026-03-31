package myexpressionfriend_api.security.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.common.config.JWTProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JWTUtil {

    private final SecretKey secretKey;

    public JWTUtil(JWTProperties jwtProperties) {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Map<String, Object> valueMap, int minutes) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expiration = now.plusMinutes(minutes);

        String token = Jwts.builder()
                .setClaims(valueMap)
                .setIssuedAt(Date.from(now.toInstant()))
                .setExpiration(Date.from(expiration.toInstant()))
                .signWith(secretKey)
                .compact();

        log.debug("JWT generated. expiresAt={}", expiration);
        return token;
    }

    public Claims validateToken(String token) throws CustomJWTException {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.debug("JWT validation success");
            return claims;

        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
            throw new CustomJWTException("MalFormed");

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            throw new CustomJWTException("Expired");

        } catch (InvalidClaimException e) {
            log.warn("JWT invalid claim: {}", e.getMessage());
            throw new CustomJWTException("Invalid");

        } catch (JwtException e) {
            log.warn("JWT error: {}", e.getMessage());
            throw new CustomJWTException("JWTError");

        } catch (Exception e) {
            log.error("Unexpected JWT error: {}", e.getMessage());
            throw new CustomJWTException("Error");
        }
    }
}
