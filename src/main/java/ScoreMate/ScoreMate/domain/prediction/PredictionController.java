package ScoreMate.ScoreMate.domain.prediction;

import ScoreMate.ScoreMate.common.ApiResponse;
import ScoreMate.ScoreMate.dto.request.PredictionRequest;
import ScoreMate.ScoreMate.dto.response.PredictionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    // TODO: 인증 붙인 뒤 @AuthenticationPrincipal 등으로 로그인 유저 id 꺼내오도록 교체
    @PostMapping
    public ApiResponse<PredictionResponse> submit(@RequestParam Long userId, @Valid @RequestBody PredictionRequest request) {
        return ApiResponse.success(predictionService.submit(userId, request));
    }

    @GetMapping("/me")
    public ApiResponse<List<PredictionResponse>> getMyPredictions(@RequestParam Long userId) {
        return ApiResponse.success(predictionService.getMyPredictions(userId));
    }
}
