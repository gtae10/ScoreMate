package ScoreMate.ScoreMate.domain.player;

import ScoreMate.ScoreMate.common.ApiResponse;
import ScoreMate.ScoreMate.dto.response.PlayerRecordResponse;
import ScoreMate.ScoreMate.dto.response.PlayerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping("/search")
    public ApiResponse<List<PlayerResponse>> search(@RequestParam String name) {
        return ApiResponse.success(playerService.search(name));
    }

    @GetMapping
    public ApiResponse<List<PlayerResponse>> getByTeam(@RequestParam Long teamId) {
        return ApiResponse.success(playerService.getByTeam(teamId));
    }

    @GetMapping("/{playerId}")
    public ApiResponse<PlayerResponse> getPlayer(@PathVariable Long playerId) {
        return ApiResponse.success(playerService.getPlayer(playerId));
    }

    @GetMapping("/{playerId}/records")
    public ApiResponse<List<PlayerRecordResponse>> getRecords(@PathVariable Long playerId) {
        return ApiResponse.success(playerService.getPlayerRecords(playerId));
    }
}
