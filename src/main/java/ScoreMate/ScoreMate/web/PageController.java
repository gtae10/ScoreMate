package ScoreMate.ScoreMate.web;

import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.match.MatchService;
import ScoreMate.ScoreMate.dto.response.MatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 화면(Thymeleaf) 렌더링을 담당하는 컨트롤러. REST API(도메인별 *Controller)와는 별개로,
 * 서버사이드 렌더링 페이지는 이 패키지(web)에 모아둔다.
 *
 * 날짜 이동은 React 버전처럼 클라이언트 상태로 처리하는 대신, 서버 렌더링 방식에 맞게
 * ?date=yyyy-MM-dd 쿼리 파라미터로 페이지를 다시 요청하는 방식으로 구현했다.
 * 날짜 탭 9개는 항상 선택된 날짜(selected)를 중심으로 다시 계산된다.
 */
@Controller
@RequiredArgsConstructor
public class PageController {

    private static final Map<DayOfWeek, String> WEEKDAY_KR = Map.of(
            DayOfWeek.MONDAY, "월", DayOfWeek.TUESDAY, "화", DayOfWeek.WEDNESDAY, "수",
            DayOfWeek.THURSDAY, "목", DayOfWeek.FRIDAY, "금", DayOfWeek.SATURDAY, "토", DayOfWeek.SUNDAY, "일"
    );

    private final MatchService matchService;

    @GetMapping("/")
    public String schedule(@RequestParam(required = false) String date, Model model) {
        LocalDate today = LocalDate.now();
        LocalDate selected = (date != null) ? LocalDate.parse(date) : today;

        List<MatchResponse> matches = matchService.getMatchesByLeagueAndDate(League.KBO, selected);

        model.addAttribute("matches", matches.stream().map(this::toView).toList());
        model.addAttribute("selectedDate", selected);
        model.addAttribute("dateTabs", buildDateTabs(selected, today));
        model.addAttribute("prevWeekDate", selected.minusDays(7));
        model.addAttribute("nextWeekDate", selected.plusDays(7));
        model.addAttribute("todayDate", today);
        model.addAttribute("showTodayJump", Math.abs(ChronoUnit.DAYS.between(today, selected)) > 3);

        return "dashboard";
    }

    private MatchView toView(MatchResponse match) {
        TeamBadgeMapper.TeamBadge homeBadge = TeamBadgeMapper.badgeFor(match.homeTeam());
        TeamBadgeMapper.TeamBadge awayBadge = TeamBadgeMapper.badgeFor(match.awayTeam());

        String statusText = switch (match.status()) {
            case "FINISHED" -> "종료";
            case "CANCELLED" -> "취소";
            case "POSTPONED" -> "연기";
            case "LIVE" -> (match.liveInning() != null && !match.liveInning().isBlank())
                    ? match.liveInning()
                    : "진행중";
            default -> match.matchDate().toLocalTime().toString().substring(0, 5);
        };

        String homeResult = "";
        String awayResult = "";
        if (match.homeScore() != null && match.awayScore() != null) {
            if (match.homeScore() > match.awayScore()) {
                homeResult = "win";
                awayResult = "lose";
            } else if (match.homeScore() < match.awayScore()) {
                homeResult = "lose";
                awayResult = "win";
            }
        }

        return new MatchView(
                statusText,
                "LIVE".equals(match.status()),
                match.homeTeam(), homeBadge.code(), homeBadge.color(), match.homeScore(), homeResult,
                match.awayTeam(), awayBadge.code(), awayBadge.color(), match.awayScore(), awayResult
        );
    }

    private List<DateTabView> buildDateTabs(LocalDate center, LocalDate today) {
        List<DateTabView> tabs = new ArrayList<>();
        for (int i = -4; i <= 4; i++) {
            LocalDate d = center.plusDays(i);
            tabs.add(new DateTabView(labelFor(d, today), monthDay(d), d.toString(), d.equals(center)));
        }
        return tabs;
    }

    private String labelFor(LocalDate d, LocalDate today) {
        long diff = ChronoUnit.DAYS.between(today, d);
        if (diff == 0) return "오늘";
        if (diff == -1) return "어제";
        if (diff == 1) return "내일";
        return WEEKDAY_KR.get(d.getDayOfWeek());
    }

    private String monthDay(LocalDate d) {
        return d.getMonthValue() + "." + d.getDayOfMonth();
    }

    public record DateTabView(String label, String sub, String dateParam, boolean active) {
    }

    /**
     * 화면 렌더링에 필요한 걸 전부 미리 계산해서 담아두는 뷰 모델.
     * (팀 배지 색/약자, 승/패 여부까지 컨트롤러에서 계산해서 템플릿은 단순 출력만 하게 함)
     */
    public record MatchView(
            String statusText,
            boolean live,
            String homeTeam, String homeCode, String homeColor, Integer homeScore, String homeResult,
            String awayTeam, String awayCode, String awayColor, Integer awayScore, String awayResult
    ) {
    }
}
