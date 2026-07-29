package ScoreMate.ScoreMate.crawler;

import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.match.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 매일 정해진 시각에 KBO 경기 데이터를 크롤링해 DB에 반영한다.
 * cron 표현식은 서버 부하를 고려해 하루 1~2회 정도로 시작 (필요시 조정).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KboScheduler {

    private final KboCrawler kboCrawler;
    private final MatchService matchService;

    // 매일 새벽 3시 (전날 경기 결과 확정 이후) 실행
    @Scheduled(cron = "0 0 3 * * *")
    public void syncYesterdayMatches() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        run(yesterday);
    }

    // 매일 오전 9시, 오늘 경기 일정 갱신
    @Scheduled(cron = "0 0 9 * * *")
    public void syncTodayMatches() {
        run(LocalDate.now());
    }

    private void run(LocalDate date) {
        log.info("KBO 동기화 시작 - date: {}", date);
        List<CrawledMatchDto> crawled = kboCrawler.crawlByDate(date);
        matchService.syncCrawledMatches(League.KBO, crawled);
    }
}
