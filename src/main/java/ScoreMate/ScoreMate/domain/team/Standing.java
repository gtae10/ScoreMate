package ScoreMate.ScoreMate.domain.team;

import ScoreMate.ScoreMate.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시즌 단위 팀 순위표.
 * 매일 배치로 갱신되는 스냅샷 성격이라, team + season 조합에 유니크 제약을 걸어
 * 크롤링할 때마다 새로 만들지 않고 기존 행을 업데이트한다.
 */
@Entity
@Table(
        name = "standings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "season"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Standing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private int season;

    @Column(name = "team_rank", nullable = false)
    private int rank;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(nullable = false)
    private int draws;

    @Column(nullable = false)
    private double winRate;

    // 1위와의 승차 (KBO 순위표 기본 항목)
    private Double gamesBehind;

    @Builder
    public Standing(Team team, int season, int rank, int wins, int losses, int draws, double winRate, Double gamesBehind) {
        this.team = team;
        this.season = season;
        this.rank = rank;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.winRate = winRate;
        this.gamesBehind = gamesBehind;
    }

    public void update(int rank, int wins, int losses, int draws, double winRate, Double gamesBehind) {
        this.rank = rank;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.winRate = winRate;
        this.gamesBehind = gamesBehind;
    }
}
