package ScoreMate.ScoreMate.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MeetupCreateRequest(

        @NotNull(message = "경기 id는 필수입니다.")
        Long matchId,

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "장소는 필수입니다.")
        String location,

        @Min(value = 2, message = "최소 인원은 2명 이상이어야 합니다.")
        int maxParticipants
) {
}
