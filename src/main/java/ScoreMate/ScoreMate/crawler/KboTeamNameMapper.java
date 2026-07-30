package ScoreMate.ScoreMate.crawler;

import java.util.Map;

/**
 * KBO 영문 사이트(eng.koreabaseball.com)에서 쓰는 팀 코드(SAMSUNG, KT ...)를
 * 우리 서비스 전반에서 쓰는 한글 팀명으로 매핑한다.
 * StandingCrawler, PlayerCrawler 등 영문 사이트를 크롤링하는 곳에서 공통으로 쓴다.
 */
public final class KboTeamNameMapper {

    private static final Map<String, String> TEAM_NAME_MAP = Map.of(
            "SAMSUNG", "삼성",
            "KT", "KT",
            "LG", "LG",
            "KIA", "KIA",
            "DOOSAN", "두산",
            "HANWHA", "한화",
            "NC", "NC",
            "LOTTE", "롯데",
            "SSG", "SSG",
            "KIWOOM", "키움"
    );

    private KboTeamNameMapper() {
    }

    /**
     * 영문 팀 코드를 한글 팀명으로 변환한다. 매핑에 없으면 null을 반환한다.
     */
    public static String toKoreanName(String englishCode) {
        return TEAM_NAME_MAP.get(englishCode);
    }
}
