package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.KboScheduler;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.team.StandingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KboScheduler의 @Scheduled 메서드들이 실제로 정상 동작하는지 확인하는 테스트.
 * (cron 트리거를 기다리지 않고, 스케줄러가 호출하는 메서드를 직접 실행해서 확인한다)
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로
 * 돌면 안 되니, 확인이 필요할 때만 이 메서드를 직접 선택해서(IntelliJ 메서드 옆
 * 초록 버튼) 수동으로 실행할 것 — 클래스 전체 실행이나 ./gradlew test로는 켜지 않는다.
 *
 * syncYesterdayMatches / syncTodayMatches는 MatchSyncIntegrationTest에서 이미
 * 같은 흐름(KboCrawler -> MatchService)을 검증했으므로 여기서는 새로 추가된
 * syncStandings / syncPlayerRecords 두 개만 확인한다.
 */
@SpringBootTest
class KboSchedulerTest {

    @Autowired
    private KboScheduler kboScheduler;

    @Autowired
    private StandingService standingService;

    @Test
    void 스케줄러의_순위표_동기화가_정상_동작한다() {
        kboScheduler.syncStandings();

        int season = Year.now().getValue();
        var standings = standingService.getStandings(League.KBO, season);

        System.out.println("스케줄러 실행 후 저장된 순위 항목 수: " + standings.size());
        assertThat(standings).hasSize(10);
    }

    @Test
    void 스케줄러의_선수_기록_동기화가_정상_동작한다() {
        // 예외 없이 끝까지 실행되는지만 확인 (타자/투수 각 20명 upsert)
        kboScheduler.syncPlayerRecords();
        System.out.println("선수 기록 동기화 완료 (예외 없이 종료됨)");
    }
}
