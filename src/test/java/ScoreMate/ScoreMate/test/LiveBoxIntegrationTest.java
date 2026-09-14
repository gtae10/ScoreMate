package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.LiveBoxCrawler;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.match.Match;
import ScoreMate.ScoreMate.domain.match.MatchRepository;
import ScoreMate.ScoreMate.domain.match.MatchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Match에 저장된 라이브박스 상태(주자/B-S-O/수비 포지션/타석 타자)가
 * (1) GET /api/matches/{id}/live-box JSON 응답과
 * (2) GET /matches/{id} 상세 페이지의 다이아몬드 그래픽
 * 양쪽에 제대로 반영되는지, 주자 조합별로(무주자/만루/3루만/1·3루) 확인한다.
 * 로컬 개발 DB에 테스트용 경기 1건을 만들었다가 끝나면 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LiveBoxIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchRepository matchRepository;

    private Long matchId;

    @AfterEach
    void cleanup() {
        if (matchId != null) {
            matchRepository.deleteById(matchId);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("runnerScenarios")
    void 주자_조합별로_API와_상세페이지에_반영된다(String label, boolean first, boolean second, boolean third,
                                     int balls, int strikes, int outs) throws Exception {
        String externalId = "livebox-test-" + System.nanoTime();
        Match match = matchRepository.save(Match.builder()
                .league(League.KBO)
                .homeTeam("두산")
                .awayTeam("키움")
                .matchDate(LocalDateTime.now())
                .externalId(externalId)
                .build());
        match.markLive(1, 0, "5회 초");
        match = matchRepository.save(match);
        matchId = match.getId();

        LiveBoxCrawler.LiveBoxState state = new LiveBoxCrawler.LiveBoxState(
                "5회 초", first, second, third,
                balls, strikes, outs,
                "박찬혁", "박신지", "김기연",
                "강승호", "이유찬", "임종성", "박지훈",
                "전다민", "조수행", "류승민"
        );
        matchService.updateLiveBoxState(externalId, state);

        mockMvc.perform(get("/api/matches/{id}/live-box", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.live").value(true))
                .andExpect(jsonPath("$.data.runnerOnFirst").value(first))
                .andExpect(jsonPath("$.data.runnerOnSecond").value(second))
                .andExpect(jsonPath("$.data.runnerOnThird").value(third))
                .andExpect(jsonPath("$.data.ballCount").value(balls))
                .andExpect(jsonPath("$.data.strikeCount").value(strikes))
                .andExpect(jsonPath("$.data.outCount").value(outs))
                .andExpect(jsonPath("$.data.currentBatter").value("박찬혁"))
                .andExpect(jsonPath("$.data.pitcher").value("박신지"))
                .andExpect(jsonPath("$.data.rightFielder").value("류승민"));

        MvcResult result = mockMvc.perform(get("/matches/{id}", matchId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("박신지")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("박찬혁")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("류승민")))
                .andReturn();

        // 각 베이스 div에 점유 여부대로 "occupied" 클래스가 붙었는지 확인한다.
        // (하단 폴링 스크립트 소스에도 "occupied"라는 문자열이 나오므로 전체 카운트가 아니라
        // 베이스별 class 속성을 정확히 짚어서 확인한다.)
        String html = result.getResponse().getContentAsString();
        assertThat(html.contains("base base-1b" + (first ? " occupied" : "") + "\"")).isTrue();
        assertThat(html.contains("base base-2b" + (second ? " occupied" : "") + "\"")).isTrue();
        assertThat(html.contains("base base-3b" + (third ? " occupied" : "") + "\"")).isTrue();
    }

    static Stream<Arguments> runnerScenarios() {
        return Stream.of(
                Arguments.of("무주자", false, false, false, 0, 0, 0),
                Arguments.of("만루", true, true, true, 3, 2, 2),
                Arguments.of("3루만_주자", false, false, true, 1, 1, 1),
                Arguments.of("1_3루_주자", true, false, true, 2, 1, 1)
        );
    }
}
