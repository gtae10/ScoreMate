package ScoreMate.ScoreMate.domain.match;

import ScoreMate.ScoreMate.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    @Column(length = 30)
    private String stadium;

    @Column(length = 30)
    private String winPitcher;

    @Column(length = 30)
    private String losePitcher;

    // 진짜 "선발투수" (경기 전 예고된 선발) - 승/패 결정투수랑 다른 개념
    @Column(length = 30)
    private String awayStartingPitcher;

    @Column(length = 30)
    private String homeStartingPitcher;

    // 지금 이닝에 실제로 던지고 있는 투수 (문자중계 페이지에서만 얻을 수 있음)
    @Column(length = 30)
    private String awayCurrentPitcher;

    @Column(length = 30)
    private String homeCurrentPitcher;

    @Column(length = 30)
    private String currentBatter;

    // 최근 문자중계 몇 줄 (줄바꿈으로 구분, 최신이 맨 앞)
    @Column(length = 2000)
    private String liveTextFeed;

    // 이닝별 점수 (쉼표로 구분, 예: "0,0,2,1,0,0,0,0,0" — 아직 안 온 이닝은 "-")
    @Column(length = 60)
    private String awayInnings;

    @Column(length = 60)
    private String homeInnings;

    private Integer awayHits;
    private Integer homeHits;
    private Integer awayErrors;
    private Integer homeErrors;

    // 취소/연기 사유 원문 (예: "우천취소", "폭염취소")
    @Column(length = 30)
    private String cancelReason;

    // --- 라이브박스 상태 (LiveBoxCrawler, LiveTextView2.aspx ".economy" 블록) ---
    // 이닝/타석타자는 기존 liveInning/currentBatter를 그대로 재사용한다.
    private boolean runnerOnFirst;
    private boolean runnerOnSecond;
    private boolean runnerOnThird;

    private Integer ballCount;
    private Integer strikeCount;
    private Integer outCount;

    @Column(length = 30)
    private String livePitcher;
    @Column(length = 30)
    private String liveCatcher;
    @Column(length = 30)
    private String liveFirstBase;
    @Column(length = 30)
    private String liveSecondBase;
    @Column(length = 30)
    private String liveThirdBase;
    @Column(length = 30)
    private String liveShortstop;
    @Column(length = 30)
    private String liveLeftFielder;
    @Column(length = 30)
    private String liveCenterFielder;
    @Column(length = 30)
    private String liveRightFielder;

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

    public void updateStadium(String stadium) {
        if (stadium != null && !stadium.isBlank()) {
            this.stadium = stadium;
        }
    }

    public void updatePitchers(String winPitcher, String losePitcher) {
        if (winPitcher != null && !winPitcher.isBlank()) {
            this.winPitcher = winPitcher;
        }
        if (losePitcher != null && !losePitcher.isBlank()) {
            this.losePitcher = losePitcher;
        }
    }

    public void updateStartingPitchers(String awayStartingPitcher, String homeStartingPitcher) {
        if (awayStartingPitcher != null && !awayStartingPitcher.isBlank()) {
            this.awayStartingPitcher = awayStartingPitcher;
        }
        if (homeStartingPitcher != null && !homeStartingPitcher.isBlank()) {
            this.homeStartingPitcher = homeStartingPitcher;
        }
    }

    public void updateCurrentPitchers(String awayCurrentPitcher, String homeCurrentPitcher) {
        // 지금 던지는 팀 쪽만 값이 오고, 타격 중인 팀 쪽은 null로 온다 — null이어도
        // "지금 안 던진다"는 유효한 정보라 예전 값 유지 대신 그대로 반영한다.
        this.awayCurrentPitcher = awayCurrentPitcher;
        this.homeCurrentPitcher = homeCurrentPitcher;
    }

    public void updateLiveState(String currentBatter, List<String> recentPlays) {
        this.currentBatter = currentBatter;
        if (recentPlays != null && !recentPlays.isEmpty()) {
            this.liveTextFeed = String.join("\n", recentPlays);
        }
    }

    /**
     * LiveBoxCrawler가 매번 ".economy" 블록 전체를 새로 파싱해서 던져주는 "지금 이 순간" 스냅샷이라,
     * updateCurrentPitchers와 같은 이유로 null/false도 유효한 값으로 보고 그대로 덮어쓴다.
     * liveInning만은 ScoreBoardCrawler가 채운 값을 파싱 실패로 지워버리지 않도록 빈 값이면 건드리지 않는다.
     */
    public void updateLiveBox(String inningText, String currentBatter,
                               boolean runnerOnFirst, boolean runnerOnSecond, boolean runnerOnThird,
                               Integer ballCount, Integer strikeCount, Integer outCount,
                               String pitcher, String catcher, String firstBase, String secondBase,
                               String thirdBase, String shortstop, String leftFielder,
                               String centerFielder, String rightFielder) {
        if (inningText != null && !inningText.isBlank()) {
            this.liveInning = inningText;
        }
        this.currentBatter = currentBatter;
        this.runnerOnFirst = runnerOnFirst;
        this.runnerOnSecond = runnerOnSecond;
        this.runnerOnThird = runnerOnThird;
        this.ballCount = ballCount;
        this.strikeCount = strikeCount;
        this.outCount = outCount;
        this.livePitcher = pitcher;
        this.liveCatcher = catcher;
        this.liveFirstBase = firstBase;
        this.liveSecondBase = secondBase;
        this.liveThirdBase = thirdBase;
        this.liveShortstop = shortstop;
        this.liveLeftFielder = leftFielder;
        this.liveCenterFielder = centerFielder;
        this.liveRightFielder = rightFielder;
    }

    public void updateLineScore(String awayInnings, String homeInnings,
                                 Integer awayHits, Integer homeHits,
                                 Integer awayErrors, Integer homeErrors) {
        if (awayInnings != null && !awayInnings.isBlank()) {
            this.awayInnings = awayInnings;
        }
        if (homeInnings != null && !homeInnings.isBlank()) {
            this.homeInnings = homeInnings;
        }
        if (awayHits != null) this.awayHits = awayHits;
        if (homeHits != null) this.homeHits = homeHits;
        if (awayErrors != null) this.awayErrors = awayErrors;
        if (homeErrors != null) this.homeErrors = homeErrors;
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

    public void markCancelled(String reason) {
        this.status = MatchStatus.CANCELLED;
        if (reason != null && !reason.isBlank()) {
            this.cancelReason = reason;
        }
    }

    public void markPostponed(String reason) {
        this.status = MatchStatus.POSTPONED;
        if (reason != null && !reason.isBlank()) {
            this.cancelReason = reason;
        }
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
