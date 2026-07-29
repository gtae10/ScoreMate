package ScoreMate.ScoreMate.domain.meetup;

import ScoreMate.ScoreMate.common.ApiResponse;
import ScoreMate.ScoreMate.dto.request.MeetupCreateRequest;
import ScoreMate.ScoreMate.dto.response.MeetupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetups")
@RequiredArgsConstructor
public class MeetupController {

    private final MeetupService meetupService;

    // TODO: 인증 붙인 뒤 @AuthenticationPrincipal 등으로 로그인 유저 id 꺼내오도록 교체
    @PostMapping
    public ApiResponse<MeetupResponse> create(@RequestParam Long hostId, @Valid @RequestBody MeetupCreateRequest request) {
        return ApiResponse.success(meetupService.create(hostId, request));
    }

    @PostMapping("/{meetupId}/join")
    public ApiResponse<Void> join(@PathVariable Long meetupId, @RequestParam Long userId) {
        meetupService.join(meetupId, userId);
        return ApiResponse.success("참여 신청이 완료되었습니다.", null);
    }

    @GetMapping
    public ApiResponse<List<MeetupResponse>> getOpenMeetups() {
        return ApiResponse.success(meetupService.getOpenMeetups());
    }
}
