package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.KboCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.match.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 날짜 탭으로 아무 날짜나 눌러도 실제 경기 데이터가 보이도록, 시즌 시작 월부터
 * 현재 월까지 KBO 경기를 통째로 크롤링해서 DB에 채워넣는 백필(backfill) 작업.
 *
 * 지금까지는 MatchSyncIntegrationTest에서 특정 날짜(7월 1일) 하나만 채워봤는데,
 * 이건 그걸 여러 달치로 확장한 버전이다 — 기능 자체(크롤링→저장)는 이미 검증된
 * 것과 같은 흐름을 재사용한다.
 *
 * 아래 테스트는 실제 KBO 사이트로 여러 번 네트워크 요청을 보내고(달마다 1번씩),
 * 몇 분 정도 걸릴 수 있다. 평소 빌드/CI에서 자동으로 돌면 안 되니, DB를
 * 채우고 싶을 때만 이 메서드를 직접 선택해서(IntelliJ 메서드 옆 초록 버튼)
 * 수동으로 실행할 것.
 *
 * START_MONTH를 시즌 시작 월에 맞게, END_MONTH를 현재 월에 맞게 바꿔서 실행하면 된다.
 */
@SpringBootTest
class MatchBackfillTest {

    private static final int SEASON_YEAR = 2026;
    private static final int START_MONTH = 4; // KBO 정규시즌 개막월 (필요하면 조정)
    private static final int END_MONTH = 7;   // 현재까지 진행된 월 (필요하면 조정)

    @Autowired
    private KboCrawler kboCrawler;

    @Autowired
    private MatchService matchService;

    @Test
    void 시즌_전체_경기를_월별로_크롤링해서_DB에_채운다() {
        int totalSynced = 0;

        for (int month = START_MONTH; month <= END_MONTH; month++) {
            List<CrawledMatchDto> crawled = kboCrawler.crawlByMonth(SEASON_YEAR, month);
            matchService.syncCrawledMatches(League.KBO, crawled);
            totalSynced += crawled.size();

            System.out.println(SEASON_YEAR + "-" + month + " : " + crawled.size() + "건 동기화 완료");
        }

        System.out.println("전체 백필 완료 - 총 " + totalSynced + "건");
    }
}
