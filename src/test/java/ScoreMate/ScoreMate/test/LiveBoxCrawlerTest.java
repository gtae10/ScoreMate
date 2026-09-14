package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.LiveBoxCrawler;
import ScoreMate.ScoreMate.crawler.LiveTextCrawler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LiveBoxCrawler가 LiveTextView2.aspx의 ".economy" 블록(주자/B-S-O/수비 포지션/타석 타자)을
 * 제대로 뽑는지 확인한다.
 *
 * 이 파일의 테스트는 두 종류로 나뉜다:
 * - 실동작 테스트(아래 두 개, "실제 KBO 사이트로..." 주석이 붙은 것): 네트워크 요청을 보내므로
 *   평소 빌드/CI에서 자동으로 돌면 안 되니, 확인이 필요할 때만 메서드를 직접 선택해서(IntelliJ
 *   메서드 옆 초록 버튼) 수동으로 실행할 것.
 * - 픽스처 기반 테스트(그 아래 전부): 네트워크 없이 parseLiveBoxHtml()에 클래스 상단 javadoc에
 *   문서화된 실제 응답 구조를 그대로 본뜬 HTML 문자열을 넣어서 파싱 로직만 검증한다. 무주자/만루처럼
 *   실제 경기에서 특정 순간을 찾기 번거로운 케이스도 결정적으로(deterministic) 테스트할 수 있어서,
 *   평소 빌드에서도 자동으로 돈다.
 */
class LiveBoxCrawlerTest {

    private final LiveTextCrawler liveTextCrawler = new LiveTextCrawler();
    private final LiveBoxCrawler liveBoxCrawler = new LiveBoxCrawler(liveTextCrawler);

    /**
     * 아래는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로 돌면 안 되니,
     * 확인이 필요할 때만 메서드를 직접 선택해서(IntelliJ 메서드 옆 초록 버튼) 수동으로 실행할 것.
     *
     * gameId는 실제로 있었던 경기 기준으로 팀명/날짜를 바꿔서 실행할 것. 경기가 시작 전이면
     * div.economy 자체가 없어서 null이 반환될 수 있는데, 그건 정상이다.
     */
    @Test
    @Tag("manual")
    void 현재_경기_상태를_크롤링한다() {
        // 실제로 있었던(혹은 진행 중인) 경기로 바꿔서 실행할 것 (오늘 날짜 + 실제 대진)
        String gameId = liveTextCrawler.buildGameId(LocalDate.of(2026, 8, 30), "키움", "두산");

        LiveBoxCrawler.LiveBoxState state = liveBoxCrawler.crawlLiveBox(gameId);

        if (state == null) {
            System.out.println("state가 null - 경기 시작 전이거나 gameId가 잘못됐을 수 있음");
            return;
        }

        System.out.println("이닝: " + state.inningText());
        System.out.println("주자 - 1루: " + state.runnerOnFirst() + ", 2루: " + state.runnerOnSecond()
                + ", 3루: " + state.runnerOnThird());
        System.out.println("카운트: " + state.ballCount() + "B " + state.strikeCount() + "S " + state.outCount() + "O");
        System.out.println("타석 타자: " + state.currentBatter());
        System.out.println("수비 - 투수: " + state.pitcher() + ", 포수: " + state.catcher()
                + ", 1루수: " + state.firstBase() + ", 2루수: " + state.secondBase()
                + ", 3루수: " + state.thirdBase() + ", 유격수: " + state.shortstop()
                + ", 좌익수: " + state.leftFielder() + ", 중견수: " + state.centerFielder()
                + ", 우익수: " + state.rightFielder());
    }

    /**
     * 2026-04-21 NC-키움 경기는 9회초 종료 시점에 1·2루에 주자가 있는 상태로 얼려 있는 것을
     * 실제로 확인했다(ground_base12.png). 주자 코드 해석("1"/"2"/"3" 포함 여부)이 맞는지
     * 실제 응답으로 회귀 확인한다 (네트워크 요청 - 수동 실행).
     */
    @Test
    @Tag("manual")
    void 주자_상태를_정확히_해석한다() {
        LiveBoxCrawler.LiveBoxState state = liveBoxCrawler.crawlLiveBox("20260421NCWO0");

        assertThat(state).isNotNull();
        assertThat(state.runnerOnFirst()).isTrue();
        assertThat(state.runnerOnSecond()).isTrue();
        assertThat(state.runnerOnThird()).isFalse();
    }

