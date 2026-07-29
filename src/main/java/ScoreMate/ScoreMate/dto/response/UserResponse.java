package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.user.User;

public record UserResponse(
        Long id,
        String username,
        String email,
        int points,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPoints(),
                user.getRole().name()
        );
    }
}
