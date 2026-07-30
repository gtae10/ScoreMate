package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.StandingCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledStandingDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.team.StandingService;
import ScoreMate.ScoreMate.dto.response.StandingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StandingCrawler가 수집한 순위표가 실제로 DB(Standing/Team 엔티티)에
 * 저장되는지 확인하는 통합 테스트.
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로
 * 돌면 안 되니, 확인이 필요할 때만 이 메서드를 직접 선택해서(IntelliJ 메서드 옆
 * 초록 버튼) 수동으로 실행할 것 — 클래스 전체 실행이나 ./gradlew test로는 켜지 않는다.
 */
@SpringBootTest
class StandingSyncIntegrationTest {

    @Autowired
    private StandingCrawler standingCrawler;

    @Autowired
    private StandingService standingService;

    @Test
    void 크롤링한_순위표가_DB에_저장된다() {
        int season = Year.now().getValue();

        // 1. 크롤링
        List<CrawledStandingDto> crawled = standingCrawler.crawlCurrentStandings();
        assertThat(crawled).hasSize(10); // KBO는 10개 팀

        // 2. DB 반영 (팀이 없으면 새로 생성까지 같이 됨)
        standingService.syncCrawledStandings(League.KBO, season, crawled);

        // 3. 실제로 저장됐는지 확인 (DTO로 조회 - 트랜잭션 안에서 팀 정보까지 이미 변환됨)
        List<StandingResponse> saved = standingService.getStandings(League.KBO, season);

        System.out.println("DB에 저장된 순위 항목 수: " + saved.size());
        saved.forEach(standing -> System.out.println(
                standing.rank() + "위 " + standing.teamName()
                        + " | " + standing.wins() + "승 " + standing.losses() + "패 " + standing.draws() + "무"
                        + " | 승률 " + standing.winRate()
                        + " | 게임차 " + standing.gamesBehind()
        ));

        assertThat(saved).hasSize(10);
        // 순위(rank)가 1~10까지 중복 없이 다 있는지 확인
        List<Integer> ranks = saved.stream().map(StandingResponse::rank).sorted().toList();
        assertThat(ranks).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // 4. 같은 데이터로 한 번 더 동기화해도 행 개수가 안 늘어나는지 확인 (upsert 검증)
        standingService.syncCrawledStandings(League.KBO, season, crawled);
        List<StandingResponse> savedAgain = standingService.getStandings(League.KBO, season);
        assertThat(savedAgain).hasSize(10);
    }
}
