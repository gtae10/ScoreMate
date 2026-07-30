package ScoreMate.ScoreMate.crawler.dto;

/**
 * KBO 영문 사이트(Team Standings) 크롤링 결과.
 * externalTeamCode는 영문 페이지의 팀 표기(SAMSUNG, KT 등)를 그대로 담고,
 * teamNameKorean은 매핑을 거쳐 우리 서비스 전반에서 쓰는 한글 팀명으로 변환된 값이다.
 */
public record CrawledStandingDto(
        String externalTeamCode,
        String teamNameKorean,
        int rank,
        int games,
        int wins,
        int losses,
        int draws,
        double winRate,
        Double gamesBehind,
        String streak
) {
}
