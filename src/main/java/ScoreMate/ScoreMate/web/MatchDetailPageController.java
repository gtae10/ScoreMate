package ScoreMate.ScoreMate.web;

import ScoreMate.ScoreMate.domain.match.MatchService;
import ScoreMate.ScoreMate.dto.response.LiveBoxResponse;
import ScoreMate.ScoreMate.dto.response.MatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MatchDetailPageController {

    private final MatchService matchService;

    @GetMapping("/matches/{id}")
    public String detail(@PathVariable Long id, Model model) {
        MatchResponse match = matchService.getMatch(id);

        TeamBadgeMapper.TeamBadge homeBadge = TeamBadgeMapper.badgeFor(match.homeTeam());
        TeamBadgeMapper.TeamBadge awayBadge = TeamBadgeMapper.badgeFor(match.awayTeam());

        String statusText = switch (match.status()) {
            case "FINISHED" -> "종료";
            case "CANCELLED" -> (match.cancelReason() != null && !match.cancelReason().isBlank()) ? match.cancelReason() : "취소";
            case "POSTPONED" -> (match.cancelReason() != null && !match.cancelReason().isBlank()) ? match.cancelReason() : "연기";
            case "LIVE" -> (match.liveInning() != null && !match.liveInning().isBlank()) ? match.liveInning() : "진행중";
            default -> match.matchDate().toLocalTime().toString().substring(0, 5);
        };

        // 투수 표시 우선순위는 MatchResponse.from()에서 이미 계산되어 내려온다 (PageController와 공통).
        String homePitcher = match.homePitcher();
        String awayPitcher = match.awayPitcher();

        List<String> inningHeaders = new ArrayList<>();
        List<String> awayInningValues = splitInnings(match.awayInnings());
        List<String> homeInningValues = splitInnings(match.homeInnings());
        int inningCount = Math.max(awayInningValues.size(), homeInningValues.size());
        for (int i = 1; i <= inningCount; i++) {
            inningHeaders.add(String.valueOf(i));
        }

        model.addAttribute("match", match);
        model.addAttribute("homeCode", homeBadge.code());
        model.addAttribute("homeColor", homeBadge.color());
        model.addAttribute("awayCode", awayBadge.code());
        model.addAttribute("awayColor", awayBadge.color());
        model.addAttribute("statusText", statusText);
        model.addAttribute("homePitcher", homePitcher);
        model.addAttribute("awayPitcher", awayPitcher);
        model.addAttribute("inningHeaders", inningHeaders);
        model.addAttribute("awayInningValues", awayInningValues);
        model.addAttribute("homeInningValues", homeInningValues);
        String fieldingPitcher = match.awayCurrentPitcher() != null ? match.awayCurrentPitcher() : match.homeCurrentPitcher();
        List<String> liveTextLines = splitLines(match.liveTextFeed());

        model.addAttribute("hasLineScore", inningCount > 0);
        // LIVE 상태일 때뿐 아니라, 예전에 크롤링해둔 문자중계/투수/타자 정보가 남아있으면
        // 종료된 경기에서도 "마지막 상황"으로 계속 보여준다.
        boolean showLivePanel = "LIVE".equals(match.status())
                || !liveTextLines.isEmpty()
                || fieldingPitcher != null
                || match.currentBatter() != null;
        model.addAttribute("isLive", showLivePanel);
        model.addAttribute("liveTextLines", liveTextLines);
        model.addAttribute("fieldingPitcher", fieldingPitcher);

        // 라이브박스(다이아몬드 그래픽)는 LIVE일 때만 폴링하지만, 최초 진입 시 깜빡임 없이
        // 서버 렌더링 값으로 바로 채워둔다 (마지막 상태가 있으면 종료 후에도 그대로 표시됨).
        model.addAttribute("liveBox", matchService.getLiveBox(id));

        return "match-detail";
    }

    private List<String> splitLines(String feed) {
        if (feed == null || feed.isBlank()) {
            return List.of();
        }
        return List.of(feed.split("\n"));
    }

    private List<String> splitInnings(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return List.of(csv.split(","));
    }
}
