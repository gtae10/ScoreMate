package ScoreMate.ScoreMate.crawler.dto;

import ScoreMate.ScoreMate.domain.player.Player;

/**
 * KBO 영문 사이트(Batting/Pitching Leaders) 크롤링 결과.
 * externalId는 KBO 선수 고유 식별자(pcode)를 그대로 쓴다.
 * 타자/투수 공용 DTO라 해당 안 되는 필드는 null.
 */
public record CrawledPlayerRecordDto(
        String externalId,
        String playerName,
        String externalTeamCode,
        String teamNameKorean,
        Player.Position position,
        int gamesPlayed,

        // 타자 전용
        Double battingAverage,
        Integer hits,
        Integer homeRuns,
        Integer rbi,

        // 투수 전용
        Double era,
        Integer wins,
        Integer losses,
        Integer saves,
        Integer strikeouts // 이 리더보드 페이지엔 탈삼진 컬럼이 없어서 항상 null
) {
}
