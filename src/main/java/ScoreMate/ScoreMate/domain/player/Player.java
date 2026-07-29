package ScoreMate.ScoreMate.domain.player;

import ScoreMate.ScoreMate.common.BaseEntity;
import ScoreMate.ScoreMate.domain.team.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Position position;

    private Integer backNumber;

    @Column(unique = true, length = 100)
    private String externalId;

    @Builder
    public Player(Team team, String name, Position position, Integer backNumber, String externalId) {
        this.team = team;
        this.name = name;
        this.position = position;
        this.backNumber = backNumber;
        this.externalId = externalId;
    }

    public void changeTeam(Team team) {
        this.team = team;
    }

    // 야구(투수/타자 구분 정도), 이후 축구 포지션(FW/MF/DF/GK) 확장 여지를 남겨둠
    public enum Position {
        PITCHER, BATTER,
        FW, MF, DF, GK
    }
}
