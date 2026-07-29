package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.player.Player;

public record PlayerResponse(
        Long id,
        String name,
        String position,
        Integer backNumber,
        Long teamId,
        String teamName
) {
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getName(),
                player.getPosition().name(),
                player.getBackNumber(),
                player.getTeam().getId(),
                player.getTeam().getName()
        );
    }
}
