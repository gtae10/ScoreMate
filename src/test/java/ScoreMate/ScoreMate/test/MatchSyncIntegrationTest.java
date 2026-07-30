package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.KboCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.match.Match;
import ScoreMate.ScoreMate.domain.match.MatchRepository;
import ScoreMate.ScoreMate.domain.match.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 크롤러가 수집한 데이터가 실제로 DB(Match 엔티티)에 저장되는지 확인하는 통합 테스트.
 * H2 인메모리 DB로 돈다 (src/test/resources 쪽 설정 덕분에 실제 MySQL 불필요).
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로
 * 돌면 안 되니, 확인이 필요할 때만 이 메서드를 직접 선택해서(IntelliJ 메서드 옆
 * 초록 버튼) 수동으로 실행할 것 — 클래스 전체 실행이나 ./gradlew test로는 켜지 않는다.
 */
@SpringBootTest
class MatchSyncIntegrationTest {

    @Autowired
    private KboCrawler kboCrawler;

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void 크롤링한_경기가_DB에_저장된다() {
        LocalDate testDate = LocalDate.of(2026, 7, 1);

        // 1. 크롤링
        List<CrawledMatchDto> crawled = kboCrawler.crawlByDate(testDate);
        assertThat(crawled).isNotEmpty();

        // 2. DB 반영
        matchService.syncCrawledMatches(League.KBO, crawled);

        // 3. 실제로 저장됐는지 확인
        List<Match> saved = matchRepository.findByLeagueAndDate(League.KBO, testDate);

        System.out.println("DB에 저장된 경기 수: " + saved.size());
        saved.forEach(match -> System.out.println(
                match.getHomeTeam() + " vs " + match.getAwayTeam()
                        + " | " + match.getStatus()
                        + " | " + match.getHomeScore() + ":" + match.getAwayScore()
        ));

        assertThat(saved).hasSize(crawled.size());
        saved.forEach(match -> {
            assertThat(match.getExternalId()).isNotBlank();
            assertThat(match.getStatus()).isEqualTo(Match.MatchStatus.FINISHED);
        });

        // 4. 같은 데이터로 한 번 더 동기화해도 중복 저장되지 않는지 확인 (upsert 검증)
        matchService.syncCrawledMatches(League.KBO, crawled);
        List<Match> savedAgain = matchRepository.findByLeagueAndDate(League.KBO, testDate);
        assertThat(savedAgain).hasSize(crawled.size());
    }
}
