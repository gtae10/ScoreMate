package ScoreMate.ScoreMate.domain.team;

import ScoreMate.ScoreMate.common.BaseEntity;
import ScoreMate.ScoreMate.domain.match.League;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private League league;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String shortName;

    private String logoUrl;

    // 크롤링 소스에서의 팀 식별자 (중복 수집 방지 + 매칭용)
    @Column(unique = true, length = 100)
    private String externalId;

    @Builder
    public Team(League league, String name, String shortName, String logoUrl, String externalId) {
        this.league = league;
        this.name = name;
        this.shortName = shortName;
        this.logoUrl = logoUrl;
        this.externalId = externalId;
    }

    public void updateLogo(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
