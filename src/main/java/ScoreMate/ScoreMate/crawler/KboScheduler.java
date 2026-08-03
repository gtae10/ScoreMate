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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
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

    private final PlayerRosterCrawler playerRosterCrawler;

    // KBO 정규시즌 개막월 근사치. 이 달 이전 데이터는 어차피 없으니 매일 훑을 필요 없음.
    private static final int SEASON_START_MONTH = 4;

    /**
     * 앱 시작 시 자동으로 시즌 전체(4월~이번달)를 동기화할지 여부.
     * 기본값 true — 앱만 켜면 시즌 전체 일정이 바로 보이길 원해서 기본으로 켜뒀다.
     *
     * 다만 @SpringBootTest로 도는 모든 테스트도 컨텍스트를 띄울 때 이게 같이 실행돼서
     * 실제 네트워크 호출 + 많은 SQL 로그가 같이 찍힌다. 테스트할 때 이게 거슬리면
     * application.yml에 app.startup-sync.enabled=false를 추가해서 끌 수 있다.
     */
    @Value("${app.startup-sync.enabled:true}")
    private boolean startupSyncEnabled;

    /**
     * 앱이 완전히 뜬 직후 실행되는 시작 루틴.
     *
     * - 중복 정리 + 이번 달 전체 채우기는 가벼운 작업(요청 1번)이라 토글과 무관하게
     *   항상 실행한다. 이게 없으면 어제/내일처럼 오늘이 아닌 날짜는 새벽 스케줄이
     *   돌기 전까지 계속 비어있었다.
     * - 시즌 전체(4월~이번달 전부) 백필은 더 무거운 작업이라 startupSyncEnabled가
     *   true일 때만 실행한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        matchService.deduplicateMatches();

        LocalDate today = LocalDate.now();
        List<CrawledMatchDto> thisMonth = kboCrawler.crawlByMonth(today.getYear(), today.getMonthValue());
        matchService.syncCrawledMatches(League.KBO, thisMonth);
        log.info("앱 시작 - 이번 달 일정 채우기 완료 - {}건", thisMonth.size());

        if (!startupSyncEnabled) {
            return;
        }
        log.info("앱 시작 - 시즌 전체 동기화 시작");
        syncFullSeason();

        // 승/패 투수는 시즌 시작일부터 오늘까지 전부 하루씩 순서대로 훑어야 해서
        // (한 번에 임의 날짜로 점프가 안 됨) 요청이 꽤 많이 나간다 — 시즌 전체
        // 백필(startupSyncEnabled)이 켜져 있을 때만 같이 돈다.
        LocalDate seasonStart = LocalDate.of(today.getYear(), SEASON_START_MONTH, 1);
        int daysSinceSeasonStart = (int) ChronoUnit.DAYS.between(seasonStart, today);
        List<CrawledMatchDto> allPitchers = scoreBoardCrawler.crawlRecentDays(daysSinceSeasonStart);
        matchService.syncCrawledMatches(League.KBO, allPitchers);
        log.info("앱 시작 - 시즌 전체 승/패 투수 채우기 완료 - {}건 ({}일치)", allPitchers.size(), daysSinceSeasonStart);
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
     *
     * KBO 서버가 우리한테 변경을 push해주는 게 아니라 우리가 계속 다시 물어보는(polling)
     * 방식이라 완전한 실시간은 아니다. 너무 잦은 요청은 서버에 부담을 주는 행위라
     * (지금까지 크롤러 전반에서 지켜온 원칙 — robots.txt 준수, 과도한 요청 자제),
     * 사람이 스코어보드를 새로고침하는 정도의 빈도(10초)로 절충했다.
     */
    @Scheduled(fixedRate = 10 * 1000)
    public void syncTodayLiveScores() {
        log.info("KBO 실시간 스코어 동기화 시작");
        List<CrawledMatchDto> crawled = scoreBoardCrawler.crawlToday();
        matchService.syncCrawledMatches(League.KBO, crawled);
    }

    /**
     * 매일 새벽 3시 10분, 최근 3일치를 스코어보드 방식(하루씩 이전날짜 이동)으로 다시 훑어서
     * 승/패 투수를 채운다. 일정 API(KboCrawler)에는 이 정보가 없어서 별도로 돈다.
     */
    @Scheduled(cron = "0 10 3 * * *")
    public void syncRecentPitchers() {
        log.info("KBO 최근 경기 승/패 투수 동기화 시작");
        List<CrawledMatchDto> crawled = scoreBoardCrawler.crawlRecentDays(3);
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

    /**
     * 매일 새벽 3시 50분, 10개 구단 전체 로스터(감독/코치 제외 선수만)를 갱신한다.
     * 리더보드(상위 20명)엔 안 잡히는 선수들까지 프로필을 채워주는 역할 —
     * 팀당 GET+POST 2번씩, 총 20번 요청이 나가는 무거운 축이라 하루 1번으로 제한.
     */
    @Scheduled(cron = "0 50 3 * * *")
    public void syncRosters() {
        log.info("KBO 팀 로스터 동기화 시작");
        var crawled = playerRosterCrawler.crawlAllTeamRosters();
        playerService.syncCrawledRoster(League.KBO, crawled);
    }

    private void runMatchSync(LocalDate date) {
        log.info("KBO 경기 동기화 시작 - date: {}", date);
        List<CrawledMatchDto> crawled = kboCrawler.crawlByDate(date);
        matchService.syncCrawledMatches(League.KBO, crawled);
    }
}
