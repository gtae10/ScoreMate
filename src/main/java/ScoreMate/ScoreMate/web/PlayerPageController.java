package ScoreMate.ScoreMate.web;

import ScoreMate.ScoreMate.domain.player.PlayerService;
import ScoreMate.ScoreMate.dto.response.PlayerRecordResponse;
import ScoreMate.ScoreMate.dto.response.PlayerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PlayerPageController {

    private final PlayerService playerService;

    @GetMapping("/players")
    public String players(@RequestParam(required = false) String q, Model model) {
        List<PlayerResponse> results = (q != null && !q.isBlank())
                ? playerService.search(q)
                : List.of();

        model.addAttribute("query", q);
        model.addAttribute("results", results);
        model.addAttribute("searched", q != null && !q.isBlank());

        return "players";
    }

    @GetMapping("/players/{id}")
    public String playerDetail(@PathVariable Long id, Model model) {
        PlayerResponse player = playerService.getPlayer(id);
        List<PlayerRecordResponse> records = playerService.getPlayerRecords(id);

        TeamBadgeMapper.TeamBadge badge = TeamBadgeMapper.badgeFor(player.teamName());

        model.addAttribute("player", player);
        model.addAttribute("badgeCode", badge.code());
        model.addAttribute("badgeColor", badge.color());
        model.addAttribute("records", records);
        model.addAttribute("isPitcher", "PITCHER".equals(player.position()));

        return "player-detail";
    }
}
