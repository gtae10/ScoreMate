package ScoreMate.ScoreMate.crawler;

import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
 * KBO 공식 홈페이지(koreabaseball.com)에서 경기 일정/결과를 크롤링한다.
 *
 * 일정 페이지 자체는 자바스크립트가 별도 API를 호출해서 표를 채우는 방식(AJAX)이라,
 * 페이지 HTML을 그대로 GET 해봐야 빈 뼈대만 나온다. 실제 데이터는 아래 API를
 * POST로 호출해야 JSON으로 내려온다 (브라우저 개발자도구 Network 탭에서 확인).
 *
 * 요청 파라미터:
 *  - leId: 리그ID (1 = KBO 1군)
 *  - srIdList: 시즌구분 목록 (예: "0,9,6" - 시범/정규/포스트시즌 등 복합)
 *  - seasonId: 연도 (예: 2026)
 *  - gameMonth: 월, 2자리 (예: "07")
 *  - teamId: 특정 팀 필터 (전체 조회 시 빈 값)
 *
 * 응답은 월 단위로 하루하루의 경기를 순서대로 내려주고, 같은 날 첫 경기에만
 * "day" 클래스의 날짜 셀이 붙는다 (RowSpan으로 같은 날 나머지 경기와 공유).
 *
 * 주의사항 (비상업적 개인 프로젝트 기준):
 * - robots.txt를 준수하고, 과도한 요청으로 서버에 부하를 주지 않는다.
 * - 수집한 데이터의 저작권은 KBO(한국야구위원회)에 있으며 상업적 이용은 하지 않는다.
 */
@Slf4j
@Component
public class KboCrawler {

    private static final String SCHEDULE_API_URL = "https://www.koreabaseball.com/ws/Schedule.asmx/GetScheduleList";
    private static final int TIMEOUT_MS = 10_000;
    private static final int KBO_LEAGUE_ID = 1;
    private static final String DEFAULT_SEASON_ID_LIST = "0,9,6";

    private static final Pattern DAY_PATTERN = Pattern.compile("(\\d{2})\\.(\\d{2})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2})");
    private static final Pattern GAME_ID_PATTERN = Pattern.compile("gameId=([A-Za-z0-9]+)");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 특정 날짜의 KBO 경기 일정/결과를 크롤링한다.
     * 내부적으로는 해당 월 전체를 한 번에 받아온 뒤 요청받은 날짜만 걸러서 반환한다
     * (API 자체가 월 단위로만 데이터를 주기 때문).
     */
    public List<CrawledMatchDto> crawlByDate(LocalDate date) {
        List<CrawledMatchDto> monthMatches = crawlByMonth(date.getYear(), date.getMonthValue());

        List<CrawledMatchDto> results = monthMatches.stream()
                .filter(m -> m.matchDate().toLocalDate().equals(date))
                .toList();

        log.info("KBO 크롤링 완료 - date: {}, 수집 건수: {}", date, results.size());
        return results;
    }

    /**
     * 특정 연/월의 KBO 경기 전체를 크롤링한다.
     */
    public List<CrawledMatchDto> crawlByMonth(int year, int month) {
        List<CrawledMatchDto> results = new ArrayList<>();

        try {
            String json = fetchScheduleJson(year, month);

            JsonNode root;
            try {
                root = objectMapper.readTree(json);
            } catch (Exception parseError) {
                String snippet = json.length() > 500 ? json.substring(0, 500) : json;
                log.error("KBO 응답이 JSON이 아님 - {}-{}, 응답 앞부분: {}", year, month, snippet);
                return results;
            }
            JsonNode rows = root.path("rows");

            LocalDate currentDate = null;

            for (JsonNode rowWrapper : rows) {
                JsonNode cells = rowWrapper.path("row");
                if (cells.isMissingNode() || cells.size() == 0) {
                    continue;
                }

                int idx = 0;

                // 같은 날 첫 경기에만 붙는 "day" 셀 처리
                JsonNode first = cells.get(0);
                if ("day".equals(textOrNull(first, "Class"))) {
                    currentDate = parseDayCell(text(first), year);
                    idx = 1;
                }

                if (currentDate == null) {
                    log.warn("날짜를 특정할 수 없는 행이 있어 건너뜀 - month: {}-{}", year, month);
                    continue;
                }

                // idx 기준 남은 셀: [time, play, relay, highlight, broadcast, radio, stadium, remark]
                if (cells.size() < idx + 8) {
                    continue;
                }

                String timeText = text(cells.get(idx));
                String playHtml = text(cells.get(idx + 1));
                String relayHtml = text(cells.get(idx + 2));
                String remark = text(cells.get(idx + 7));

                CrawledMatchDto dto = parseGame(currentDate, timeText, playHtml, relayHtml, remark);
                if (dto != null) {
                    results.add(dto);
                }
            }

            log.info("KBO 월별 크롤링 완료 - {}-{}, 수집 건수: {}", year, month, results.size());

        } catch (IOException e) {
            log.error("KBO 크롤링 실패 - {}-{}", year, month, e);
        }

        return results;
    }

