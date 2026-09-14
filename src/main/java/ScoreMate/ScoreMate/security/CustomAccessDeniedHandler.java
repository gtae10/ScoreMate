package ScoreMate.ScoreMate.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 됐지만 권한이 없는 요청이 거부됐을 때 호출된다. CustomAuthenticationEntryPoint와
 * 같은 이유(필터 체인 단계, Jackson 2/3 버전 충돌 회피)로 ApiResponse 포맷의 403 응답을
 * ObjectMapper 없이 직접 조립해서 내려준다.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final String MESSAGE = "접근 권한이 없습니다.";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + MESSAGE + "\",\"data\":null}");
    }
}
