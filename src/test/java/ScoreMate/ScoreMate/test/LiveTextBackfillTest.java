package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.LiveTextCrawler;
import ScoreMate.ScoreMate.domain.match.MatchService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 20초 실시간 크롤러가 생기기 전에 이미 끝나버린 경기는, 그 경기가 "진행중"이던
 * 순간을 놓쳐서 DB에 문자중계/현재타자 데이터가 아예 없다. KBO 사이트 자체는
 * 경기가 끝나도 문자중계 기록을 계속 보여주니, 이 테스트로 특정 경기 하나를
 * 수동으로 한 번 긁어서 지금이라도 채워넣을 수 있다 (기능 검증용).
 *
 * 아래 테스트는 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서 자동으로
 * 돌면 안 되니, 확인이 필요할 때만 메서드를 직접 선택해서(IntelliJ 메서드 옆 초록 버튼)
 * 수동으로 실행할 것.
 */
@SpringBootTest
@Tag("manual")
class LiveTextBackfillTest {

    @Autowired
    private LiveTextCrawler liveTextCrawler;

    @Autowired
    private MatchService matchService;

    @Test
    void 특정_경기의_문자중계를_수동으로_채운다() {
        // 확인하고 싶은 경기에 맞게 바꿔서 실행할 것
        String gameId = "20260804HHSS0";
        String externalId = "2026-08-04_삼성_한화"; // "날짜_홈팀_원정팀" 형식

        LiveTextCrawler.LiveGameState state = liveTextCrawler.crawlLiveState(gameId);

        System.out.println("원정 현재 투수: " + state.awayPitcher());
        System.out.println("홈 현재 투수: " + state.homePitcher());
        System.out.println("현재 타자: " + state.currentBatter());
        System.out.println("문자중계 " + state.recentPlays().size() + "줄");

        matchService.updateLiveGameState(externalId, state.awayPitcher(), state.homePitcher(),
                state.currentBatter(), state.recentPlays());

        System.out.println("DB 반영 완료 - /matches 상세 페이지에서 확인해볼 것");
    }
}
