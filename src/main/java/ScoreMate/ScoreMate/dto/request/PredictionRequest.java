package ScoreMate.ScoreMate.dto.request;

import ScoreMate.ScoreMate.domain.match.Match;
import jakarta.validation.constraints.NotNull;

public record PredictionRequest(

        @NotNull(message = "경기 id는 필수입니다.")
        Long matchId,

        @NotNull(message = "예측 결과는 필수입니다.")
        Match.MatchResult predictedResult
) {
}
