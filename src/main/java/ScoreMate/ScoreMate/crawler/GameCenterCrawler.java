package ScoreMate.ScoreMate.crawler;

import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * KBO GameCenter 페이지의 "그날 경기 목록"(ul.game-list-n)을 크롤링한다.
 * gameDate 쿼리 파라미터만으로 그날 전체 경기가 한 번에 나온다 (특정 gameId 불필요).
 *
 * 이 목록의 각 <li class="game-cont">에 그 경기의 핵심 정보가 속성으로 그대로 들어있다:
 *   g_id(경기ID), s_nm(구장), away_id/home_id(팀코드), away_nm/home_nm(팀명),
 *   away_p_id/home_p_id(선발투수 고유번호), game_sc(상태코드)
 * 그리고 각 팀의 .today-pitcher 안에 실제 선발투수 "이름"이 텍스트로 있다
 * (span.before에 "선"이라는 배지 글자가 별도로 있어서, 그 부분은 제외하고 이름만 뽑는다).
 *
 * robots.txt 확인 결과 /Schedule/ 경로는 막혀있지 않다 (Disallow는 /Common/, /Help/,
 * /Member/, /ws/ 뿐).
 */
@Slf4j
@Component
public class GameCenterCrawler {

    private static final String GAME_CENTER_URL = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx";
    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    private static final DateTimeFormatter DATE_PARAM_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<CrawledMatchDto> crawlToday() {
        return crawlByDate(LocalDate.now());
    }

    public List<CrawledMatchDto> crawlByDate(LocalDate date) {
        List<CrawledMatchDto> results = new ArrayList<>();

        try {
            String url = GAME_CENTER_URL + "?gameDate=" + date.format(DATE_PARAM_FORMAT);
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent(USER_AGENT)
                    .get();

            Elements gameBlocks = doc.select("ul.game-list-n li.game-cont");
            log.warn("GameCenter 디버깅 - date: {}, 찾은 game-cont 블록 수: {}", date, gameBlocks.size());
            if (!gameBlocks.isEmpty()) {
                log.warn("GameCenter 디버깅 - 첫 블록 시작 태그: {}",
                        gameBlocks.first().toString().substring(0, Math.min(500, gameBlocks.first().toString().length())));
            }

            if (gameBlocks.isEmpty()) {
                String html = doc.html();
                boolean hasGameListMarker = html.contains("game-list-n");
                boolean hasGameContMarker = html.contains("game-cont");
                log.warn("GameCenter 목록에서 game-cont를 못 찾음 - date: {}, 응답 길이: {}, "
                                + "'game-list-n' 포함여부: {}, 'game-cont' 포함여부: {}",
                        date, html.length(), hasGameListMarker, hasGameContMarker);
                int idx = html.indexOf("game-list-n");
                if (idx >= 0) {
                    int end = Math.min(html.length(), idx + 1500);
                    log.warn("'game-list-n' 주변 HTML: {}", html.substring(idx, end));
                }
            }

            for (Element block : gameBlocks) {
                CrawledMatchDto dto = parseGameBlock(block, date);
                if (dto != null) {
                    results.add(dto);
                }
            }

            log.info("GameCenter 일별 목록 크롤링 완료 - date: {}, 수집 건수: {}", date, results.size());

        } catch (IOException e) {
            log.error("GameCenter 일별 목록 크롤링 실패 - date: {}", date, e);
        }

        return results;
    }

    private CrawledMatchDto parseGameBlock(Element block, LocalDate date) {
        String awayTeam = block.attr("away_nm");
        String homeTeam = block.attr("home_nm");
        if (awayTeam.isBlank() || homeTeam.isBlank()) {
            return null;
        }

        String stadium = block.attr("s_nm");
        if (stadium.isBlank()) {
            stadium = null;
        }

        String awayPitcher = pitcherName(block, "away");
        String homePitcher = pitcherName(block, "home");

        // externalId는 다른 크롤러(KboCrawler/ScoreBoardCrawler)와 반드시 같은 형식으로 통일
        String externalId = date + "_" + homeTeam + "_" + awayTeam;

        // 이 크롤러는 선발투수/구장 보강이 목적이라 스코어/상태는 건드리지 않는다
        // (finished/live 둘 다 기본값 false로 둬서 MatchService가 상태를 덮어쓰지 않게 함).
        return CrawledMatchDto.builder()
                .externalId(externalId)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .matchDate(date.atStartOfDay())
                .stadium(stadium)
                .awayStartingPitcher(awayPitcher)
                .homeStartingPitcher(homePitcher)
                .build();
    }

    /**
     * .today-pitcher 안의 텍스트에서 "선"(선발 배지) 부분을 제외한 실제 이름만 뽑는다.
     * 구조: <div class="today-pitcher"><p><span class="before">선</span>라일리 </p></div>
     */
    private String pitcherName(Element block, String sideClass) {
        Element p = block.selectFirst("div.team." + sideClass + " .today-pitcher p");
        if (p == null) {
            return null;
        }
        String name = p.ownText().trim();
        return name.isBlank() ? null : name;
    }
}
