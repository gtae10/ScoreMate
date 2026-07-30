package ScoreMate.ScoreMate.test;

import ScoreMate.ScoreMate.crawler.KboCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KBO 실제 페이지에 접속/파싱이 되는지 확인하는 테스트.
 *
 * 아래 테스트들은 실제 KBO 사이트로 네트워크 요청을 보낸다. 평소 빌드/CI에서
 * 자동으로 돌면 안 되니, 확인이 필요할 때만 IntelliJ에서 해당 메서드를 직접
 * 선택해서(메서드 옆 초록 버튼) 수동으로 실행할 것 — 클래스 전체 실행이나
 * ./gradlew test로는 켜지 않는다.
 *
 * 스프링 컨텍스트를 아예 안 띄우는 순수 단위 테스트라 실행이 빠르다.
 * (KboCrawler가 @Component라 스프링 빈으로 등록돼 있긴 하지만,
 *  이 테스트는 `new KboCrawler()`로 직접 생성해서 컨텍스트 로딩 없이 돈다)
 */
class KboCrawlerTest {

    private final KboCrawler kboCrawler = new KboCrawler();

    /**
     * 1단계: 접속 자체가 되는지만 확인.
     * 셀렉터 작업 전에 가장 먼저 통과시켜야 하는 테스트.
     */
    @Test
    void KBO_일정_페이지에_접속할_수_있다() throws Exception {
        Document doc = Jsoup.connect("https://www.koreabaseball.com/Schedule/Schedule.aspx")
                .timeout(10_000)
                .userAgent("Mozilla/5.0 (ScoreMate personal project crawler; non-commercial)")
                .get();

        // 페이지 타이틀 정도만 찍어봐서 정말로 KBO 페이지가 맞는지 눈으로 확인
        System.out.println("페이지 타이틀: " + doc.title());
        System.out.println("HTML 길이: " + doc.html().length());

        assertThat(doc.title()).isNotBlank();
    }

    /**
     * 2단계: 실제 KboCrawler.crawlByDate()가 몇 건이나 파싱해오는지 확인.
     */
    @Test
    void 특정_날짜의_KBO_경기를_크롤링한다() {
        // 실제 KBO 경기가 있었던 과거 날짜로 지정해서 테스트하는 걸 추천
        // (오늘 날짜는 휴식일/오프시즌일 수 있어서 결과가 0건이면 셀렉터 문제인지
        //  단순히 그날 경기가 없는 건지 헷갈릴 수 있음)
        LocalDate testDate = LocalDate.of(2026, 7, 1);

        List<CrawledMatchDto> results = kboCrawler.crawlByDate(testDate);

        System.out.println("수집된 경기 수: " + results.size());
        results.forEach(match -> System.out.println(
                match.homeTeam() + " vs " + match.awayTeam()
                        + " | " + match.matchDate()
                        + " | finished=" + match.finished()
        ));

        assertThat(results).isNotEmpty();
        results.forEach(match -> {
            assertThat(match.homeTeam()).isNotBlank();
            assertThat(match.awayTeam()).isNotBlank();
            assertThat(match.matchDate()).isNotNull();
        });
    }
}
