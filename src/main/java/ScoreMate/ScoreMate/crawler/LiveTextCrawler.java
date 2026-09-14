package ScoreMate.ScoreMate.crawler;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KBO "문자중계"(LiveText) 페이지에서 지금 던지고 있는 투수, 타석에 선 타자,
 * 최근 투구별 코멘터리(문자중계)를 크롤링한다.
 *
 * 원정팀 박스(.playerBox.awayBox)와 홈팀 박스(.playerBox.homeBox)가 있고,
 * 그 안의 하위 div가 class="pitcher"(투수전적)면 그 팀이 지금 수비 중이라는 뜻이라
 * 거기 있는 선수가 바로 "지금 던지고 있는 투수"다. class="batter"(타자전적)면
 * 그 팀은 지금 공격 중이라 투수 정보가 없다.
 *
 * 문자중계는 이닝별로 별도 컨테이너(div.numCon)에 나뉘어 있고, 지금 보여지는
 * 이닝만 style에 "display: none"이 안 붙어있다. 그 안에 span(class="normaiflTxt"
 * 또는 "blue")들이 투구/타석 단위로 하나씩 있다.
 *
 * 다이아몬드 그래픽 자체(수비 위치 시각화)는 이 정적 HTML엔 없다 — 자바스크립트가
 * 별도로 그리는 것으로 추정되어 크롤링 대상에서 제외했다. 대신 이 데이터(투수/타자/
 * 최근 플레이)로 우리가 직접 간단한 그래픽을 그린다.
 *
 * robots.txt 확인 결과 /Game/ 경로는 막혀있지 않다.
 */
@Slf4j
@Component
public class LiveTextCrawler {

    private static final String LIVE_TEXT_URL = "https://www.koreabaseball.com/Game/LiveText.aspx";
    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    private static final DateTimeFormatter GAME_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern NUMBER_PREFIX = Pattern.compile("No\\.\\d+\\s*");
    private static final Pattern BATTER_LINE = Pattern.compile("^\\d+번타자\\s*(.+)$");
    private static final int MAX_RECENT_PLAYS = 15;

    // 경기ID 조합에 쓰는 KBO 내부 팀 코드 (PlayerRosterCrawler의 TEAM_SEARCH_CODES와 동일 체계)
    private static final Map<String, String> KOREAN_TO_CODE = Map.ofEntries(
            Map.entry("삼성", "SS"),
            Map.entry("KT", "KT"),
            Map.entry("LG", "LG"),
            Map.entry("KIA", "HT"),
            Map.entry("두산", "OB"),
            Map.entry("한화", "HH"),
            Map.entry("NC", "NC"),
            Map.entry("롯데", "LT"),
            Map.entry("SSG", "SK"),
            Map.entry("키움", "WO")
    );

    public record LiveGameState(
            String awayPitcher,
            String homePitcher,
            String currentBatter,
            List<String> recentPlays // 최신 순
    ) {
    }

    /**
     * 날짜 + 원정팀명 + 홈팀명으로 KBO gameId를 조합한다 (형식: yyyyMMdd + 원정코드 + 홈코드 + "0").
     * 더블헤더 2경기는 이 형식으로 못 잡지만(항상 "0"으로 고정), 우선 1경기 기준으로만 지원.
     */
    public String buildGameId(LocalDate date, String awayTeam, String homeTeam) {
        String awayCode = KOREAN_TO_CODE.get(awayTeam);
        String homeCode = KOREAN_TO_CODE.get(homeTeam);
        if (awayCode == null || homeCode == null) {
            return null;
        }
        return date.format(GAME_ID_DATE_FORMAT) + awayCode + homeCode + "0";
    }

    /**
     * 투수/타자/최근 문자중계를 한 번의 페이지 요청으로 같이 가져온다.
     */
    public LiveGameState crawlLiveState(String gameId) {
        try {
            String url = LIVE_TEXT_URL + "?leagueId=1&seriesId=0&gameId=" + gameId + "&gyear=" + gameId.substring(0, 4);
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent(USER_AGENT)
                    .get();

            if (!doc.html().contains("playerBox")) {
                log.warn("LiveText 디버깅 - 받은 페이지 전체 내용:\n{}", doc.html());
            }

            String awayPitcher = extractPitcherIfPitching(doc, "awayBox");
            String homePitcher = extractPitcherIfPitching(doc, "homeBox");

            List<String> lines = extractActivePlayByPlay(doc);
            String currentBatter = findCurrentBatter(lines);

            // 화면엔 최신 플레이가 위로 오게 뒤집어서, 최대 개수만 잘라서 보여준다
            List<String> recentPlays = new ArrayList<>(lines);
            Collections.reverse(recentPlays);
            if (recentPlays.size() > MAX_RECENT_PLAYS) {
                recentPlays = recentPlays.subList(0, MAX_RECENT_PLAYS);
            }

            return new LiveGameState(awayPitcher, homePitcher, currentBatter, recentPlays);

        } catch (IOException e) {
            log.error("LiveText 크롤링 실패 - gameId: {}", gameId, e);
            return new LiveGameState(null, null, null, List.of());
        }
    }

    private String extractPitcherIfPitching(Document doc, String boxClass) {
        Element box = doc.selectFirst("div.playerBox." + boxClass + " > div.pitcher");
        if (box == null) {
            Element anyBox = doc.selectFirst("div.playerBox." + boxClass);
            log.warn("LiveText 디버깅 - {} 안에 div.pitcher 없음 (타격중이거나 셀렉터 문제). playerBox 존재여부: {}",
                    boxClass, anyBox != null);
            return null; // 지금 이 팀은 타격 중 (투수전적 박스가 없음)
        }
        Element nameEl = box.selectFirst("strong.who span.no");
        if (nameEl == null) {
            return null;
        }
        // "No.58 박상원" -> "박상원"
        return NUMBER_PREFIX.matcher(nameEl.text()).replaceFirst("").trim();
    }

    /**
     * 지금 화면에 보여지는(= style에 display:none이 없는) 이닝 컨테이너 안의
     * 문자중계 줄들을 순서대로(과거→최근) 반환한다. 못 찾으면 문서상 마지막
     * 컨테이너를 최신 이닝으로 간주해서 대신 쓴다.
     */
    private List<String> extractActivePlayByPlay(Document doc) {
        Elements containers = doc.select("div.numCon");

        Element active = null;
        for (Element c : containers) {
            String style = c.attr("style").replace(" ", "");
            if (!style.contains("display:none")) {
                active = c;
            }
        }
        if (active == null && !containers.isEmpty()) {
            active = containers.last();
        }
        if (active == null) {
            log.warn("LiveText 디버깅 - div.numCon을 하나도 못 찾음. 페이지 길이: {}, '문자중계' 포함여부: {}, 'numCon' 포함여부: {}",
                    doc.html().length(), doc.html().contains("문자중계"), doc.html().contains("numCon"));
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        for (Element span : active.select("span[id*=spanLiveText_]")) {
            String text = span.text().trim();
            if (!text.isBlank()) {
                lines.add(text);
            }
        }

        if (lines.isEmpty()) {
            log.warn("LiveText 디버깅 - numCon은 찾았지만 spanLiveText가 0개. active 블록 앞부분: {}",
                    active.html().substring(0, Math.min(500, active.html().length())));
        }

        return lines;
    }

    private String findCurrentBatter(List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            Matcher matcher = BATTER_LINE.matcher(lines.get(i));
            if (matcher.matches()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }
}
