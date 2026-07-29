package ScoreMate.ScoreMate.domain.team;

import ScoreMate.ScoreMate.common.ApiResponse;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.dto.response.StandingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/standings")
@RequiredArgsConstructor
public class StandingController {

    private final StandingService standingService;

    @GetMapping
    public ApiResponse<List<StandingResponse>> getStandings(
            @RequestParam League league,
            @RequestParam(required = false) Integer season
    ) {
        int targetSeason = season != null ? season : Year.now().getValue();
        return ApiResponse.success(standingService.getStandings(league, targetSeason));
    }
}
