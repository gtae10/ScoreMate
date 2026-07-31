package ScoreMate.ScoreMate.crawler;

import ScoreMate.ScoreMate.crawler.dto.CrawledPlayerDto;
import ScoreMate.ScoreMate.domain.player.Player;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KBO 영문 사이트의 Player Search 페이지(팀별 전체 로스터)를 크롤링한다.
 *
 * 다른 크롤러(Standing/PlayerCrawler의 리더보드)와 달리, 이 페이지는 순수 AJAX API가
 * 아니라 **ASP.NET UpdatePanel 비동기 포스트백** 방식이다. 팀/포지션 버튼을 누르면:
 *   1. 숨겨진 input(hfTeam, hfPosition)에 값을 채우고
 *   2. 같은 페이지 자기 자신에 POST(__doPostBack)를 보내고
 *   3. 응답이 일반 HTML이 아니라 `<길이>|<타입>|<id>|<내용>|` 형식이 반복되는
 *      MS AJAX 델타 포맷으로 온다 (그 중 type=updatePanel 조각에 실제 HTML이 들어있음)
 *
 * 그래서 흐름이 이렇다:
 *   1. 페이지를 먼저 GET해서 __VIEWSTATE / __VIEWSTATEGENERATOR / __EVENTVALIDATION,
 *      그리고 세션 쿠키를 얻는다 (이 값들은 매 요청마다 새로 발급되는 1회용 토큰)
 *   2. 그 값들 + 원하는 팀 코드 + 포지션 코드를 실어서 같은 URL에 POST
 *   3. 델타 포맷 응답에서 updatePanel 조각만 뽑아 Jsoup으로 파싱
 *
 * 포지션 코드는 탭별로 "1"(투수) "2"(포수) "3,4,5,6"(내야수) "7,8,9"(외야수) 인데,
 * 전부 합친 "1,2,3,4,5,6,7,8,9"를 한 번에 보내서 팀당 요청 1번으로 끝내는 걸 시도한다.
 * (서버가 IN 절처럼 처리해줄 거라는 가정 — 실제로 안 통하면 포지션별로 나눠 4번
 *  보내는 방식으로 바꿔야 함. 이 파일의 crawlTeamRoster가 예외적인 응답을 받으면
 *  원본 응답 앞부분을 로그로 남기니 그걸 보고 판단할 것)
 */
@Slf4j
@Component
public class PlayerRosterCrawler {

    private static final String PLAYER_SEARCH_URL = "https://eng.koreabaseball.com/Teams/PlayerSearch.aspx";
    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    // 페이지 소스에서 확인한 전체 필드 prefix
    private static final String FIELD_PREFIX = "ctl00$ctl00$ctl00$ctl00$cphContainer$cphContainer$cphContent$cphContent$";
    private static final String SCRIPT_MANAGER_UNIQUE_ID = FIELD_PREFIX + "ScriptManager";
    private static final String SEARCH_BUTTON_UNIQUE_ID = FIELD_PREFIX + "lbtnSearch";
    private static final String SCRIPT_MANAGER_FIELD_NAME = FIELD_PREFIX + "ScriptManager1";
    private static final String UPDATE_PANEL_ID = "cphContainer_cphContainer_cphContent_cphContent_udpRecord";

    // 팀 코드: 영문 사이트 표기 -> Player Search 페이지에서 쓰는 짧은 코드
    private static final Map<String, String> TEAM_SEARCH_CODES = new LinkedHashMap<>();
    static {
        TEAM_SEARCH_CODES.put("SAMSUNG", "ss");
        TEAM_SEARCH_CODES.put("KT", "kt");
        TEAM_SEARCH_CODES.put("LG", "lg");
        TEAM_SEARCH_CODES.put("KIA", "ht");
        TEAM_SEARCH_CODES.put("DOOSAN", "ob");
        TEAM_SEARCH_CODES.put("HANWHA", "hh");
        TEAM_SEARCH_CODES.put("NC", "nc");
        TEAM_SEARCH_CODES.put("LOTTE", "lt");
        TEAM_SEARCH_CODES.put("SSG", "sk");
        TEAM_SEARCH_CODES.put("KIWOOM", "wo");
    }

    private static final String ALL_POSITIONS = "1,2,3,4,5,6,7,8,9";
    private static final Pattern PCODE_PATTERN = Pattern.compile("pcode=(\\d+)");

    /**
     * 전체 10개 팀의 로스터를 크롤링한다.
     */
    public List<CrawledPlayerDto> crawlAllTeamRosters() {
        List<CrawledPlayerDto> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : TEAM_SEARCH_CODES.entrySet()) {
            String teamCode = entry.getKey();
            String searchCode = entry.getValue();
            String koreanName = KboTeamNameMapper.toKoreanName(teamCode);

            try {
                results.addAll(crawlTeamRoster(teamCode, searchCode, koreanName));
            } catch (IOException e) {
                log.error("팀 로스터 크롤링 실패 - team: {}", teamCode, e);
            }
        }

