package ScoreMate.ScoreMate.web;

import java.util.Map;

/**
 * 화면에 팀을 표시할 때 쓰는 배지 정보(팀 컬러 + 약자).
 * 실제 구단 엠블럼(로고 이미지)은 저작권이 있는 이미지라 그대로 쓸 수 없어서,
 * 팀 고유 컬러 + 영문 약자로 대체한다 (키움 같은 경우 한 글자보다 "KW"가 더 로고답게 보임).
 */
public final class TeamBadgeMapper {

    public record TeamBadge(String code, String color) {
    }

    private static final Map<String, TeamBadge> BADGES = Map.ofEntries(
            Map.entry("삼성", new TeamBadge("SS", "#074ca1")),
            Map.entry("KT", new TeamBadge("KT", "#000000")),
            Map.entry("LG", new TeamBadge("LG", "#a90031")),
            Map.entry("KIA", new TeamBadge("KIA", "#ea0029")),
            Map.entry("두산", new TeamBadge("OB", "#131230")),
            Map.entry("한화", new TeamBadge("HH", "#fc4e00")),
            Map.entry("NC", new TeamBadge("NC", "#315288")),
            Map.entry("롯데", new TeamBadge("LT", "#c8102e")),
            Map.entry("SSG", new TeamBadge("SSG", "#ce0e2d")),
            Map.entry("키움", new TeamBadge("KW", "#570514"))
    );

    private static final TeamBadge DEFAULT_BADGE = new TeamBadge("?", "#565d6d");

    private TeamBadgeMapper() {
    }

    public static TeamBadge badgeFor(String teamName) {
        return BADGES.getOrDefault(teamName, DEFAULT_BADGE);
    }
}
