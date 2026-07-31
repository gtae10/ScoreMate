package ScoreMate.ScoreMate.crawler.dto;

import ScoreMate.ScoreMate.domain.player.Player;

/**
 * KBO 영문 사이트(Batting/Pitching Leaders) 크롤링 결과.
 * externalId는 KBO 선수 고유 식별자(pcode)를 그대로 쓴다.
 * 타자/투수 공용 DTO라 해당 안 되는 필드는 null.
 *
 * 타자 세부 스탯(walks ~ multiHits)은 Batting Leaders 2페이지에서 온다.
 * 투수는 2페이지 선수 구성이 1페이지와 달라서 세부 스탯을 안 채운다(항상 null).
 */
public record CrawledPlayerRecordDto(
        String externalId,
        String playerName,
        String externalTeamCode,
        String teamNameKorean,
        Player.Position position,
        int gamesPlayed,

        // 타자 기본
        Double battingAverage,
        Integer hits,
        Integer homeRuns,
        Integer rbi,

        // 타자 세부 (Batting Leaders 2페이지, 없으면 null)
        Integer walks,
        Integer intentionalWalks,
        Integer hitByPitch,
        Integer groundIntoDoublePlay,
        Integer errors,
        Double stolenBasePercentage,
        Double onBasePercentage,
        Double sluggingPercentage,
        Double ops,
        Double runnersInScoringPositionAvg,
        Double pinchHitAvg,
        Integer multiHits,

        // 투수
        Double era,
        Integer wins,
        Integer losses,
        Integer saves,
        Integer strikeouts // 투수 리더보드엔 탈삼진 컬럼이 없어서 항상 null
) {
}
