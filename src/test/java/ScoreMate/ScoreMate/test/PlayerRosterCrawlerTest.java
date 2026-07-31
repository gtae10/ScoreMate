package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.PlayerRosterCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledPlayerDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.player.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * PlayerRosterCrawler(팀별 전체 로스터, ASP.NET UpdatePanel 방식)가 실제로 동작하는지 확인.
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로
 * 돌면 안 되니, 확인이 필요할 때만 이 메서드를 직접 선택해서(IntelliJ 메서드 옆
 * 초록 버튼) 수동으로 실행할 것 — 클래스 전체 실행이나 ./gradlew test로는 켜지 않는다.
 *
 * 이 크롤러는 델타 응답 파싱이 처음이라 assertThat으로 단정짓지 않고, 우선 결과를
 * 콘솔에 다 찍어서 눈으로 확인하는 용도로 작성했다. (정상 동작 확인되면 이후에
 * MatchSyncIntegrationTest처럼 assert를 추가할 것)
 */
@SpringBootTest
class PlayerRosterCrawlerTest {

    @Autowired
    private PlayerRosterCrawler playerRosterCrawler;

    @Autowired
    private PlayerService playerService;

    @Test
    void 팀_로스터를_크롤링한다() {
        List<CrawledPlayerDto> results = playerRosterCrawler.crawlAllTeamRosters();

        System.out.println("전체 수집 건수: " + results.size());
        results.forEach(p -> System.out.println(
                p.teamNameKorean() + " | " + p.playerName()
                        + " | " + p.position()
                        + " | 등번호 " + p.backNumber()
                        + " | pcode " + p.externalId()
        ));

        if (!results.isEmpty()) {
            playerService.syncCrawledRoster(League.KBO, results);
            System.out.println("DB 반영 완료");
        }
    }
}
