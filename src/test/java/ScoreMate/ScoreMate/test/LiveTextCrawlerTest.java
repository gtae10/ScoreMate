package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.LiveTextCrawler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LiveTextCrawler가 문자중계 페이지에서 현재 투수를 제대로 뽑는지 확인한다.
 * 스프링 컨텍스트 없이 크롤러만 직접 생성해서 빠르게 돈다.
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로
 * 돌면 안 되니, 확인이 필요할 때만 메서드를 직접 선택해서(IntelliJ 메서드 옆 초록 버튼)
 * 수동으로 실행할 것.
 *
 * gameId는 buildGameId()로 직접 조합해서 테스트한다 (오늘 실제로 있었던 경기 기준으로
 * 팀명을 바꿔서 실행하면 됨). 경기가 이미 끝났거나 시작 전이면 둘 다 null이 나올 수
 * 있는데, 그건 정상이다 — "지금 던지는 중"일 때만 값이 채워지는 필드라서다.
 */
class LiveTextCrawlerTest {

    private final LiveTextCrawler liveTextCrawler = new LiveTextCrawler();

    @Test
    void 경기ID를_정상적으로_조합한다() {
        String gameId = liveTextCrawler.buildGameId(LocalDate.of(2026, 8, 4), "한화", "삼성");
        assertThat(gameId).isEqualTo("20260804HHSS0");
    }

    @Test
    @Tag("manual")
    void 현재_투수를_크롤링한다() {
        // 실제로 있었던 경기로 바꿔서 실행할 것 (오늘 날짜 + 실제 대진)
        String gameId = liveTextCrawler.buildGameId(LocalDate.of(2026, 8, 4), "한화", "삼성");

        LiveTextCrawler.LiveGameState state = liveTextCrawler.crawlLiveState(gameId);

        System.out.println("원정(한화) 현재 투수: " + state.awayPitcher());
        System.out.println("홈(삼성) 현재 투수: " + state.homePitcher());
        System.out.println("현재 타자: " + state.currentBatter());
        System.out.println("최근 문자중계 (" + state.recentPlays().size() + "줄):");
        state.recentPlays().forEach(line -> System.out.println("  - " + line));
    }
}
