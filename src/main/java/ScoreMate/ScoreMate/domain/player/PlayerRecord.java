package ScoreMate.ScoreMate.domain.player;

import ScoreMate.ScoreMate.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시즌 단위 선수 기록.
 * KBO Stats(ruta 업데이트 이전) 수준의 "기본 스탯"만 우선 다룬다.
 * WAR, 승리확률 같은 심화 지표는 이후 단계에서 추가 (지금은 범위 밖).
 *
 * 타자/투수 스탯을 한 테이블에 nullable로 같이 둔 이유:
 * 선수 하나가 시즌 중 포지션 변경되는 경우는 드물고, 초기 MVP 단계에서
 * 테이블을 둘로 쪼개는 것보다 조회 쿼리를 단순하게 유지하는 게 낫다고 판단.
 * 데이터가 커지면 BatterRecord/PitcherRecord로 분리 고려.
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

    // 투수 기본 스탯
    private Double era; // 방어율
    private Integer wins;
    private Integer losses;
    private Integer saves;
    private Integer strikeouts;

    @Builder
    public PlayerRecord(Player player, int season, int gamesPlayed,
                         Double battingAverage, Integer hits, Integer homeRuns, Integer rbi,
                         Double era, Integer wins, Integer losses, Integer saves, Integer strikeouts) {
        this.player = player;
        this.season = season;
        this.gamesPlayed = gamesPlayed;
        this.battingAverage = battingAverage;
        this.hits = hits;
        this.homeRuns = homeRuns;
        this.rbi = rbi;
        this.era = era;
        this.wins = wins;
        this.losses = losses;
        this.saves = saves;
        this.strikeouts = strikeouts;
    }
}
