package ScoreMate.ScoreMate.domain.match;

import ScoreMate.ScoreMate.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private League league;

    @Column(nullable = false, length = 50)
    private String homeTeam;

    @Column(nullable = false, length = 50)
    private String awayTeam;

    @Column(nullable = false)
    private LocalDateTime matchDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    private Integer homeScore;

    private Integer awayScore;

    // 크롤링 소스에서의 원본 식별자 (중복 수집 방지용)
    @Column(unique = true, length = 100)
    private String externalId;

    @Builder
    public Match(League league, String homeTeam, String awayTeam, LocalDateTime matchDate, String externalId) {
        this.league = league;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.matchDate = matchDate;
        this.externalId = externalId;
        this.status = MatchStatus.SCHEDULED;
    }

    public void updateResult(int homeScore, int awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = MatchStatus.FINISHED;
    }

    public void markCancelled() {
        this.status = MatchStatus.CANCELLED;
    }

    public MatchResult getResult() {
        if (status != MatchStatus.FINISHED) {
            return null;
        }
        if (homeScore > awayScore) return MatchResult.HOME_WIN;
        if (homeScore < awayScore) return MatchResult.AWAY_WIN;
        return MatchResult.DRAW;
    }

    public enum MatchStatus {
        SCHEDULED, LIVE, FINISHED, CANCELLED
    }

    public enum MatchResult {
        HOME_WIN, AWAY_WIN, DRAW
    }
}
