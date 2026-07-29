package ScoreMate.ScoreMate.domain.match;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Match> findByLeagueAndMatchDateBetween(League league, LocalDateTime start, LocalDateTime end);

    List<Match> findByStatus(Match.MatchStatus status);

    default List<Match> findByLeagueAndDate(League league, LocalDate date) {
        return findByLeagueAndMatchDateBetween(league, date.atStartOfDay(), date.atTime(23, 59, 59));
    }
}
