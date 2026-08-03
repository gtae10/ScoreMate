package ScoreMate.ScoreMate.crawler;

import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KBO 스코어보드 페이지를 크롤링해서 실시간 상태/스코어, 그리고(과거 날짜의 경우)
 * 승/패 투수를 가져온다.
 *
 * 오늘 날짜는 자바스크립트/AJAX 없이 서버가 바로 렌더링해줘서 GET 한 번이면 충분하다.
 * 과거 날짜는 "이전날짜" 버튼(btnPreDate)의 비동기 포스트백을 그대로 흉내내서,
 * 하루씩 뒤로 이동하며 그때그때 데이터를 모은다 (한 번에 임의의 날짜로 점프는 안 됨 —
 * 캘린더 선택도 결국 서버가 하루하루 계산하는 방식이라 여러 날 백필하려면 이 방법뿐이었다).
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
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private static final Pattern GAME_ID_PATTERN = Pattern.compile("gameId=(\\d{8}[A-Z]{4}\\d)");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2})");

    // Sys.WebForms.PageRequestManager._initialize(...) 등록 코드에서 확인한 실제 필드명
    private static final String FIELD_PREFIX = "ctl00$ctl00$ctl00$cphContents$cphContents$cphContents$";
    private static final String EVENT_TARGET_PREV_DATE = FIELD_PREFIX + "btnPreDate";
    private static final String UPDATE_PANEL_UNIQUE_ID = FIELD_PREFIX + "udpRecord";
    // 주의: 이 페이지는 필드명이 소문자 "scriptmanager1" (Register.aspx의 "ScriptManager1"과 다름)
    private static final String SCRIPT_MANAGER_FIELD_NAME = FIELD_PREFIX + "scriptmanager1";
    // 캘린더 선택(btnCalendarSelect)뿐 아니라 이전/다음날짜 이동도 이 필드를 요구한다.
    private static final String SEARCH_DATE_FIELD_NAME = FIELD_PREFIX + "hfSearchDate";
    private static final java.time.format.DateTimeFormatter DATE_FIELD_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String UPDATE_PANEL_ID = "cphContents_cphContents_cphContents_udpRecord";

    /**
     * 오늘 경기의 실시간 상태/스코어를 크롤링한다.
     */
    public List<CrawledMatchDto> crawlToday() {
        List<CrawledMatchDto> results = new ArrayList<>();
        LocalDate today = LocalDate.now();

        try {
            Document doc = Jsoup.connect(SCOREBOARD_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent(USER_AGENT)
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

    /**
     * 오늘부터 daysBack일 전까지, 하루씩 "이전날짜" 버튼을 눌러가며 데이터를 모은다.
     * 예: daysBack=3 이면 어제/그제/그끄제 3일치를 모아서 반환.
     * 주로 지난 경기의 승/패 투수를 채우는 용도 (일정 API엔 이 정보가 없어서).
     */
    public List<CrawledMatchDto> crawlRecentDays(int daysBack) {
        List<CrawledMatchDto> results = new ArrayList<>();

        try {
            Connection.Response response = Jsoup.connect(SCOREBOARD_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent(USER_AGENT)
                    .execute();

            Document page = response.parse();
            String viewState = value(page, "__VIEWSTATE");
            String viewStateGenerator = value(page, "__VIEWSTATEGENERATOR");
            String eventValidation = value(page, "__EVENTVALIDATION");
            Map<String, String> cookies = response.cookies();

            LocalDate date = LocalDate.now();

            for (int i = 1; i <= daysBack; i++) {
                // hfSearchDate는 "이동하기 전, 지금 화면에 표시된 날짜"를 요구한다
                // (실제 브라우저 요청 캡처로 확인 — 이게 없으면 서버가 에러 페이지로 리다이렉트함)
                String currentDisplayDate = date.format(DATE_FIELD_FORMAT);
                date = date.minusDays(1);

                Connection.Response postResponse = Jsoup.connect(SCOREBOARD_URL)
                        .timeout(TIMEOUT_MS)
                        .userAgent(USER_AGENT)
                        .referrer(SCOREBOARD_URL)
                        .cookies(cookies)
                        .header("X-MicrosoftAjax", "Delta=true")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .method(Connection.Method.POST)
                        .data(SCRIPT_MANAGER_FIELD_NAME, UPDATE_PANEL_UNIQUE_ID + "|" + EVENT_TARGET_PREV_DATE)
                        .data("__EVENTTARGET", EVENT_TARGET_PREV_DATE)
                        .data("__EVENTARGUMENT", "")
                        .data("__VIEWSTATE", viewState)
                        .data("__VIEWSTATEGENERATOR", viewStateGenerator)
                        .data("__EVENTVALIDATION", eventValidation)
                        .data(SEARCH_DATE_FIELD_NAME, currentDisplayDate)
                        .data("__ASYNCPOST", "true")
                        .ignoreContentType(true)
                        .execute();

                cookies = postResponse.cookies();
                String body = postResponse.body();
                DeltaResult delta = parseDelta(body);

                if (delta == null) {
                    String tail = body.length() > 800 ? body.substring(body.length() - 800) : body;
                    log.error("스코어보드 날짜 이동 응답 파싱 실패 - date: {}, 응답 뒷부분: {}", date, tail);
                    break; // 다음 날짜도 계속 실패할 가능성이 높으니 중단
                }

                // 다음 반복을 위해 갱신된 VIEWSTATE/EVENTVALIDATION을 이어서 사용
                viewState = delta.viewState() != null ? delta.viewState() : viewState;
                eventValidation = delta.eventValidation() != null ? delta.eventValidation() : eventValidation;

                Document fragment = Jsoup.parseBodyFragment(delta.panelHtml());
                Elements gameBlocks = fragment.select("div.smsScore");
                for (Element block : gameBlocks) {
                    CrawledMatchDto dto = parseGameBlock(block, date);
                    if (dto != null) {
                        results.add(dto);
                    }
                }
            }

            log.info("KBO 스코어보드 과거 {}일치 크롤링 완료 - 수집 건수: {}", daysBack, results.size());

        } catch (IOException e) {
            log.error("KBO 스코어보드 과거 날짜 크롤링 실패", e);
        }

        return results;
    }

    private CrawledMatchDto parseGameBlock(Element block, LocalDate date) {
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
        LocalDateTime matchDateTime = time != null ? LocalDateTime.of(date, time) : date.atStartOfDay();

        // p.place 안에 시간+구장명이 같이 있는 구조라, 시간 부분을 뺀 나머지를 구장명으로 본다.
        String stadium = text(block, "p.place").replaceAll("\\d{2}:\\d{2}", "").trim();
        if (stadium.isBlank()) {
            stadium = null;
        }

        // externalId는 KboCrawler와 반드시 같은 형식(날짜_홈팀_원정팀)으로 통일 —
        // 실제 gameId 문자열을 쓰면 두 크롤러 사이에서 미묘하게 어긋나 같은 경기가
        // 중복 저장되는 문제가 있었다. gameId는 "유효한 경기 블록인지" 확인용으로만 쓴다.
        String externalId = date + "_" + homeTeam + "_" + awayTeam;

        // 진행중일 때만 "5회말" 같은 실제 이닝 텍스트를 같이 담는다 (화면에 그대로 표시하기 위함)
        String liveStatusText = live ? stateText : null;

        // 종료된 경기만 <p class="win"><span>승: 이름</span><span>패: 이름</span>...</p> 형태로
        // 승/패 투수가 나온다.
        String winPitcher = null;
        String losePitcher = null;
        if (finished) {
            for (Element span : block.select("p.win span")) {
                String spanText = span.text();
                if (spanText.startsWith("승:")) {
                    winPitcher = spanText.substring(spanText.indexOf(':') + 1).trim();
                } else if (spanText.startsWith("패:")) {
                    losePitcher = spanText.substring(spanText.indexOf(':') + 1).trim();
                }
            }
        }

        return new CrawledMatchDto(externalId, homeTeam, awayTeam, matchDateTime, finished, live, false, false, homeScore, awayScore, liveStatusText, stadium, winPitcher, losePitcher);
    }

    /**
     * MS AJAX 델타 포맷을 파싱한다. {@code 1|#||<블록개수>|<길이>|<타입>|<id>|<내용>|...}
     * <길이>는 <내용>만의 문자 길이. (PlayerRosterCrawler에서 검증된 것과 같은 로직)
     * updatePanel 조각뿐 아니라, 다음 요청에 이어서 쓸 __VIEWSTATE/__EVENTVALIDATION도 같이 뽑는다.
     */
    private DeltaResult parseDelta(String delta) {
        int idx = 0;
        if (delta.startsWith("1|#||")) {
            idx = 5;
        }
        int countPipe = delta.indexOf('|', idx);
        if (countPipe < 0) {
            return null;
        }
        idx = countPipe + 1;

        String panelHtml = null;
        String viewState = null;
        String eventValidation = null;

        while (idx < delta.length()) {
            int lenPipe = delta.indexOf('|', idx);
            if (lenPipe < 0) break;
            int contentLen;
            try {
                contentLen = Integer.parseInt(delta.substring(idx, lenPipe));
            } catch (NumberFormatException e) {
                break;
            }

            int typePipe = delta.indexOf('|', lenPipe + 1);
            if (typePipe < 0) break;
            String type = delta.substring(lenPipe + 1, typePipe);

            int idPipe = delta.indexOf('|', typePipe + 1);
            if (idPipe < 0) break;
            String id = delta.substring(typePipe + 1, idPipe);

            int contentStart = idPipe + 1;
            if (contentStart + contentLen > delta.length()) break;
            String content = delta.substring(contentStart, contentStart + contentLen);
            idx = contentStart + contentLen + 1;

            if ("updatePanel".equals(type) && UPDATE_PANEL_ID.equals(id)) {
                panelHtml = content;
            } else if ("hiddenField".equals(type) && "__VIEWSTATE".equals(id)) {
                viewState = content;
            } else if ("hiddenField".equals(type) && "__EVENTVALIDATION".equals(id)) {
                eventValidation = content;
            }
        }

        return panelHtml != null ? new DeltaResult(panelHtml, viewState, eventValidation) : null;
    }

    private record DeltaResult(String panelHtml, String viewState, String eventValidation) {
    }

    private String value(Document doc, String elementId) {
        Element el = doc.getElementById(elementId);
        return el != null ? el.val() : "";
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
