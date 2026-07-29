package ScoreMate.ScoreMate.domain.user;

import ScoreMate.ScoreMate.common.ApiResponse;
import ScoreMate.ScoreMate.dto.request.UserSignUpRequest;
import ScoreMate.ScoreMate.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ApiResponse<UserResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        return ApiResponse.success("회원가입이 완료되었습니다.", userService.signUp(request));
    }

    // TODO: 인증 붙인 뒤 SecurityContext에서 로그인 유저 id 꺼내오도록 교체
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getMyInfo(@PathVariable Long userId) {
        return ApiResponse.success(userService.getMyInfo(userId));
    }
}
