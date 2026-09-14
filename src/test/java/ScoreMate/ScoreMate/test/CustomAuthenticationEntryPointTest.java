package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.security.CustomAuthenticationEntryPoint;
import ScoreMate.ScoreMate.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomAuthenticationEntryPoint가 (1) 다른 API와 같은 ApiResponse 포맷의 401을 내려주는지,
 * (2) JwtAuthFilter가 남긴 "만료" attribute에 따라 메시지가 달라지는지 확인한다.
 * 스프링 컨텍스트 없이 클래스만 직접 생성해서 빠르게 돈다.
 */
class CustomAuthenticationEntryPointTest {

    private final CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();

    @Test
    void 인증_실패시_ApiResponse_포맷의_401을_내려준다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("인증 필요"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"success\":false").contains("인증이 필요합니다.");
    }

    @Test
    void 토큰_만료로_실패한_경우_만료_메시지를_내려준다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthFilter.EXPIRED_ATTRIBUTE, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("인증 필요"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("만료");
    }
}
