package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.team.Team;

public record TeamResponse(
        Long id,
        String league,
        String name,
        String shortName,
        String logoUrl
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getLeague().name(),
                team.getName(),
                team.getShortName(),
                team.getLogoUrl()
        );
    }
}
