package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.ScoreBoardCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScoreBoardCrawler.crawlRecentDays()가 "이전날짜" 버튼 포스트백을 흉내내서
 * 과거 며칠치 데이터(특히 승/패 투수)를 제대로 가져오는지 확인한다.
 * 스프링 컨텍스트 없이 크롤러만 직접 생성해서 빠르게 돈다.
 *
 * 아래 테스트는 실제 KBO 사이트로 여러 번 네트워크 요청을 보낸다. 평소 빌드/CI에서
 * 자동으로 돌면 안 되니, 확인이 필요할 때만 메서드를 직접 선택해서(IntelliJ 메서드 옆
 * 초록 버튼) 수동으로 실행할 것.
 */
class ScoreBoardHistoryCrawlerTest {

    private final ScoreBoardCrawler scoreBoardCrawler = new ScoreBoardCrawler();

    @Test
    void 최근_3일치_스코어보드를_크롤링한다() {
        List<CrawledMatchDto> results = scoreBoardCrawler.crawlRecentDays(3);

        System.out.println("수집된 경기 수: " + results.size());
        results.forEach(m -> System.out.println(
                m.matchDate().toLocalDate() + " | " + m.homeTeam() + " vs " + m.awayTeam()
                        + " | finished=" + m.finished()
                        + " | " + m.homeScore() + ":" + m.awayScore()
                        + " | 승:" + m.winPitcher() + " 패:" + m.losePitcher()
                        + " | 구장:" + m.stadium()
        ));

        // 3일 중 하루라도 경기가 있었으면 결과가 있어야 함 (전부 휴식일일 가능성은 낮음)
        if (!results.isEmpty()) {
            long withPitcher = results.stream().filter(m -> m.finished() && m.winPitcher() != null).count();
            System.out.println("승리투수까지 채워진 종료 경기 수: " + withPitcher);
        }
    }
}
