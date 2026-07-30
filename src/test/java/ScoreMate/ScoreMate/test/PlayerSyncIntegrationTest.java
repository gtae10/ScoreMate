package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.PlayerCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledPlayerRecordDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.player.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlayerCrawler가 수집한 타자/투수 리더보드가 실제로 DB(Player/PlayerRecord/Team)에
 * 저장되는지 확인하는 통합 테스트.
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로
 * 돌면 안 되니, 확인이 필요할 때만 이 메서드를 직접 선택해서(IntelliJ 메서드 옆
 * 초록 버튼) 수동으로 실행할 것 — 클래스 전체 실행이나 ./gradlew test로는 켜지 않는다.
 */
@SpringBootTest
class PlayerSyncIntegrationTest {

    @Autowired
    private PlayerCrawler playerCrawler;

    @Autowired
    private PlayerService playerService;

    @Test
    void 크롤링한_타자_투수_기록이_DB에_저장된다() {
        int season = Year.now().getValue();

        // 1. 크롤링 (타자/투수 각각 1페이지, 상위 20명)
        List<CrawledPlayerRecordDto> batters = playerCrawler.crawlBattingLeaders();
        List<CrawledPlayerRecordDto> pitchers = playerCrawler.crawlPitchingLeaders();
        assertThat(batters).hasSize(20);
        assertThat(pitchers).hasSize(20);

        // 2. DB 반영 (선수/팀이 없으면 새로 생성까지 같이 됨)
        playerService.syncCrawledPlayerRecords(League.KBO, season, batters);
        playerService.syncCrawledPlayerRecords(League.KBO, season, pitchers);

        // 3. 타자 1명, 투수 1명 뽑아서 실제로 잘 저장됐는지 확인
        CrawledPlayerRecordDto sampleBatter = batters.get(0);
        var savedBatter = playerService.search(sampleBatter.playerName());
        System.out.println("샘플 타자: " + savedBatter);
        assertThat(savedBatter).isNotEmpty();

        Long batterId = savedBatter.get(0).id();
        var batterRecords = playerService.getPlayerRecords(batterId);
        System.out.println("타자 기록: " + batterRecords);
        assertThat(batterRecords).isNotEmpty();
        assertThat(batterRecords.get(0).battingAverage()).isNotNull();

        CrawledPlayerRecordDto samplePitcher = pitchers.get(0);
        var savedPitcher = playerService.search(samplePitcher.playerName());
        System.out.println("샘플 투수: " + savedPitcher);
        assertThat(savedPitcher).isNotEmpty();

        Long pitcherId = savedPitcher.get(0).id();
        var pitcherRecords = playerService.getPlayerRecords(pitcherId);
        System.out.println("투수 기록: " + pitcherRecords);
        assertThat(pitcherRecords).isNotEmpty();
        assertThat(pitcherRecords.get(0).era()).isNotNull();

        // 4. 같은 데이터로 한 번 더 동기화해도 기록 건수가 안 늘어나는지 확인 (upsert 검증)
        playerService.syncCrawledPlayerRecords(League.KBO, season, batters);
        var batterRecordsAgain = playerService.getPlayerRecords(batterId);
        assertThat(batterRecordsAgain).hasSameSizeAs(batterRecords);
    }
}
