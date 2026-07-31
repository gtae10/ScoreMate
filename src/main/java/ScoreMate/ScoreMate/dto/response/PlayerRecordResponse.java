package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.player.PlayerRecord;

public record PlayerRecordResponse(
        int season,
        int gamesPlayed,
        Double battingAverage,
        Integer hits,
        Integer homeRuns,
        Integer rbi,
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
                record.getWalks(),
                record.getIntentionalWalks(),
                record.getHitByPitch(),
                record.getGroundIntoDoublePlay(),
                record.getErrors(),
                record.getStolenBasePercentage(),
                record.getOnBasePercentage(),
                record.getSluggingPercentage(),
                record.getOps(),
                record.getRunnersInScoringPositionAvg(),
                record.getPinchHitAvg(),
                record.getMultiHits(),
                record.getEra(),
                record.getWins(),
                record.getLosses(),
                record.getSaves(),
                record.getStrikeouts()
        );
    }
}
