package ScoreMate.ScoreMate.domain.team;

import ScoreMate.ScoreMate.common.ApiResponse;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.dto.response.TeamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ApiResponse<List<TeamResponse>> getTeams(@RequestParam League league) {
        return ApiResponse.success(teamService.getTeamsByLeague(league));
    }

    @GetMapping("/{teamId}")
    public ApiResponse<TeamResponse> getTeam(@PathVariable Long teamId) {
        return ApiResponse.success(teamService.getTeam(teamId));
    }
}
