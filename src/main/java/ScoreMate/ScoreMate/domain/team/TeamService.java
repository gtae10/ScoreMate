package ScoreMate.ScoreMate.domain.team;

import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.dto.response.TeamResponse;
import ScoreMate.ScoreMate.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;

    public List<TeamResponse> getTeamsByLeague(League league) {
        return teamRepository.findByLeague(league).stream()
                .map(TeamResponse::from)
                .toList();
    }

    public TeamResponse getTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException("팀을 찾을 수 없습니다."));
        return TeamResponse.from(team);
    }
}
