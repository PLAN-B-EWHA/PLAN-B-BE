package com.planB.myexpressionfriend.common.util;

import com.planB.myexpressionfriend.common.config.JWTProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final JWTProperties jwtProperties;

    public JWTUtil(JWTProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * JWT 토큰 생성
     * @param valueMap 토큰에 저장할 데이터 (claims)
     * @param  minutes 토큰 유효 시간 (분)
     * @return JWT 토큰 문자열
     */
    public String generateToken(Map<String, Object> valueMap, int minutes){

        log.info("============= JWT 토큰 생성 시작 =============");
        log.info("Claims: {}", valueMap);
        log.info("유효 시간: {}분", minutes);

        // 헤더
        Map<String, Object> headers = Map.of(
                "typ", "JWT",
                "alg", "HS256"
        );

        // 토큰 만료 시간 계산
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expiration = now.plusMinutes(minutes);

        String token = Jwts.builder()
                .setHeader(headers)
                .setClaims(valueMap)
                .setIssuedAt(Date.from(now.toInstant()))
                .setExpiration(Date.from(expiration.toInstant()))
                .signWith(secretKey)
                .compact();

        log.info("JWT 토큰 생성 완료");
        log.info("만료 시간: {}", expiration);

        return token;
    }


    /**
     * JWT 토큰 검증 및 Claims 추출
     * @param token JWT 토큰
     * @return Claims (토큰에 저장된 데이터)
     * @throws CustomJWTException 토큰이 유효하지 않을 때
     */
    public Map<String, Object> validateToken(String token) throws CustomJWTException{

        log.info("============= JWT 토큰 검증 시작 =============");

        Map<String, Object> claims = null;

        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.info("JWT 토큰 검증 성공");
            log.info("Claims: {}", claims);

        } catch (MalformedJwtException e) {
            log.error("JWT 형식이 잘못되었습니다: {}", e.getMessage());
            throw new CustomJWTException("MalFormed");

        } catch (ExpiredJwtException e) {
            log.error("JWT 토큰이 만료되었습니다: {}", e.getMessage());
            throw new CustomJWTException("Expired");

        } catch (InvalidClaimException e) {
            log.error("JWT Claim이 유효하지 않습니다: {}", e.getMessage());
            throw new CustomJWTException("Invalid");

        } catch (JwtException e) {
            log.error("JWT 에러: {}", e.getMessage());
            throw new CustomJWTException("JWTError");

        } catch (Exception e) {
            log.error("알 수 없는 에러: {}", e.getMessage());
            throw new CustomJWTException("Error");
        }

        return claims;
    }
}
