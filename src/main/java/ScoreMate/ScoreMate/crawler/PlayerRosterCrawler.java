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
 * KBO 한글 사이트의 "선수 등록 현황"(팀별) 페이지를 크롤링해서 팀별 전체 로스터를 가져온다.
 * (영문 사이트 Player Search는 사이트 자체 JS 버그로 막혀서 포기했던 것과 별개 — 이 페이지는
 * 한글 사이트 koreabaseball.com이고, 실제 확인 결과 정상적으로 동작한다)
 *
 * 팀 전환은 fnSearchChange(teamId) 자바스크립트가 처리한다:
 *   1. hfSearchTeam 숨겨진 필드에 팀 코드를 넣고
 *   2. __doPostBack('...btnCalendarSelect', '') 로 비동기 포스트백
 *
 * 그래서 흐름은:
 *   1. 페이지를 GET해서 __VIEWSTATE류 값 + hfSearchDate(오늘 날짜) + 쿠키를 얻는다
 *   2. 팀마다 hfSearchTeam 값을 바꿔서 POST (매 팀마다 새로 GET부터 다시 하는 게 안전 —
 *      VIEWSTATE가 매 포스트백마다 갱신되는 1회용 토큰이라 재사용하면 깨질 수 있어서)
 *   3. 델타 응답(updatePanel 조각)에서 "감독/코치/투수/포수/내야수/외야수" 표를 파싱
 *      (감독/코치는 선수가 아니므로 건너뛰고, 투수는 PITCHER, 나머지는 BATTER로 매핑)
 */
@Slf4j
@Component
public class PlayerRosterCrawler {

    private static final String REGISTER_URL = "https://www.koreabaseball.com/Player/Register.aspx";
    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private static final String FIELD_PREFIX = "ctl00$ctl00$ctl00$cphContents$cphContents$cphContents$";
    private static final String EVENT_TARGET = FIELD_PREFIX + "btnCalendarSelect";
    private static final String UPDATE_PANEL_UNIQUE_ID = FIELD_PREFIX + "udpRecord";
    private static final String SCRIPT_MANAGER_FIELD_NAME = FIELD_PREFIX + "ScriptManager1";
    private static final String UPDATE_PANEL_ID = "cphContents_cphContents_cphContents_udpRecord";

    private static final Map<String, String> TEAM_SEARCH_CODES = new LinkedHashMap<>();
    static {
        TEAM_SEARCH_CODES.put("SS", "삼성");
        TEAM_SEARCH_CODES.put("KT", "KT");
        TEAM_SEARCH_CODES.put("LG", "LG");
        TEAM_SEARCH_CODES.put("HT", "KIA");
        TEAM_SEARCH_CODES.put("OB", "두산");
        TEAM_SEARCH_CODES.put("HH", "한화");
        TEAM_SEARCH_CODES.put("NC", "NC");
        TEAM_SEARCH_CODES.put("LT", "롯데");
        TEAM_SEARCH_CODES.put("SK", "SSG");
        TEAM_SEARCH_CODES.put("WO", "키움");
    }

    private static final Pattern PLAYER_ID_PATTERN = Pattern.compile("playerId=(\\d+)");

    public List<CrawledPlayerDto> crawlAllTeamRosters() {
        List<CrawledPlayerDto> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : TEAM_SEARCH_CODES.entrySet()) {
            try {
                results.addAll(crawlTeamRoster(entry.getKey(), entry.getValue()));
            } catch (IOException e) {
                log.error("팀 로스터 크롤링 실패 - team: {}", entry.getValue(), e);
            }
        }

