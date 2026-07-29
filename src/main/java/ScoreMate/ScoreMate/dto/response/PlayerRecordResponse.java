package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.player.PlayerRecord;

public record PlayerRecordResponse(
        int season,
        int gamesPlayed,
        Double battingAverage,
        Integer hits,
        Integer homeRuns,
        Integer rbi,
        Double era,
        Integer wins,
        Integer losses,
        Integer saves,
        Integer strikeouts
) {
    public static PlayerRecordResponse from(PlayerRecord record) {
        return new PlayerRecordResponse(
                record.getSeason(),
                record.getGamesPlayed(),
                record.getBattingAverage(),
                record.getHits(),
                record.getHomeRuns(),
                record.getRbi(),
                record.getEra(),
                record.getWins(),
                record.getLosses(),
                record.getSaves(),
                record.getStrikeouts()
        );
    }
}
