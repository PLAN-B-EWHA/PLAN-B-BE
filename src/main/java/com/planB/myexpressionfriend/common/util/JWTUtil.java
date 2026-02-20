package com.planB.myexpressionfriend.common.util;

import com.planB.myexpressionfriend.common.config.JWTProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
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
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(Map<String, Object> valueMap, int minutes) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expiration = now.plusMinutes(minutes);

        String token = Jwts.builder()
                .setHeader(Map.of("typ", "JWT", "alg", "HS256"))
                .setClaims(valueMap)
                .setIssuedAt(Date.from(now.toInstant()))
                .setExpiration(Date.from(expiration.toInstant()))
                .signWith(secretKey)
                .compact();

        log.debug("JWT generated. expiresAt={}", expiration);
        return token;
    }

    public Map<String, Object> validateToken(String token) throws CustomJWTException {
        try {
            Map<String, Object> claims = Jwts.parserBuilder()
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
