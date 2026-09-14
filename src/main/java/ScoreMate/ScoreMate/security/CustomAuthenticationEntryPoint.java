package ScoreMate.ScoreMate.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청이 보호된 경로에 접근했을 때(=SecurityConfig의 authenticated() 구간) 호출된다.
 * 필터 체인 단계라 @RestControllerAdvice(GlobalExceptionHandler)가 관여할 수 없어서, 다른 API와
 * 동일한 ApiResponse(success/message/data) 포맷의 401 응답을 여기서 직접 만들어 내려준다.
 *
 * ObjectMapper를 굳이 주입받지 않고 문자열을 직접 조립한다 - 응답 모양이 고정 필드 3개짜리로
 * 단순하고, message는 아래 두 상수 중 하나만 들어가서 이스케이프를 걱정할 값이 아니기 때문.
 * (Spring Boot 4가 내부적으로 Jackson 3(tools.jackson.*)의 ObjectMapper 빈을 등록해서,
 * 프로젝트에서 쓰는 Jackson 2(com.fasterxml.jackson.*) ObjectMapper와 타입이 안 맞아 주입이 안 된다 -
 * 이 문제를 피하는 목적도 있다.)
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String EXPIRED_MESSAGE = "토큰이 만료되었습니다. 다시 로그인해주세요.";
    private static final String DEFAULT_MESSAGE = "인증이 필요합니다.";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        boolean expired = Boolean.TRUE.equals(request.getAttribute(JwtAuthFilter.EXPIRED_ATTRIBUTE));
        String message = expired ? EXPIRED_MESSAGE : DEFAULT_MESSAGE;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}