        log.info("KBO 전체 로스터 크롤링 완료 - 수집 건수: {}", results.size());
        return results;
    }

    private List<CrawledPlayerDto> crawlTeamRoster(String teamCode, String koreanName) throws IOException {
        Connection.Response pageResponse = Jsoup.connect(REGISTER_URL)
                .timeout(TIMEOUT_MS)
                .userAgent(USER_AGENT)
                .execute();

        Document page = pageResponse.parse();
        String viewState = value(page, "__VIEWSTATE");
        String viewStateGenerator = value(page, "__VIEWSTATEGENERATOR");
        String eventValidation = value(page, "__EVENTVALIDATION");
        String searchDate = value(page, "cphContents_cphContents_cphContents_hfSearchDate");

        Connection.Response postResponse = Jsoup.connect(REGISTER_URL)
                .timeout(TIMEOUT_MS)
                .userAgent(USER_AGENT)
                .referrer(REGISTER_URL)
                .cookies(pageResponse.cookies())
                .header("X-MicrosoftAjax", "Delta=true")
                .header("X-Requested-With", "XMLHttpRequest")
                .method(Connection.Method.POST)
                .data("__EVENTTARGET", EVENT_TARGET)
                .data("__EVENTARGUMENT", "")
                .data("__VIEWSTATE", viewState)
                .data("__VIEWSTATEGENERATOR", viewStateGenerator)
                .data("__EVENTVALIDATION", eventValidation)
                .data(SCRIPT_MANAGER_FIELD_NAME, UPDATE_PANEL_UNIQUE_ID + "|" + EVENT_TARGET)
                .data(FIELD_PREFIX + "hfSearchTeam", teamCode)
                .data(FIELD_PREFIX + "hfSearchDate", searchDate)
                .data("__ASYNCPOST", "true")
                .ignoreContentType(true)
                .execute();

        String body = postResponse.body();
        String panelHtml = extractUpdatePanelHtml(body);

        if (panelHtml == null) {
            String head = body.length() > 300 ? body.substring(0, 300) : body;
            String tail = body.length() > 800 ? body.substring(body.length() - 800) : body;
            log.error("팀 로스터 응답 파싱 실패 - team: {}, 전체 길이: {}\n앞부분: {}\n뒷부분: {}", koreanName, body.length(), head, tail);
            return List.of();
        }

        return parseRoster(panelHtml, teamCode, koreanName);
    }

    /**
     * MS AJAX 델타 포맷: {@code 1|#||<블록개수>|<길이>|<타입>|<id>|<내용>|...}
     * <길이>는 <내용>만의 문자 길이. (KboCrawler/PlayerRosterCrawler 다른 페이지에서도 검증된 로직)
     */
    private String extractUpdatePanelHtml(String delta) {
        int idx = 0;
        if (delta.startsWith("1|#||")) {
            idx = 5;
        }
        int countPipe = delta.indexOf('|', idx);
        if (countPipe < 0) {
            return null;
        }
        idx = countPipe + 1;

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
                return content;
            }
        }
        return null;
    }

    private List<CrawledPlayerDto> parseRoster(String panelHtml, String teamCode, String koreanName) {
        List<CrawledPlayerDto> results = new ArrayList<>();
        Document doc = Jsoup.parseBodyFragment(panelHtml);

        Elements tables = doc.select("table");
        for (Element table : tables) {
            Elements headerCells = table.select("thead th");
            if (headerCells.size() < 2) {
                continue;
            }
            String positionLabel = headerCells.get(1).text(); // "감독", "코치", "투수", "포수", "내야수", "외야수"

            Player.Position position;
            if (positionLabel.contains("투수")) {
                position = Player.Position.PITCHER;
            } else if (positionLabel.contains("포수") || positionLabel.contains("내야") || positionLabel.contains("외야")) {
                position = Player.Position.BATTER;
            } else {
                continue; // 감독/코치는 선수가 아니므로 건너뜀
            }

            for (Element row : table.select("tbody tr")) {
                Element nameLink = row.selectFirst("td a");
                if (nameLink == null) {
                    continue;
                }
                Matcher matcher = PLAYER_ID_PATTERN.matcher(nameLink.attr("href"));
                if (!matcher.find()) {
                    continue;
                }
                String playerId = matcher.group(1);
                String name = nameLink.text();

                Elements cells = row.select("td");
                Integer backNumber = parseIntSafe(cells.isEmpty() ? "" : cells.get(0).text());

                results.add(new CrawledPlayerDto(playerId, name, teamCode, koreanName, position, backNumber));
            }
        }

        log.info("팀 로스터 크롤링 완료 - team: {}, 수집 건수: {}", koreanName, results.size());
        return results;
    }

    private String value(Document doc, String elementId) {
        Element el = doc.getElementById(elementId);
        return el != null ? el.val() : "";
    }

    private Integer parseIntSafe(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
