package ScoreMate.ScoreMate.domain.player;

import ScoreMate.ScoreMate.domain.team.Team;
import ScoreMate.ScoreMate.domain.team.TeamRepository;
import ScoreMate.ScoreMate.dto.response.PlayerRecordResponse;
import ScoreMate.ScoreMate.dto.response.PlayerResponse;
import ScoreMate.ScoreMate.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerRecordRepository playerRecordRepository;
    private final TeamRepository teamRepository;

    public List<PlayerResponse> search(String name) {
        return playerRepository.findByNameContaining(name).stream()
                .map(PlayerResponse::from)
                .toList();
    }

    public List<PlayerResponse> getByTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException("팀을 찾을 수 없습니다."));
        return playerRepository.findByTeam(team).stream()
                .map(PlayerResponse::from)
                .toList();
    }

    public PlayerResponse getPlayer(Long playerId) {
        Player player = getPlayerOrThrow(playerId);
        return PlayerResponse.from(player);
    }

    public List<PlayerRecordResponse> getPlayerRecords(Long playerId) {
        Player player = getPlayerOrThrow(playerId);
        return playerRecordRepository.findByPlayer(player).stream()
                .map(PlayerRecordResponse::from)
                .toList();
    }

    private Player getPlayerOrThrow(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new CustomException("선수를 찾을 수 없습니다."));
    }
}
