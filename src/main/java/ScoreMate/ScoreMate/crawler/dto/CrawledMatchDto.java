package ScoreMate.ScoreMate.crawler.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 크롤링 직후, 아직 DB 엔티티로 변환되기 전 단계의 중간 데이터.
 * 크롤러 → 서비스 계층으로 넘길 때 이 DTO를 사용한다.
 *
 * 필드가 많고 타입이 겹치는 게(String/Integer) 많아 위치 기반 생성자는 순서 실수가
 * 나기 쉽다. 항상 {@link #builder()}의 명명된 세터로 생성해서 컴파일 타임에 막는다.
 */
@Builder
public record CrawledMatchDto(
        String externalId,
        String homeTeam,
        String awayTeam,
        LocalDateTime matchDate,
        boolean finished,
        boolean live,
        boolean cancelled,
        boolean postponed,
        Integer homeScore,
        Integer awayScore,
        String liveStatusText, // 예: "5회말". live=true일 때만 값이 있고, 그 외엔 null
        String stadium,
        String winPitcher,  // 종료된 경기의 승리 투수. 그 외엔 null
        String losePitcher, // 종료된 경기의 패전 투수. 그 외엔 null
        String awayStartingPitcher, // 원정팀 선발투수 (예정/진행/종료 상관없이 등록되면 나옴)
        String homeStartingPitcher, // 홈팀 선발투수
        String awayInnings, // 쉼표구분 이닝별 점수. 아직 없으면 null
        String homeInnings,
        Integer awayHits,
        Integer homeHits,
        Integer awayErrors,
        Integer homeErrors,
        String cancelReason // 취소/연기 사유 원문 (예: "우천취소", "폭염취소"). 그 외엔 null
) {
}
