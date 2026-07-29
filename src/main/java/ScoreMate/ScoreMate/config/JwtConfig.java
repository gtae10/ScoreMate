package ScoreMate.ScoreMate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * application.yml의 jwt.* 값을 바인딩.
 * 예)
 * jwt:
 *   secret: change-this-secret-in-application-yml-min-32-bytes
 *   expiration-ms: 3600000
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret;
    private long expirationMs = 3_600_000L; // 기본 1시간

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
