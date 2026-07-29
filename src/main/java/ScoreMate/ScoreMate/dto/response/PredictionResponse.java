package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.prediction.Prediction;

public record PredictionResponse(
        Long id,
        Long matchId,
        String predictedResult,
        Boolean correct
) {
    public static PredictionResponse from(Prediction prediction) {
        return new PredictionResponse(
                prediction.getId(),
                prediction.getMatch().getId(),
                prediction.getPredictedResult().name(),
                prediction.getCorrect()
        );
    }
}
