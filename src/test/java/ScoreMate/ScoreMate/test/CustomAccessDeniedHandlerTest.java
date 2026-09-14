package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.security.CustomAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomAccessDeniedHandler가 다른 API와 같은 ApiResponse 포맷의 403을 내려주는지 확인한다.
 * 스프링 컨텍스트 없이 클래스만 직접 생성해서 빠르게 돈다.
 */
class CustomAccessDeniedHandlerTest {

    private final CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();

    @Test
    void 권한_부족시_ApiResponse_포맷의_403을_내려준다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("권한 없음"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"success\":false").contains("접근 권한이 없습니다.");
    }
}
