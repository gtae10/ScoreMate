package ScoreMate.ScoreMate.domain.match;

import ScoreMate.ScoreMate.common.ApiResponse;
import ScoreMate.ScoreMate.dto.response.MatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    public ApiResponse<List<MatchResponse>> getMatches(
            @RequestParam League league,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(matchService.getMatchesByLeagueAndDate(league, date));
    }

    @GetMapping("/all")
    public ApiResponse<List<MatchResponse>> getAllMatches() {
        return ApiResponse.success(matchService.getAllMatches());
    }
}
