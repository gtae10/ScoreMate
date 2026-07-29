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
        Integer awayScore
) {
    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getLeague().name(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getMatchDate(),
                match.getStatus().name(),
                match.getHomeScore(),
                match.getAwayScore()
        );
    }
}
