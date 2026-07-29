package ScoreMate.ScoreMate.crawler;

import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * KBO 공식 홈페이지(koreabaseball.com)에서 경기 일정/결과를 크롤링한다.
 *
 * 주의사항 (비상업적 개인 프로젝트 기준):
 * - robots.txt를 준수하고, 과도한 요청으로 서버에 부하를 주지 않는다.
 * - 수집한 데이터의 저작권은 KBO(한국야구위원회)에 있으며 상업적 이용은 하지 않는다.
 * - 실제 셀렉터(태그/클래스명)는 대상 페이지의 실제 HTML 구조를 확인한 뒤 채워야 한다.
 *   (Spring Initializr로 프로젝트만 생성된 상태라, 아래는 뼈대만 잡아둔 상태)
 */
@Slf4j
@Component
public class KboCrawler {

    private static final String SCHEDULE_URL = "https://www.koreabaseball.com/Schedule/Schedule.aspx";
    private static final int TIMEOUT_MS = 10_000;

    /**
     * 특정 날짜의 KBO 경기 일정/결과를 크롤링한다.
     * TODO: 실제 페이지 구조 확인 후 CSS 셀렉터 채우기
     */
    public List<CrawledMatchDto> crawlByDate(LocalDate date) {
        List<CrawledMatchDto> results = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(SCHEDULE_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (ScoreMate personal project crawler; non-commercial)")
                    .get();

            // TODO: doc.select("실제 셀렉터")로 경기 목록 파싱
            // 예시 (실제 구조와 다를 수 있음):
            // Elements rows = doc.select(".tbl-box tbody tr");
            // for (Element row : rows) {
            //     String home = row.select(".team.home").text();
            //     String away = row.select(".team.away").text();
            //     ...
            //     results.add(new CrawledMatchDto(...));
            // }

            log.info("KBO 크롤링 완료 - date: {}, 수집 건수: {}", date, results.size());

        } catch (IOException e) {
            log.error("KBO 크롤링 실패 - date: {}", date, e);
        }

        return results;
    }
}
