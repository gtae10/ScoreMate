package ScoreMate.ScoreMate.domain.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRecordRepository extends JpaRepository<PlayerRecord, Long> {

    Optional<PlayerRecord> findByPlayerAndSeason(Player player, int season);

    List<PlayerRecord> findByPlayer(Player player);
}
