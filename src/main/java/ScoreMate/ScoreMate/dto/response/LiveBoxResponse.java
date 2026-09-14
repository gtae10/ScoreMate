package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.match.Match;

public record LiveBoxResponse(
        Long matchId,
        boolean live,
        String inningText,
        boolean runnerOnFirst,
        boolean runnerOnSecond,
        boolean runnerOnThird,
        Integer ballCount,
        Integer strikeCount,
        Integer outCount,
        String currentBatter,
        String pitcher,
        String catcher,
        String firstBase,
        String secondBase,
        String thirdBase,
        String shortstop,
        String leftFielder,
        String centerFielder,
        String rightFielder
) {
    public static LiveBoxResponse from(Match match) {
        return new LiveBoxResponse(
                match.getId(),
                match.getStatus() == Match.MatchStatus.LIVE,
                match.getLiveInning(),
                match.isRunnerOnFirst(),
                match.isRunnerOnSecond(),
                match.isRunnerOnThird(),
                match.getBallCount(),
                match.getStrikeCount(),
                match.getOutCount(),
                match.getCurrentBatter(),
                match.getLivePitcher(),
                match.getLiveCatcher(),
                match.getLiveFirstBase(),
                match.getLiveSecondBase(),
                match.getLiveThirdBase(),
                match.getLiveShortstop(),
                match.getLiveLeftFielder(),
                match.getLiveCenterFielder(),
                match.getLiveRightFielder()
        );
    }
}
