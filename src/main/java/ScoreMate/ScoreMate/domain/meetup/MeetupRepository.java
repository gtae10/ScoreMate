package ScoreMate.ScoreMate.domain.meetup;

import ScoreMate.ScoreMate.domain.match.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetupRepository extends JpaRepository<Meetup, Long> {

    List<Meetup> findByMatch(Match match);

    List<Meetup> findByStatus(Meetup.MeetupStatus status);
}
