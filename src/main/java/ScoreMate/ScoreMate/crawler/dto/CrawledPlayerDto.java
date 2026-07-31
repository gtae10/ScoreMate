package ScoreMate.ScoreMate.crawler.dto;

import ScoreMate.ScoreMate.domain.player.Player;

/**
 * KBO 영문 사이트 Player Search 페이지에서 크롤링한 로스터 항목.
 * BattingLeaders/PitchingLeaders(CrawledPlayerRecordDto)와 달리 스탯은 없고
 * 기본 프로필 정보만 담는다 — 여기 목적은 "선수 수를 늘리는 것"이지 기록이 아니다.
 */
public record CrawledPlayerDto(
        String externalId,       // pcode
        String playerName,
        String externalTeamCode,
        String teamNameKorean,
        Player.Position position,
        Integer backNumber
) {
}
