package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.match.Match;

import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        String league,
        String homeTeam,
        String awayTeam,
        LocalDateTime matchDate,
        String status,
        Integer homeScore,
        Integer awayScore,
        String liveInning,
        String stadium,
        String winPitcher,
        String losePitcher,
        String awayStartingPitcher,
        String homeStartingPitcher,
        String awayInnings,
        String homeInnings,
        Integer awayHits,
        Integer homeHits,
        Integer awayErrors,
        Integer homeErrors,
        String cancelReason,
        String awayCurrentPitcher,
        String homeCurrentPitcher,
        String currentBatter,
        String liveTextFeed,
        String homePitcher,
        String awayPitcher
) {
    public static MatchResponse from(Match match) {
        PitcherPair pitchers = resolvePitchers(match);
        return new MatchResponse(
                match.getId(),
                match.getLeague().name(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getMatchDate(),
                match.getStatus().name(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getLiveInning(),
                match.getStadium(),
                match.getWinPitcher(),
                match.getLosePitcher(),
                match.getAwayStartingPitcher(),
                match.getHomeStartingPitcher(),
                match.getAwayInnings(),
                match.getHomeInnings(),
                match.getAwayHits(),
                match.getHomeHits(),
                match.getAwayErrors(),
                match.getHomeErrors(),
                match.getCancelReason(),
                match.getAwayCurrentPitcher(),
                match.getHomeCurrentPitcher(),
                match.getCurrentBatter(),
                match.getLiveTextFeed(),
                pitchers.home(),
                pitchers.away()
        );
    }

    /**
     * 화면에 표시할 투수 우선순위: 지금 던지는 투수(LIVE일 때) > 선발투수 > 승/패 투수(종료된 옛날 데이터 대체).
     * 대시보드 목록/상세 페이지/실시간 갱신 API가 모두 이 값을 그대로 쓴다 — 우선순위 규칙은 여기 한 곳에만 있다.
     */
    private static PitcherPair resolvePitchers(Match match) {
        boolean live = match.getStatus() == Match.MatchStatus.LIVE;
        String home = (live && match.getHomeCurrentPitcher() != null)
                ? match.getHomeCurrentPitcher() : match.getHomeStartingPitcher();
        String away = (live && match.getAwayCurrentPitcher() != null)
                ? match.getAwayCurrentPitcher() : match.getAwayStartingPitcher();

        if (home == null && away == null && match.getHomeScore() != null && match.getAwayScore() != null) {
            if (match.getHomeScore() > match.getAwayScore()) {
                home = match.getWinPitcher();
                away = match.getLosePitcher();
            } else if (match.getHomeScore() < match.getAwayScore()) {
                home = match.getLosePitcher();
                away = match.getWinPitcher();
            }
        }
        return new PitcherPair(home, away);
    }

    private record PitcherPair(String home, String away) {
    }
}
