package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.ScoreBoardCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScoreBoardCrawler가 오늘 경기를 제대로 파싱하는지 확인하는 테스트.
 * 스프링 컨텍스트 없이 크롤러만 직접 생성해서 빠르게 돈다.
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서
 * 자동으로 돌면 안 되니, 확인이 필요할 때만 메서드를 직접 선택해서(IntelliJ
 * 메서드 옆 초록 버튼) 수동으로 실행할 것.
 *
 * 오늘 KBO 경기가 아예 없는 날(휴식일 등)엔 결과가 0건일 수 있다 — 그 자체는
 * 정상이니, 결과가 있을 때 필드가 제대로 채워지는지 위주로 확인하면 된다.
 */
@Tag("manual")
class ScoreBoardCrawlerTest {

    private final ScoreBoardCrawler scoreBoardCrawler = new ScoreBoardCrawler();

    @Test
    void 오늘_경기_스코어보드를_크롤링한다() {
        List<CrawledMatchDto> results = scoreBoardCrawler.crawlToday();

        System.out.println("수집된 경기 수: " + results.size());
        results.forEach(m -> System.out.println(
                m.homeTeam() + " vs " + m.awayTeam()
                        + " | finished=" + m.finished()
                        + " | live=" + m.live()
                        + " | " + m.homeScore() + ":" + m.awayScore()
                        + " | 이닝(원정)=" + m.awayInnings()
                        + " | 이닝(홈)=" + m.homeInnings()
                        + " | 안타 " + m.awayHits() + ":" + m.homeHits()
                        + " | 실책 " + m.awayErrors() + ":" + m.homeErrors()
                        + " | externalId=" + m.externalId()
        ));

        results.forEach(m -> {
            assertThat(m.homeTeam()).isNotBlank();
            assertThat(m.awayTeam()).isNotBlank();
            assertThat(m.externalId()).isNotBlank();
            // 종료/진행중이 동시에 참일 수는 없음
            assertThat(m.finished() && m.live()).isFalse();

            // 종료된 경기는 이닝별 점수판이 항상 채워져 있어야 정상.
            // LIVE 경기는 막 시작해서 완료된 이닝이 아직 없으면 빈 문자열이 정상이라 제외한다.
            if (m.finished()) {
                assertThat(m.awayInnings()).isNotBlank();
                assertThat(m.homeInnings()).isNotBlank();
            }
        });
    }
}