    private String fetchScheduleJson(int year, int month) throws IOException {
        String gameMonth = month < 10 ? "0" + month : String.valueOf(month);

        // KBO 서버가 세션 쿠키 없이 API를 직접 두드리면 에러 페이지를 돌려주는 경우가 있어서,
        // 먼저 일정 페이지를 방문해 쿠키를 받아온 뒤 그 쿠키로 API를 호출한다.
        Connection.Response pageResponse = Jsoup.connect("https://www.koreabaseball.com/Schedule/Schedule.aspx")
                .timeout(TIMEOUT_MS)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .execute();

        Connection.Response response = Jsoup.connect(SCHEDULE_API_URL)
                .timeout(TIMEOUT_MS)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .referrer("https://www.koreabaseball.com/Schedule/Schedule.aspx")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .cookies(pageResponse.cookies())
                .method(Connection.Method.POST)
                .data("leId", String.valueOf(KBO_LEAGUE_ID))
                .data("srIdList", DEFAULT_SEASON_ID_LIST)
                .data("seasonId", String.valueOf(year))
                .data("gameMonth", gameMonth)
                .data("teamId", "")
                .ignoreContentType(true)
                .execute();

        String body = response.body();
        // KBO 서버가 응답 맨 앞에 UTF-8 BOM을 붙여서 내려줄 때가 있어 JSON 파싱 전에 제거
        if (!body.isEmpty() && body.charAt(0) == '\uFEFF') {
            body = body.substring(1);
        }
        return body;
    }

    private LocalDate parseDayCell(String dayText, int year) {
        Matcher matcher = DAY_PATTERN.matcher(dayText);
        if (!matcher.find()) {
            return null;
        }
        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        return LocalDate.of(year, month, day);
    }

    private CrawledMatchDto parseGame(LocalDate date, String timeText, String playHtml, String relayHtml, String remark) {
        LocalTime time = parseTime(timeText);
        LocalDateTime matchDateTime = time != null ? LocalDateTime.of(date, time) : date.atStartOfDay();

        Document playDoc = Jsoup.parseBodyFragment(playHtml);
        Element body = playDoc.body();

        var topSpans = body.select("> span");
        if (topSpans.size() < 2) {
            log.warn("경기 정보 파싱 실패 (팀명 없음) - date: {}, html: {}", date, playHtml);
            return null;
        }
        // KBO 스코어보드 페이지(alt="원정팀"/alt="홈팀")로 실제 순서를 확인한 결과,
        // "play" 셀의 첫 번째 span은 원정팀, 마지막 span이 홈팀이다.
        // (gameId도 "원정팀코드+홈팀코드" 순서 — 예: LGOB0 = LG(원정) + OB(홈, 두산))
        String awayTeam = topSpans.first().text();
        String homeTeam = topSpans.last().text();

        var scoreSpans = body.select("em > span.win, em > span.lose, em > span.same");

        // "종료 여부"는 점수 클래스(win/lose/same) 존재로 판단하지 않는다 —
        // 시작 전/진행 중인 경기도 스코어 칸이 0으로 기본 렌더링되면서 same 클래스가
        // 붙어있는 경우가 있어서(0-0으로 오판) 신뢰할 수 없었다.
        // 대신 relay 링크의 section=REVIEW 여부로 판단 (끝난 경기만 리뷰 버튼이 이 값으로 붙음).
        boolean finished = relayHtml != null && relayHtml.contains("section=REVIEW");

        Integer homeScore = null;
        Integer awayScore = null;
        if (finished && scoreSpans.size() == 2) {
            awayScore = Integer.parseInt(scoreSpans.get(0).text());
            homeScore = Integer.parseInt(scoreSpans.get(1).text());
        }

        // 우천취소/그라운드사정 등으로 취소된 경기는 결과 미확정 처리
        boolean cancelled = remark != null && (remark.contains("취소") || remark.contains("사정"));
        if (cancelled) {
            finished = false;
            homeScore = null;
            awayScore = null;
        }

        String gameId = extractGameId(relayHtml);
        String externalId = gameId != null
                ? gameId
                : date.toString() + "_" + homeTeam + "_" + awayTeam;

        return new CrawledMatchDto(externalId, homeTeam, awayTeam, matchDateTime, finished, false, homeScore, awayScore);
    }

    private LocalTime parseTime(String timeHtml) {
        if (timeHtml == null) {
            return null;
        }
        Matcher matcher = TIME_PATTERN.matcher(timeHtml);
        if (!matcher.find()) {
            return null;
        }
        return LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    private String extractGameId(String relayHtml) {
        if (relayHtml == null) {
            return null;
        }
        Matcher matcher = GAME_ID_PATTERN.matcher(relayHtml);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String text(JsonNode node) {
        JsonNode textNode = node.path("Text");
        return textNode.isMissingNode() ? "" : textNode.asText("");
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
