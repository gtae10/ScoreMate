package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.config.JwtConfig;
import ScoreMate.ScoreMate.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider.parseClaimsIfValid()/isExpired()가 정상/만료/서명위조/형식오류 토큰을
 * 제대로 구분하는지 확인한다 (isValid()+getUsername() 이중 파싱을 하나로 합친 리팩터링의 회귀 테스트).
 * 스프링 컨텍스트 없이 클래스만 직접 생성해서 빠르게 돈다.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-jwt-must-be-at-least-32-bytes-long";

    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtTokenProvider jwtTokenProvider;

    JwtTokenProviderTest() {
        jwtConfig.setSecret(SECRET);
        jwtConfig.setExpirationMs(3_600_000L);
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
    }

    @Test
    void 유효한_토큰은_Claims를_반환한다() {
        String token = jwtTokenProvider.createToken("gamejoo");

        Optional<Claims> claims = jwtTokenProvider.parseClaimsIfValid(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("gamejoo");
        assertThat(jwtTokenProvider.isExpired(token)).isFalse();
    }

    @Test
    void 만료된_토큰은_빈_Optional이고_isExpired는_true다() {
        String expiredToken = buildToken(new Date(System.currentTimeMillis() - 10_000), "gamejoo");

        assertThat(jwtTokenProvider.parseClaimsIfValid(expiredToken)).isEmpty();
        assertThat(jwtTokenProvider.isExpired(expiredToken)).isTrue();
    }

    @Test
    void 서명이_다른_토큰은_빈_Optional이고_isExpired는_false다() {
        SecretKey otherKey = Keys.hmacShaKeyFor("another-secret-key-that-is-also-32-bytes-plus".getBytes(StandardCharsets.UTF_8));
        String tokenSignedByOtherKey = Jwts.builder()
                .subject("gamejoo")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(otherKey)
                .compact();

        assertThat(jwtTokenProvider.parseClaimsIfValid(tokenSignedByOtherKey)).isEmpty();
        assertThat(jwtTokenProvider.isExpired(tokenSignedByOtherKey)).isFalse();
    }

    @Test
    void 형식이_깨진_토큰은_빈_Optional이고_isExpired는_false다() {
        assertThat(jwtTokenProvider.parseClaimsIfValid("this-is-not-a-jwt")).isEmpty();
        assertThat(jwtTokenProvider.isExpired("this-is-not-a-jwt")).isFalse();
    }

    private String buildToken(Date expiry, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(expiry.getTime() - 1))
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
