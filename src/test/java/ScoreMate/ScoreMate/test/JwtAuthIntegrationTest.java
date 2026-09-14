package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.config.JwtConfig;
import ScoreMate.ScoreMate.domain.user.User;
import ScoreMate.ScoreMate.domain.user.UserRepository;
import ScoreMate.ScoreMate.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfig에 실제로 등록된 JwtAuthFilter + CustomAuthenticationEntryPoint 조합이
 * /api/users/{id}(authenticated() 구간)에 대해 시나리오별로 올바르게 동작하는지 확인한다.
 * 단위 테스트(JwtTokenProviderTest, CustomAuthenticationEntryPointTest)가 각 조각을 검증했다면,
 * 이 테스트는 SecurityConfig 배선이 실제로 맞물려 돌아가는지를 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @AfterEach
    void cleanup() {
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void 토큰이_없으면_401과_ApiResponse_포맷으로_응답한다() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void 형식이_깨진_토큰은_401과_기본_메시지를_응답한다() throws Exception {
        mockMvc.perform(get("/api/users/1").header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void 만료된_토큰은_401과_만료_메시지를_응답한다() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("아무개")
                .issuedAt(new Date(System.currentTimeMillis() - 20_000))
                .expiration(new Date(System.currentTimeMillis() - 10_000))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/users/1").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("토큰이 만료되었습니다. 다시 로그인해주세요."));
    }

    @Test
    void 유효한_토큰이면_인증되어_컨트롤러까지_도달한다() throws Exception {
        String username = "jwt-test-" + System.nanoTime();
        User user = userRepository.save(User.builder()
                .username(username)
                .password("irrelevant-for-this-test")
                .email(username + "@example.com")
                .build());
        userId = user.getId();

        String token = jwtTokenProvider.createToken(username);

        // 인증 자체는 통과해서 컨트롤러(UserController.getMyInfo)까지 도달함을 확인한다 -
        // 401이 아니라는 것 자체가 JwtAuthFilter가 정상적으로 인증 처리했다는 증거다.
        mockMvc.perform(get("/api/users/{id}", userId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
