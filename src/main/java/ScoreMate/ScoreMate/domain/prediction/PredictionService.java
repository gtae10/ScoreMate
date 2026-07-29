package ScoreMate.ScoreMate.domain.prediction;

import ScoreMate.ScoreMate.domain.match.Match;
import ScoreMate.ScoreMate.domain.match.MatchRepository;
import ScoreMate.ScoreMate.domain.user.User;
import ScoreMate.ScoreMate.domain.user.UserRepository;
import ScoreMate.ScoreMate.dto.request.PredictionRequest;
import ScoreMate.ScoreMate.dto.response.PredictionResponse;
import ScoreMate.ScoreMate.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PredictionService {

    private static final int CORRECT_PREDICTION_POINTS = 10;

    private final PredictionRepository predictionRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    @Transactional
    public PredictionResponse submit(Long userId, PredictionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다."));
        Match match = matchRepository.findById(request.matchId())
                .orElseThrow(() -> new CustomException("경기를 찾을 수 없습니다."));

        if (match.getStatus() != Match.MatchStatus.SCHEDULED) {
            throw new CustomException("이미 시작했거나 종료된 경기는 예측할 수 없습니다.");
        }
        if (predictionRepository.existsByUserAndMatch(user, match)) {
            throw new CustomException("이미 이 경기를 예측했습니다.");
        }

        Prediction prediction = Prediction.builder()
                .user(user)
                .match(match)
                .predictedResult(request.predictedResult())
                .build();

        return PredictionResponse.from(predictionRepository.save(prediction));
    }

    public List<PredictionResponse> getMyPredictions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다."));

        return predictionRepository.findByUser(user).stream()
                .map(PredictionResponse::from)
                .toList();
    }

    /**
     * 경기 종료 후 호출되어 해당 경기에 걸린 미채점 예측들을 채점하고 포인트를 지급한다.
     * (크롤러가 경기 결과를 업데이트한 직후 호출하는 것을 염두에 둠)
     */
    @Transactional
    public void gradePredictions(Match match) {
        Match.MatchResult actualResult = match.getResult();
        if (actualResult == null) {
            return;
        }

        List<Prediction> pendingPredictions = predictionRepository.findByMatchAndCorrectIsNull(match);
        for (Prediction prediction : pendingPredictions) {
            prediction.grade(actualResult);
            if (Boolean.TRUE.equals(prediction.getCorrect())) {
                prediction.getUser().addPoints(CORRECT_PREDICTION_POINTS);
            }
        }
        log.info("예측 채점 완료 - matchId: {}, 채점 건수: {}", match.getId(), pendingPredictions.size());
    }
}