    /**
     * 주자 조합(무주자/만루/3루만/1·3루)이 ground_base{코드}.png 규칙대로 전부 정확히 해석되는지
     * 픽스처로 확인한다. 실제 경기에서 이 조합들을 전부 찾는 건 비현실적이라(특히 무주자·만루는
     * 순간적으로만 존재) 클래스 상단 javadoc에 문서화된 실제 응답 구조를 그대로 본뜬 HTML로 검증한다.
     */
    @ParameterizedTest(name = "베이스코드 {0} -> 1루:{1} 2루:{2} 3루:{3}")
    @MethodSource("baseCodeScenarios")
    void 주자_조합을_정확히_해석한다(String baseCode, boolean first, boolean second, boolean third) {
        String html = economyHtml(baseCode, 0, 0, 0);

        LiveBoxCrawler.LiveBoxState state = liveBoxCrawler.parseLiveBoxHtml(html);

        assertThat(state).isNotNull();
        assertThat(state.runnerOnFirst()).isEqualTo(first);
        assertThat(state.runnerOnSecond()).isEqualTo(second);
        assertThat(state.runnerOnThird()).isEqualTo(third);
    }

    static Stream<Arguments> baseCodeScenarios() {
        return Stream.of(
                Arguments.of("0", false, false, false),   // 무주자
                Arguments.of("3", false, false, true),    // 3루만 주자
                Arguments.of("13", true, false, true),    // 1·3루
                Arguments.of("123", true, true, true)     // 만루
        );
    }

    @Test
    void 이닝_시작_직후_BSO는_0이다() {
        String html = economyHtml("0", 0, 0, 0);

        LiveBoxCrawler.LiveBoxState state = liveBoxCrawler.parseLiveBoxHtml(html);

        assertThat(state).isNotNull();
        assertThat(state.ballCount()).isZero();
        assertThat(state.strikeCount()).isZero();
        assertThat(state.outCount()).isZero();
    }

    @Test
    void BSO_카운트와_수비_포지션_타석타자를_정확히_해석한다() {
        String html = economyHtml("123", 2, 1, 1);

        LiveBoxCrawler.LiveBoxState state = liveBoxCrawler.parseLiveBoxHtml(html);

        assertThat(state).isNotNull();
        assertThat(state.ballCount()).isEqualTo(2);
        assertThat(state.strikeCount()).isEqualTo(1);
        assertThat(state.outCount()).isEqualTo(1);
        assertThat(state.currentBatter()).isEqualTo("타자A");
        assertThat(state.pitcher()).isEqualTo("투수A");
        assertThat(state.catcher()).isEqualTo("포수A");
        assertThat(state.firstBase()).isEqualTo("1루수A");
        assertThat(state.secondBase()).isEqualTo("2루수A");
        assertThat(state.thirdBase()).isEqualTo("3루수A");
        assertThat(state.shortstop()).isEqualTo("유격수A");
        assertThat(state.leftFielder()).isEqualTo("좌익수A");
        assertThat(state.centerFielder()).isEqualTo("중견수A");
        assertThat(state.rightFielder()).isEqualTo("우익수A");
    }

    @Test
    void economy_블록이_없으면_null을_반환한다() {
        LiveBoxCrawler.LiveBoxState state = liveBoxCrawler.parseLiveBoxHtml("<div id=\"smsLeft\"></div>");

        assertThat(state).isNull();
    }

    /**
     * 클래스 상단 javadoc에 있는 실제 ".economy" 응답 구조를 그대로 본뜬 최소 HTML 픽스처를 만든다.
     */
    private String economyHtml(String baseCode, int balls, int strikes, int outs) {
        return "<div class=\"economy\">"
                + "<p class=\"present\"><span class=\"base\">"
                + "<strong>5회 초</strong>"
                + "<img id=\"imgThisGameBase\" src=\"https://www.koreabaseball.com/img/game/ground_base" + baseCode + ".png\" alt=\"주자\" />"
                + "<strong>" + balls + "-" + strikes + " " + outs + "out</strong>"
                + "</span></p>"
                + "<div class=\"playerName\"><ul>"
                + "<li class=\"pitcher\">투수A</li>"
                + "<li class=\"catcher\">포수A</li>"
                + "<li class=\"firBase\">1루수A</li>"
                + "<li class=\"secondBase\">2루수A</li>"
                + "<li class=\"thirdBase\">3루수A</li>"
                + "<li class=\"shortstop\">유격수A</li>"
                + "<li class=\"leftFielder\">좌익수A</li>"
                + "<li class=\"centerFielder\">중견수A</li>"
                + "<li class=\"rightFielder\">우익수A</li>"
                + "<li class=\"supervision2\">타자A</li>"
                + "</ul></div>"
                + "<div class=\"sbo\">"
                + dotsHtml("b", balls)
                + dotsHtml("s", strikes)
                + dotsHtml("o", outs)
                + "</div></div>";
    }

    private String dotsHtml(String cssClass, int onCount) {
        StringBuilder sb = new StringBuilder("<div class=\"" + cssClass + "\"><ul>");
        for (int i = 0; i < onCount; i++) {
            sb.append("<li class='on'>dot</li>");
        }
        sb.append("</ul></div>");
        return sb.toString();
    }
}
