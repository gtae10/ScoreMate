package ScoreMate.ScoreMate.domain.prediction;

import ScoreMate.ScoreMate.domain.match.Match;
import ScoreMate.ScoreMate.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findByUserAndMatch(User user, Match match);

    boolean existsByUserAndMatch(User user, Match match);

    List<Prediction> findByUser(User user);

    List<Prediction> findByMatchAndCorrectIsNull(Match match);
}
