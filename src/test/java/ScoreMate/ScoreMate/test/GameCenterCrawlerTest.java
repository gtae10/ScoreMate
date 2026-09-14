package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.GameCenterCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GameCenterCrawler가 오늘 경기 목록(구장/선발투수)을 제대로 파싱하는지 확인한다.
 * 스프링 컨텍스트 없이 크롤러만 직접 생성해서 빠르게 돈다.
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로
 * 돌면 안 되니, 확인이 필요할 때만 메서드를 직접 선택해서(IntelliJ 메서드 옆 초록 버튼)
 * 수동으로 실행할 것.
 *
 * 오늘 KBO 경기가 아예 없는 날(휴식일 등)엔 결과가 0건일 수 있다 — 그 자체는
 * 정상이니, 결과가 있을 때 필드가 제대로 채워지는지 위주로 확인하면 된다.
 */
@Tag("manual")
class GameCenterCrawlerTest {

    private final GameCenterCrawler gameCenterCrawler = new GameCenterCrawler();

    @Test
    void 오늘_경기_목록을_크롤링한다() {
        List<CrawledMatchDto> results = gameCenterCrawler.crawlToday();

        System.out.println("수집된 경기 수: " + results.size());
        results.forEach(m -> System.out.println(
                m.awayTeam() + " vs " + m.homeTeam()
                        + " | 구장: " + m.stadium()
                        + " | 원정 선발: " + m.awayStartingPitcher()
                        + " | 홈 선발: " + m.homeStartingPitcher()
        ));

        results.forEach(m -> {
            assertThat(m.homeTeam()).isNotBlank();
            assertThat(m.awayTeam()).isNotBlank();
            assertThat(m.externalId()).isNotBlank();
        });
    }
}
