package ScoreMate.ScoreMate.domain.team;

import ScoreMate.ScoreMate.domain.match.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandingRepository extends JpaRepository<Standing, Long> {

    Optional<Standing> findByTeamAndSeason(Team team, int season);

    List<Standing> findByTeam_LeagueAndSeasonOrderByRankAsc(League league, int season);
}
