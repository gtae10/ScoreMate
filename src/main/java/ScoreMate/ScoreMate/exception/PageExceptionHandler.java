package ScoreMate.ScoreMate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 화면(Thymeleaf) 렌더링 컨트롤러(web 패키지) 전용 예외 핸들러.
 * API 컨트롤러는 GlobalExceptionHandler(JSON)가, 페이지 컨트롤러는 이 핸들러(HTML)가 담당한다.
 */
@ControllerAdvice(basePackages = "ScoreMate.ScoreMate.web")
public class PageExceptionHandler {

    @ExceptionHandler(CustomException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleCustomException(CustomException e, Model model) {
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e, Model model) {
        model.addAttribute("message", "페이지를 표시하는 중 오류가 발생했습니다.");
        return "error";
    }
}