        log.info("KBO 전체 로스터 크롤링 완료 - 수집 건수: {}", results.size());
        return results;
    }

    private List<CrawledPlayerDto> crawlTeamRoster(String teamCode, String searchCode, String koreanName) throws IOException {
        // 1단계: 페이지 GET, 최신 VIEWSTATE류 값 + 쿠키 획득
        Connection.Response pageResponse = Jsoup.connect(PLAYER_SEARCH_URL)
                .timeout(TIMEOUT_MS)
                .userAgent(USER_AGENT)
                .method(Connection.Method.GET)
                .execute();

        Document page = pageResponse.parse();
        String viewState = value(page, "__VIEWSTATE");
        String viewStateGenerator = value(page, "__VIEWSTATEGENERATOR");
        String eventValidation = value(page, "__EVENTVALIDATION");

        // 2단계: 팀/포지션 값을 채워서 같은 URL에 비동기 포스트백
        Connection.Response postResponse = Jsoup.connect(PLAYER_SEARCH_URL)
                .timeout(TIMEOUT_MS)
                .userAgent(USER_AGENT)
                .referrer(PLAYER_SEARCH_URL)
                .cookies(pageResponse.cookies())
                .header("X-MicrosoftAjax", "Delta=true")
                .header("X-Requested-With", "XMLHttpRequest")
                .method(Connection.Method.POST)
                .data(SCRIPT_MANAGER_FIELD_NAME, SCRIPT_MANAGER_UNIQUE_ID + "|" + SEARCH_BUTTON_UNIQUE_ID)
                .data("__EVENTTARGET", SEARCH_BUTTON_UNIQUE_ID)
                .data("__EVENTARGUMENT", "")
                .data("__VIEWSTATE", viewState)
                .data("__VIEWSTATEGENERATOR", viewStateGenerator)
                .data("__EVENTVALIDATION", eventValidation)
                .data(FIELD_PREFIX + "hfTeam", searchCode)
                .data(FIELD_PREFIX + "hfPosition", ALL_POSITIONS)
                .data("__ASYNCPOST", "true")
                .ignoreContentType(true)
                .execute();

        String body = postResponse.body();
        String panelHtml = extractUpdatePanelHtml(body);

        if (panelHtml == null) {
            String snippet = body.length() > 2000 ? body.substring(0, 2000) : body;
            log.error("팀 로스터 응답 파싱 실패 - team: {}, 전체 길이: {}, 응답 앞부분: {}", teamCode, body.length(), snippet);
            return List.of();
        }

        return parsePlayers(panelHtml, teamCode, koreanName);
    }

    /**
     * MS AJAX 델타 포맷을 파싱한다.
     *
     * 실제 관찰된 형식: {@code 1|#||<블록개수>|<길이>|<타입>|<id>|<내용>|<길이>|<타입>|<id>|<내용>|...}
     *  - 맨 앞 "1|#||"는 고정 헤더
     *  - 그 다음 숫자는 이어질 블록 개수
     *  - 각 블록은 "<길이>|<타입>|<id>|<내용>|" 이고, <길이>는 <내용>만의 문자 길이
     *    (타입/id는 포함 안 됨 — __VIEWSTATE 블록의 길이가 실제 base64 값 길이와
     *     정확히 일치하는 걸로 확인함)
     */
    private String extractUpdatePanelHtml(String delta) {
        int idx = 0;

        if (delta.startsWith("1|#||")) {
            idx = 5;
        }

        // 블록 개수 필드 건너뜀
        int countPipe = delta.indexOf('|', idx);
        if (countPipe < 0) {
            return null;
        }
        idx = countPipe + 1;

        while (idx < delta.length()) {
            int lenPipe = delta.indexOf('|', idx);
            if (lenPipe < 0) {
                break;
            }
            int contentLen;
            try {
                contentLen = Integer.parseInt(delta.substring(idx, lenPipe));
            } catch (NumberFormatException e) {
                break;
            }

            int typePipe = delta.indexOf('|', lenPipe + 1);
            if (typePipe < 0) {
                break;
            }
            String type = delta.substring(lenPipe + 1, typePipe);

            int idPipe = delta.indexOf('|', typePipe + 1);
            if (idPipe < 0) {
                break;
            }
            String id = delta.substring(typePipe + 1, idPipe);

            int contentStart = idPipe + 1;
            if (contentStart + contentLen > delta.length()) {
                break;
            }
            String content = delta.substring(contentStart, contentStart + contentLen);
            idx = contentStart + contentLen + 1; // 내용 뒤 구분자 '|' 하나 건너뜀

            if ("updatePanel".equals(type) && UPDATE_PANEL_ID.equals(id)) {
                return content;
            }
        }
        return null;
    }

    private List<CrawledPlayerDto> parsePlayers(String panelHtml, String teamCode, String koreanName) {
        List<CrawledPlayerDto> results = new ArrayList<>();

        Document doc = Jsoup.parseBodyFragment(panelHtml);
        Elements rows = doc.select("table[summary=\"player list\"] tbody tr");

        for (Element row : rows) {
            Element nameLink = row.selectFirst("th[scope=row] a, td[title=player] a");
            if (nameLink == null) {
                continue; // 빈 행(선수 없음 placeholder)일 수 있음
            }

            Matcher matcher = PCODE_PATTERN.matcher(nameLink.attr("href"));
            if (!matcher.find()) {
                continue;
            }
            String pcode = matcher.group(1);
            String name = nameLink.text();

            String noText = text(row, "td[title=no.]");
            Integer backNumber = parseIntSafe(noText);

            String positionText = text(row, "td[title=position]");
            Player.Position position = "Pitcher".equalsIgnoreCase(positionText)
                    ? Player.Position.PITCHER
                    : Player.Position.BATTER;

            results.add(new CrawledPlayerDto(pcode, name, teamCode, koreanName, position, backNumber));
        }

        log.info("팀 로스터 크롤링 완료 - team: {}, 수집 건수: {}", teamCode, results.size());
        return results;
    }

    private String value(Document doc, String elementId) {
        Element el = doc.getElementById(elementId);
        return el != null ? el.val() : "";
    }

    private String text(Element row, String cssQuery) {
        Element cell = row.selectFirst(cssQuery);
        return cell != null ? cell.text() : "";
    }

    private Integer parseIntSafe(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
