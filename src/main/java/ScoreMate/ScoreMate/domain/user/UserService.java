package ScoreMate.ScoreMate.domain.user;

import ScoreMate.ScoreMate.dto.request.UserSignUpRequest;
import ScoreMate.ScoreMate.dto.response.UserResponse;
import ScoreMate.ScoreMate.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse signUp(UserSignUpRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new CustomException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role(User.Role.USER)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }
}
