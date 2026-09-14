package ScoreMate.ScoreMate.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KBO 문자중계(LiveText) 화면이 실제로 그리는 "지금 이 순간" 상태(주자/B-S-O 카운트/수비 포지션/
 * 타석 타자)를 크롤링한다.
 *
 * LiveText.aspx 자체는 빈 뼈대(div#smsLeft, div#smsRight)만 내려주고, 화면에 보이는 모든 내용은
 * 자바스크립트가 아래 두 엔드포인트를 폴링(각각 13초/10초 간격)해서 innerHTML로 채워 넣는 방식이다
 * (LiveText.aspx를 GET해서 받은 HTML을 브라우저 없이 그대로 파싱하면 아무 것도 못 건지는 이유):
 *  - LiveTextView1.aspx: 이닝별 박스스코어, 다른 구장 진행상황 요약
 *  - LiveTextView2.aspx: 투수/타자 전적 박스, 문자중계 텍스트, 그리고 이 크롤러가 쓰는
 *    ".economy" 블록(현재 이닝/주자/B-S-O/수비 포지션/타석 타자)
 * 즉 "별도의 JSON API"는 없고, 두 ASPX 엔드포인트가 매번 새로 렌더링한 HTML 조각을 돌려주는
 * 구조다 (다른 크롤러들과 동일하게 jsoup으로 파싱 가능).
 *
 * ".economy" 블록 구조 (실제 응답, 2026-08-30 종료 경기 기준으로 확인):
 * <pre>
 * &lt;div class="economy"&gt;
 *   &lt;p class="present"&gt;
 *     &lt;span class="base"&gt;
 *       &lt;strong&gt;9회 초&lt;/strong&gt;
 *       &lt;img id="imgThisGameBase" src=".../ground_base0.png" alt="주자" /&gt;
 *       &lt;strong&gt;3-3 3out&lt;/strong&gt;
 *     &lt;/span&gt;
 *   &lt;/p&gt;
 *   &lt;div class="playerName"&gt;
 *     &lt;ul&gt;
 *       &lt;li class="pitcher"&gt;박신지&lt;/li&gt;
 *       &lt;li class="catcher"&gt;김기연&lt;/li&gt;
 *       &lt;li class="firBase"&gt;강승호&lt;/li&gt;
 *       &lt;li class="secondBase"&gt;이유찬&lt;/li&gt;
 *       &lt;li class="thirdBase"&gt;임종성&lt;/li&gt;
 *       &lt;li class="shortstop"&gt;박지훈&lt;/li&gt;
 *       &lt;li class="leftFielder"&gt;전다민&lt;/li&gt;
 *       &lt;li class="centerFielder"&gt;조수행&lt;/li&gt;
 *       &lt;li class="rightFielder"&gt;류승민&lt;/li&gt;
 *       &lt;li class="supervision2"&gt;박찬혁&lt;/li&gt; &lt;!-- 타석에 있는 타자 --&gt;
 *     &lt;/ul&gt;
 *   &lt;/div&gt;
 *   &lt;div class="sbo"&gt;
 *     &lt;div class="b"&gt;&lt;ul&gt;&lt;li class='on'&gt;ball&lt;/li&gt;...&lt;/ul&gt;&lt;/div&gt;
 *     &lt;div class="s"&gt;&lt;ul&gt;&lt;li class='on'&gt;strike&lt;/li&gt;...&lt;/ul&gt;&lt;/div&gt;
 *     &lt;div class="o"&gt;&lt;ul&gt;&lt;li class='on'&gt;out&lt;/li&gt;...&lt;/ul&gt;&lt;/div&gt;
 *   &lt;/div&gt;
 * &lt;/div&gt;
 * </pre>
 *
 * 수비 포지션(투수~우익수)과 타석 타자는 CSS(sms.min.css)의 좌표 지정(.economy .playerName
 * .pitcher{left:189px;top:122px} 등)으로 클래스명이 고정돼 있음을 확인했다 — 그래픽이 아니라
 * 이 텍스트 목록 자체가 "다이아몬드 위 수비 배치"의 실제 데이터 소스다.
 *
 * 주자 위치는 &lt;img id="imgThisGameBase"&gt;의 src 파일명(ground_base{코드}.png)으로 내려온다.
 * 코드는 "점유된 루 번호를 오름차순으로 이어붙인 문자열"이다 — 실제 진행 중이던 경기 25건을
 * 표본으로 모아 ground_base0/1/2/12/13/23/123.png 7가지를 확인했고(3루만 단독 점유는 표본에
 * 안 걸렸지만 규칙상 "3"일 것), 전부 이 규칙과 정확히 일치했다. 예: "12" = 1·2루,
 * "123" = 만루. 그래서 코드 문자열에 '1'/'2'/'3'이 포함돼 있는지만 보면 각 루의 점유 여부를
 * 바로 알 수 있다(0은 어떤 숫자도 포함하지 않으므로 별도 분기 없이 자연스럽게 "주자 없음"이
 * 된다). 주자 "이름"은 이 블록에 없고, 문자중계 텍스트(예: "1루주자 정수빈 : 홈인")에서만
 * 얻을 수 있다 (LiveTextCrawler.recentPlays 참고).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveBoxCrawler {

    private static final String LIVE_TEXT_VIEW2_URL = "https://www.koreabaseball.com/Game/LiveTextView2.aspx";
    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private static final Pattern BASE_STATE_PATTERN = Pattern.compile("ground_base(\\d+)\\.png");

    private final LiveTextCrawler liveTextCrawler;

    public record LiveBoxState(
            String inningText,      // 예: "9회 초"
            boolean runnerOnFirst,
            boolean runnerOnSecond,
            boolean runnerOnThird,
            int ballCount,
            int strikeCount,
            int outCount,
            String currentBatter,
            String pitcher,
            String catcher,
            String firstBase,
            String secondBase,
            String thirdBase,
            String shortstop,
            String leftFielder,
            String centerFielder,
            String rightFielder
    ) {
    }

    /**
     * 날짜 + 원정팀명 + 홈팀명으로 gameId를 조합해서 바로 크롤링한다.
     * gameId 조합 규칙은 LiveTextCrawler와 동일하므로 그대로 위임한다.
     */
    public LiveBoxState crawlLiveBox(java.time.LocalDate date, String awayTeam, String homeTeam) {
        String gameId = liveTextCrawler.buildGameId(date, awayTeam, homeTeam);
        if (gameId == null) {
            log.warn("LiveBox gameId 조합 실패 - date: {}, away: {}, home: {}", date, awayTeam, homeTeam);
            return null;
        }
        return crawlLiveBox(gameId);
    }

    public LiveBoxState crawlLiveBox(String gameId) {
        try {
            String referer = "https://www.koreabaseball.com/Game/LiveText.aspx?leagueId=1&seriesId=0&gameId="
                    + gameId + "&gyear=" + gameId.substring(0, 4);

            Connection.Response response = Jsoup.connect(LIVE_TEXT_VIEW2_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent(USER_AGENT)
                    .referrer(referer)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .method(Connection.Method.POST)
                    .data("leagueId", "1")
                    .data("seriesId", "0")
                    .data("gameId", gameId)
                    .data("gyear", gameId.substring(0, 4))
                    .ignoreContentType(true)
                    .execute();

            LiveBoxState state = parseLiveBoxHtml(response.body());
            if (state == null) {
                log.warn("LiveBox 디버깅 - div.economy를 못 찾음 (경기 시작 전이거나 gameId가 잘못됨) - gameId: {}", gameId);
            }
            return state;

        } catch (IOException e) {
            log.error("LiveBox 크롤링 실패 - gameId: {}", gameId, e);
            return null;
        }
    }

    /**
     * 네트워크 호출 없이 이미 받아온(혹은 테스트 픽스처로 만든) LiveTextView2.aspx 응답 HTML을
     * 그대로 파싱한다. crawlLiveBox가 내부적으로 이 메서드를 쓰고, 단위 테스트에서 실제 KBO
     * 서버 없이 ".economy" 블록 파싱 로직만 검증할 때도 그대로 쓸 수 있다.
     */
    public LiveBoxState parseLiveBoxHtml(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        Element economy = doc.selectFirst("div.economy");
        if (economy == null) {
            return null;
        }
        return parseEconomy(economy);
    }

    private LiveBoxState parseEconomy(Element economy) {
        var baseStrongs = economy.select(".present .base strong");
        String inningText = baseStrongs.isEmpty() ? null : baseStrongs.first().text();

        boolean runnerOnFirst = false;
        boolean runnerOnSecond = false;
        boolean runnerOnThird = false;
        Element baseImg = economy.selectFirst("#imgThisGameBase");
        if (baseImg != null) {
            Matcher matcher = BASE_STATE_PATTERN.matcher(baseImg.attr("src"));
            if (matcher.find()) {
                String baseCode = matcher.group(1); // 예: "0", "1", "12", "123"
                runnerOnFirst = baseCode.contains("1");
                runnerOnSecond = baseCode.contains("2");
                runnerOnThird = baseCode.contains("3");
            }
        }

        int ballCount = economy.select(".sbo .b li.on").size();
        int strikeCount = economy.select(".sbo .s li.on").size();
        int outCount = economy.select(".sbo .o li.on").size();

        String currentBatter = positionText(economy, "supervision2");

        return new LiveBoxState(
                inningText,
                runnerOnFirst,
                runnerOnSecond,
                runnerOnThird,
                ballCount,
                strikeCount,
                outCount,
                currentBatter,
                positionText(economy, "pitcher"),
                positionText(economy, "catcher"),
                positionText(economy, "firBase"),
                positionText(economy, "secondBase"),
                positionText(economy, "thirdBase"),
                positionText(economy, "shortstop"),
                positionText(economy, "leftFielder"),
                positionText(economy, "centerFielder"),
                positionText(economy, "rightFielder")
        );
    }

    private String positionText(Element economy, String cssClass) {
        Element el = economy.selectFirst(".playerName li." + cssClass);
        if (el == null) {
            return null;
        }
        String text = el.text().trim();
        return text.isBlank() ? null : text;
    }
}
