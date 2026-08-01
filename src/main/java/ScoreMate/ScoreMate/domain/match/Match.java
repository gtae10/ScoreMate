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

    // 진행중일 때의 실제 이닝 텍스트 (예: "5회말"). LIVE 상태가 아니면 null.
    @Column(length = 20)
    private String liveInning;

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
        this.liveInning = null;
    }

    /**
     * 진행 중인 경기의 실시간 스코어 + 이닝 텍스트를 반영한다. 이미 FINISHED로 확정된
     * 경기는 (크롤링 순서가 꼬여서 뒤늦게 LIVE로 되돌리는 일이 없도록) 건드리지 않는다.
     */
    public void markLive(int homeScore, int awayScore, String liveInning) {
        if (this.status == MatchStatus.FINISHED) {
            return;
        }
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = MatchStatus.LIVE;
        this.liveInning = liveInning;
    }

    public void markCancelled() {
        this.status = MatchStatus.CANCELLED;
    }

    public void markPostponed() {
        this.status = MatchStatus.POSTPONED;
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
        SCHEDULED, LIVE, FINISHED, CANCELLED, POSTPONED
    }

    public enum MatchResult {
        HOME_WIN, AWAY_WIN, DRAW
    }
}
