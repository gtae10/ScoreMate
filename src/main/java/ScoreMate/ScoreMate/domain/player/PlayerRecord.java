package ScoreMate.ScoreMate.domain.player;

import ScoreMate.ScoreMate.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시즌 단위 선수 기록.
 * 기본 스탯(KBO Stats 시절 수준)에 더해, 타자는 Batting Leaders 2페이지에서
 * 얻을 수 있는 세부 스탯(볼넷/삼진/출루율/장타율/OPS 등)까지 확장해서 담는다.
 *
 * 타자/투수 스탯을 한 테이블에 nullable로 같이 둔 이유:
 * 선수 하나가 시즌 중 포지션 변경되는 경우는 드물고, 초기 MVP 단계에서
 * 테이블을 둘로 쪼개는 것보다 조회 쿼리를 단순하게 유지하는 게 낫다고 판단.
 * 데이터가 커지면 BatterRecord/PitcherRecord로 분리 고려.
 *
 * 투수 쪽 세부 스탯(WHIP, 탈삼진 등 Pitching Leaders 2페이지)은 1페이지와
 * 선수 구성이 달라서(정렬 기준이 달라 다른 선수 집합이 나옴) 안전하게 매칭할
 * 수 없어 이번엔 보류했다 — 타자만 확장.
 */
@Entity
@Table(
        name = "player_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "season"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int season;

    @Column(nullable = false)
    private int gamesPlayed;

    // 타자 기본 스탯
    private Double battingAverage; // 타율
    private Integer hits;
    private Integer homeRuns;
    private Integer rbi;

    // 타자 세부 스탯 (Batting Leaders 2페이지)
    private Integer walks;                  // BB, 볼넷
    private Integer intentionalWalks;       // IBB, 고의사구
    private Integer hitByPitch;             // HBP, 사구
    private Integer groundIntoDoublePlay;   // GIDP, 병살타
    private Integer errors;                 // E, 실책
    private Double stolenBasePercentage;    // SBPCT, 도루성공률 (0~1)
    private Double onBasePercentage;        // OBP, 출루율
    private Double sluggingPercentage;      // SLG, 장타율
    private Double ops;                     // OPS
    private Double runnersInScoringPositionAvg; // RISP, 득점권타율
    private Double pinchHitAvg;             // PH, 대타타율
    private Integer multiHits;              // MH, 멀티히트

    // 투수 기본 스탯
    private Double era; // 방어율
    private Integer wins;
    private Integer losses;
    private Integer saves;
    private Integer strikeouts;

    @Builder
    public PlayerRecord(Player player, int season, int gamesPlayed,
                         Double battingAverage, Integer hits, Integer homeRuns, Integer rbi,
                         Integer walks, Integer intentionalWalks, Integer hitByPitch,
                         Integer groundIntoDoublePlay, Integer errors, Double stolenBasePercentage,
                         Double onBasePercentage, Double sluggingPercentage, Double ops,
                         Double runnersInScoringPositionAvg, Double pinchHitAvg, Integer multiHits,
                         Double era, Integer wins, Integer losses, Integer saves, Integer strikeouts) {
        this.player = player;
        this.season = season;
        this.gamesPlayed = gamesPlayed;
        this.battingAverage = battingAverage;
        this.hits = hits;
        this.homeRuns = homeRuns;
        this.rbi = rbi;
        this.walks = walks;
        this.intentionalWalks = intentionalWalks;
        this.hitByPitch = hitByPitch;
        this.groundIntoDoublePlay = groundIntoDoublePlay;
        this.errors = errors;
        this.stolenBasePercentage = stolenBasePercentage;
        this.onBasePercentage = onBasePercentage;
        this.sluggingPercentage = sluggingPercentage;
        this.ops = ops;
        this.runnersInScoringPositionAvg = runnersInScoringPositionAvg;
        this.pinchHitAvg = pinchHitAvg;
        this.multiHits = multiHits;
        this.era = era;
        this.wins = wins;
        this.losses = losses;
        this.saves = saves;
        this.strikeouts = strikeouts;
    }

    /**
     * upsert 시 기존 행을 최신 값으로 갱신한다.
     */
    public void update(int gamesPlayed,
                        Double battingAverage, Integer hits, Integer homeRuns, Integer rbi,
                        Integer walks, Integer intentionalWalks, Integer hitByPitch,
                        Integer groundIntoDoublePlay, Integer errors, Double stolenBasePercentage,
                        Double onBasePercentage, Double sluggingPercentage, Double ops,
                        Double runnersInScoringPositionAvg, Double pinchHitAvg, Integer multiHits,
                        Double era, Integer wins, Integer losses, Integer saves, Integer strikeouts) {
        this.gamesPlayed = gamesPlayed;
        this.battingAverage = battingAverage;
        this.hits = hits;
        this.homeRuns = homeRuns;
        this.rbi = rbi;
        this.walks = walks;
        this.intentionalWalks = intentionalWalks;
        this.hitByPitch = hitByPitch;
        this.groundIntoDoublePlay = groundIntoDoublePlay;
        this.errors = errors;
        this.stolenBasePercentage = stolenBasePercentage;
        this.onBasePercentage = onBasePercentage;
        this.sluggingPercentage = sluggingPercentage;
        this.ops = ops;
        this.runnersInScoringPositionAvg = runnersInScoringPositionAvg;
        this.pinchHitAvg = pinchHitAvg;
        this.multiHits = multiHits;
        this.era = era;
        this.wins = wins;
        this.losses = losses;
        this.saves = saves;
        this.strikeouts = strikeouts;
    }
}
