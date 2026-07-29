package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.team.Standing;

public record StandingResponse(
        int rank,
        Long teamId,
        String teamName,
        String teamLogoUrl,
        int wins,
        int losses,
        int draws,
        double winRate,
        Double gamesBehind
) {
    public static StandingResponse from(Standing standing) {
        return new StandingResponse(
                standing.getRank(),
                standing.getTeam().getId(),
                standing.getTeam().getName(),
                standing.getTeam().getLogoUrl(),
                standing.getWins(),
                standing.getLosses(),
                standing.getDraws(),
                standing.getWinRate(),
                standing.getGamesBehind()
        );
    }
}
