package ScoreMate.ScoreMate.domain.player;

import ScoreMate.ScoreMate.domain.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Player> findByTeam(Team team);

    List<Player> findByNameContaining(String name);
}
