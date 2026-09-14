package ScoreMate.ScoreMate.security;

import ScoreMate.ScoreMate.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    /**
     * 토큰을 딱 한 번만 파싱해서 성공하면 Claims를, 실패하면 빈 Optional을 반환한다.
     * 예전엔 isValid()+getUsername()을 필터에서 각각 호출해서 매 요청마다 파싱이 두 번 일어났는데,
     * 이 메서드 하나로 합쳐서 호출 측(JwtAuthFilter)이 파싱 결과를 그대로 재사용하게 한다.
     *
     * 실패 원인은 종류별로 구분해서 로그를 남긴다 - 만료는 흔히 있는 정상적인 상황이라 debug,
     * 서명 위조/형식 오류처럼 정상적으로는 발생하면 안 되는 경우는 warn.
     */
    public Optional<Claims> parseClaimsIfValid(String token) {
        try {
            return Optional.of(parseClaims(token));
        } catch (ExpiredJwtException e) {
            log.debug("JWT 만료 - {}", e.getMessage());
        } catch (SignatureException | MalformedJwtException e) {
            log.warn("JWT 서명/형식이 올바르지 않음 - {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 파싱 실패 - {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * parseClaimsIfValid()가 실패했을 때(드문 경로)만 호출해서 "만료라서 실패했는지"를 따로
     * 확인한다. 프론트가 "재로그인 필요(만료)"와 "잘못된 토큰"을 구분해서 안내할 수 있도록
     * JwtAuthFilter가 이 결과를 request attribute로 남겨 CustomAuthenticationEntryPoint에 전달한다.
     */
    public boolean isExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
