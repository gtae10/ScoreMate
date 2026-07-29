package ScoreMate.ScoreMate.crawler.dto;

import java.time.LocalDateTime;

/**
 * 크롤링 직후, 아직 DB 엔티티로 변환되기 전 단계의 중간 데이터.
 * 크롤러 → 서비스 계층으로 넘길 때 이 DTO를 사용한다.
 */
public record CrawledMatchDto(
        String externalId,
        String homeTeam,
        String awayTeam,
        LocalDateTime matchDate,
        boolean finished,
        Integer homeScore,
        Integer awayScore
) {
}
