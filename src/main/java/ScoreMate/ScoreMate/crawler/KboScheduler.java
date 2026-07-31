package ScoreMate.ScoreMate.crawler;

import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import ScoreMate.ScoreMate.crawler.dto.CrawledPlayerRecordDto;
import ScoreMate.ScoreMate.crawler.dto.CrawledStandingDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.match.MatchService;
import ScoreMate.ScoreMate.domain.player.PlayerService;
import ScoreMate.ScoreMate.domain.team.StandingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/**
 * 매일 정해진 시각에 KBO 데이터(경기 일정/결과, 순위표, 선수 기록)를 크롤링해 DB에 반영한다.
 * cron 표현식은 서버 부하를 고려해 하루 1~2회 정도로 시작 (필요시 조정).
 * 순서는 일정(경기 결과) → 순위표 → 선수 기록 순으로, 서로 겹치지 않게 시간을 조금씩 띄워뒀다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KboScheduler {

    private final KboCrawler kboCrawler;
    private final MatchService matchService;

    private final ScoreBoardCrawler scoreBoardCrawler;

    private final StandingCrawler standingCrawler;
    private final StandingService standingService;

    private final PlayerCrawler playerCrawler;
    private final PlayerService playerService;

    // KBO 정규시즌 개막월 근사치. 이 달 이전 데이터는 어차피 없으니 매일 훑을 필요 없음.
    private static final int SEASON_START_MONTH = 4;

    /**
     * 앱이 완전히 뜬 직후 한 번 자동으로 시즌 전체를 동기화한다.
     * 서버를 재시작할 때마다 크롤링이 도는 트레이드오프가 있지만,
     * 자정 스케줄만 믿으면 방금 켠 서버엔 최신 데이터가 없을 수 있어서 같이 걸어둔다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("앱 시작 - 초기 데이터 동기화 시작");
        syncFullSeason();
    }

    /**
     * 매일 자정, 시즌 시작월부터 이번달까지 KBO 경기를 전부 다시 훑어서 DB에 반영한다.
     * 날짜 탭에서 어떤 날짜를 눌러도 데이터가 비어있지 않도록, 특정 하루만 갱신하는
     * syncYesterdayMatches/syncTodayMatches와 별개로 훨씬 넓은 범위를 커버한다.
     * (예전엔 MatchBackfillTest를 수동으로 돌려야 했는데, 이제 이 스케줄이 그 역할을 자동으로 함)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void syncFullSeason() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int totalSynced = 0;

        for (int month = SEASON_START_MONTH; month <= now.getMonthValue(); month++) {
            List<CrawledMatchDto> crawled = kboCrawler.crawlByMonth(year, month);
            matchService.syncCrawledMatches(League.KBO, crawled);
            totalSynced += crawled.size();
        }

        log.info("KBO 시즌 전체 동기화 완료 - {}년 {}~{}월, 총 {}건", year, SEASON_START_MONTH, now.getMonthValue(), totalSynced);
    }

    /**
     * 오늘 경기를 짧은 주기로 다시 훑어서 실시간에 가깝게 유지한다.
     * 일정 API(KboCrawler) 대신 스코어보드 페이지(ScoreBoardCrawler)를 쓴다 —
     * lblGameState 텍스트로 "진행 중(LIVE)"까지 구분해서 반영할 수 있어서다.
     */
    @Scheduled(fixedRate = 3 * 60 * 1000)
    public void syncTodayLiveScores() {
        log.info("KBO 실시간 스코어 동기화 시작");
        List<CrawledMatchDto> crawled = scoreBoardCrawler.crawlToday();
        matchService.syncCrawledMatches(League.KBO, crawled);
    }

    // 매일 새벽 3시 (전날 경기 결과 확정 이후) 실행
    @Scheduled(cron = "0 0 3 * * *")
    public void syncYesterdayMatches() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        runMatchSync(yesterday);
    }

    // 매일 오전 9시, 오늘 경기 일정 갱신
    @Scheduled(cron = "0 0 9 * * *")
    public void syncTodayMatches() {
        runMatchSync(LocalDate.now());
    }

    // 매일 새벽 3시 30분 (경기 결과 반영 이후) 순위표 갱신
    @Scheduled(cron = "0 30 3 * * *")
    public void syncStandings() {
        log.info("KBO 순위표 동기화 시작");
        List<CrawledStandingDto> crawled = standingCrawler.crawlCurrentStandings();
        int season = Year.now().getValue();
        standingService.syncCrawledStandings(League.KBO, season, crawled);
    }

    // 매일 새벽 3시 40분, 타자/투수 리더보드(선수 기록) 갱신
    @Scheduled(cron = "0 40 3 * * *")
    public void syncPlayerRecords() {
        log.info("KBO 선수 기록 동기화 시작");
        int season = Year.now().getValue();

        List<CrawledPlayerRecordDto> batters = playerCrawler.crawlBattingLeaders();
        playerService.syncCrawledPlayerRecords(League.KBO, season, batters);

        List<CrawledPlayerRecordDto> pitchers = playerCrawler.crawlPitchingLeaders();
        playerService.syncCrawledPlayerRecords(League.KBO, season, pitchers);
    }

    private void runMatchSync(LocalDate date) {
        log.info("KBO 경기 동기화 시작 - date: {}", date);
        List<CrawledMatchDto> crawled = kboCrawler.crawlByDate(date);
        matchService.syncCrawledMatches(League.KBO, crawled);
    }
}
