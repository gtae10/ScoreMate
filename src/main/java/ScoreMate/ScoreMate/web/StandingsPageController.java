package ScoreMate.ScoreMate.web;

import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.team.StandingService;
import ScoreMate.ScoreMate.dto.response.StandingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Year;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class StandingsPageController {

    private final StandingService standingService;

    @GetMapping("/standings")
    public String standings(@RequestParam(required = false) Integer season, Model model) {
        int targetSeason = (season != null) ? season : Year.now().getValue();

        List<StandingView> standings = standingService.getStandings(League.KBO, targetSeason).stream()
                .map(this::toView)
                .toList();

        model.addAttribute("standings", standings);
        model.addAttribute("season", targetSeason);

        return "standings";
    }

    private StandingView toView(StandingResponse s) {
        TeamBadgeMapper.TeamBadge badge = TeamBadgeMapper.badgeFor(s.teamName());
        return new StandingView(
                s.rank(), s.teamName(), badge.code(), badge.color(),
                s.wins(), s.losses(), s.draws(), s.winRate(), s.gamesBehind()
        );
    }

    public record StandingView(
            int rank, String teamName, String code, String color,
            int wins, int losses, int draws, double winRate, Double gamesBehind
    ) {
    }
}
