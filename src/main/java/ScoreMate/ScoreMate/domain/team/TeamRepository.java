package ScoreMate.ScoreMate.domain.team;

import ScoreMate.ScoreMate.domain.match.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Team> findByLeague(League league);
}
