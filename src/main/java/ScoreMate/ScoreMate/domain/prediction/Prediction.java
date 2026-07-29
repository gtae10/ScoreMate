package ScoreMate.ScoreMate.domain.prediction;

import ScoreMate.ScoreMate.common.BaseEntity;
import ScoreMate.ScoreMate.domain.match.Match;
import ScoreMate.ScoreMate.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "predictions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "match_id"}) // 유저당 경기 1회 예측
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prediction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Match.MatchResult predictedResult;

    private Boolean correct; // 채점 전에는 null

    @Builder
    public Prediction(User user, Match match, Match.MatchResult predictedResult) {
        this.user = user;
        this.match = match;
        this.predictedResult = predictedResult;
    }

    public void grade(Match.MatchResult actualResult) {
        this.correct = this.predictedResult == actualResult;
    }
}
