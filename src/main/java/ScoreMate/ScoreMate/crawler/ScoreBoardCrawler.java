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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KBO 스코어보드 페이지(오늘 경기만)를 크롤링해서 실시간 상태/스코어를 가져온다.
 *
 * 일정 API(KboCrawler)와 달리 이 페이지는 자바스크립트/AJAX 없이 서버가 오늘 경기를
 * 바로 렌더링해서 내려준다. 그래서 GET 한 번이면 충분하다 (날짜 이동은 __doPostBack
 * 방식이라 복잡한데, 오늘 날짜만 필요하니 그냥 파라미터 없이 GET해서 얻는 기본값을 쓴다).
 *
 * 홈/원정 순서 주의: "leftTeam"이 원정팀(alt="원정팀"), "rightTeam"이 홈팀(alt="홈팀")이다.
 * gameId도 "원정팀코드+홈팀코드" 순서로 구성돼 있다 (예: LGOB0 = LG 원정, OB(두산) 홈).
 *
 * 상태는 lblGameState 텍스트로 판단한다:
 *  - "종료" 포함 → 종료
 *  - "전" 으로 끝남(예: "경기전") → 아직 시작 안 함 (일정 크롤러가 이미 SCHEDULED로 넣어뒀을 것)
 *  - 그 외(예: "5회말") → 진행 중(LIVE)
 */
@Slf4j
@Component
public class ScoreBoardCrawler {

    private static final String SCOREBOARD_URL = "https://www.koreabaseball.com/Schedule/ScoreBoard.aspx";
    private static final int TIMEOUT_MS = 10_000;
    private static final Pattern GAME_ID_PATTERN = Pattern.compile("gameId=(\\d{8}[A-Z]{4}\\d)");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2})");

    /**
     * 오늘 경기의 실시간 상태/스코어를 크롤링한다.
     */
    public List<CrawledMatchDto> crawlToday() {
        List<CrawledMatchDto> results = new ArrayList<>();
        LocalDate today = LocalDate.now();

        try {
            Document doc = Jsoup.connect(SCOREBOARD_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .get();

            Elements gameBlocks = doc.select("div.smsScore");

            for (Element block : gameBlocks) {
                CrawledMatchDto dto = parseGameBlock(block, today);
                if (dto != null) {
                    results.add(dto);
                }
            }

            log.info("KBO 스코어보드 크롤링 완료 - 수집 건수: {}", results.size());

        } catch (IOException e) {
            log.error("KBO 스코어보드 크롤링 실패", e);
        }

        return results;
    }

    private CrawledMatchDto parseGameBlock(Element block, LocalDate today) {
        String gameId = extractGameId(block);
        if (gameId == null) {
            log.warn("스코어보드에서 gameId를 못 찾음 - block: {}", block.text());
            return null;
        }

        Element awayNameEl = block.selectFirst("p.leftTeam strong.teamT");
        Element homeNameEl = block.selectFirst("p.rightTeam strong.teamT");
        if (awayNameEl == null || homeNameEl == null) {
            return null;
        }
        String awayTeam = awayNameEl.text();
        String homeTeam = homeNameEl.text();

        Integer awayScore = parseIntSafe(text(block, "p.leftTeam em.score span"));
        Integer homeScore = parseIntSafe(text(block, "p.rightTeam em.score span"));

        String stateText = text(block, "strong.flag span");
        boolean finished = stateText.contains("종료");
        boolean notStarted = stateText.endsWith("전") || stateText.isBlank();
        boolean live = !finished && !notStarted;

        LocalTime time = parseTime(text(block, "p.place"));
        LocalDateTime matchDateTime = time != null ? LocalDateTime.of(today, time) : today.atStartOfDay();

        return new CrawledMatchDto(gameId, homeTeam, awayTeam, matchDateTime, finished, live, homeScore, awayScore);
    }

    private String extractGameId(Element block) {
        Matcher matcher = GAME_ID_PATTERN.matcher(block.html());
        return matcher.find() ? matcher.group(1) : null;
    }

    private LocalTime parseTime(String placeText) {
        Matcher matcher = TIME_PATTERN.matcher(placeText);
        if (!matcher.find()) {
            return null;
        }
        return LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    private String text(Element block, String cssQuery) {
        Element el = block.selectFirst(cssQuery);
        return el != null ? el.text() : "";
    }

    private Integer parseIntSafe(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
