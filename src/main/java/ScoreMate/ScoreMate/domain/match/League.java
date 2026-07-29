package ScoreMate.ScoreMate.domain.match;

/**
 * 지원 리그 목록.
 * 현재는 KBO만 크롤링하지만, 추후 K리그 / 축구 5대리그 확장을 고려해
 * Match 엔티티가 종목에 상관없이 재사용 가능하도록 미리 분리해둔다.
 */
public enum League {
    KBO(Sport.BASEBALL),
    K_LEAGUE(Sport.SOCCER),
    EPL(Sport.SOCCER),
    LA_LIGA(Sport.SOCCER),
    SERIE_A(Sport.SOCCER),
    BUNDESLIGA(Sport.SOCCER),
    LIGUE_1(Sport.SOCCER);

    private final Sport sport;

    League(Sport sport) {
        this.sport = sport;
    }

    public Sport getSport() {
        return sport;
    }

    public enum Sport {
        BASEBALL, SOCCER
    }
}
