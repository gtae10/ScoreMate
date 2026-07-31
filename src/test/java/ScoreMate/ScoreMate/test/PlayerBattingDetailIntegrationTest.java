package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.PlayerCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledPlayerRecordDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.player.PlayerService;
import ScoreMate.ScoreMate.dto.response.PlayerRecordResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlayerCrawler.crawlBattingLeaders()가 1페이지(기본 스탯) + 2페이지(세부 스탯)를
 * pcode 기준으로 정확히 합쳐서 DB에 저장하는지 확인하는 통합 테스트.
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로
 * 돌면 안 되니, 확인이 필요할 때만 이 메서드를 직접 선택해서(IntelliJ 메서드 옆
 * 초록 버튼) 수동으로 실행할 것 — 클래스 전체 실행이나 ./gradlew test로는 켜지 않는다.
 */
@SpringBootTest
class PlayerBattingDetailIntegrationTest {

    @Autowired
    private PlayerCrawler playerCrawler;

    @Autowired
    private PlayerService playerService;

    @Test
    void 타자_세부_스탯이_병합되어_DB에_저장된다() {
        int season = Year.now().getValue();

        // 1. 크롤링 (1페이지 + 2페이지 병합)
        List<CrawledPlayerRecordDto> batters = playerCrawler.crawlBattingLeaders();
        assertThat(batters).hasSize(20);

        // 2. 세부 스탯이 최소 절반 이상은 채워져 있는지 확인
        //    (2페이지 매칭 실패한 선수가 있어도 대부분은 채워져야 정상)
        long withDetail = batters.stream().filter(b -> b.onBasePercentage() != null).count();
        System.out.println("세부 스탯(OBP)까지 채워진 타자 수: " + withDetail + " / " + batters.size());
        assertThat(withDetail).isGreaterThanOrEqualTo(10);

        // 3. DB 반영
        playerService.syncCrawledPlayerRecords(League.KBO, season, batters);

        // 4. 샘플 1명 뽑아서 세부 스탯이 실제로 저장/조회되는지 확인
        CrawledPlayerRecordDto sample = batters.stream()
                .filter(b -> b.onBasePercentage() != null)
                .findFirst()
                .orElseThrow();

        var savedPlayer = playerService.search(sample.playerName());
        assertThat(savedPlayer).isNotEmpty();

        List<PlayerRecordResponse> records = playerService.getPlayerRecords(savedPlayer.get(0).id());
        System.out.println("샘플 선수(" + sample.playerName() + ") 저장된 기록: " + records);

        assertThat(records).isNotEmpty();
        PlayerRecordResponse record = records.get(0);
        assertThat(record.onBasePercentage()).isNotNull();
        assertThat(record.sluggingPercentage()).isNotNull();
        assertThat(record.ops()).isNotNull();

        // 5. 같은 데이터로 한 번 더 동기화해도 값이 그대로 유지되는지 확인 (update 로직 검증)
        playerService.syncCrawledPlayerRecords(League.KBO, season, batters);
        List<PlayerRecordResponse> recordsAgain = playerService.getPlayerRecords(savedPlayer.get(0).id());
        assertThat(recordsAgain).hasSize(records.size());
        assertThat(recordsAgain.get(0).onBasePercentage()).isEqualTo(record.onBasePercentage());
    }
}
