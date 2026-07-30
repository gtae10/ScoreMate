package ScoreMate.ScoreMate.crawler;

import ScoreMate.ScoreMate.crawler.dto.CrawledStandingDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * KBO 영문 사이트의 팀 순위표를 크롤링한다.
 *
 * 한글 사이트(koreabaseball.com)의 일정 페이지와 달리, 영문 사이트
 * (eng.koreabaseball.com)의 순위표 페이지는 자바스크립트 없이 서버에서
 * 순위표를 직접 렌더링해서 내려준다. 그래서 별도 API 호출 없이 페이지를
 * 그대로 GET해서 Jsoup 셀렉터로 파싱하면 된다.
 *
 * 페이지 안에 table[summary="team standings"]가 두 개 있는데 (순위표 / 팀 스탯표),
 * 순위표 쪽에만 td[title="GAMES"]가 있어서 그걸로 구분한다.
 *
 * 팀명이 영문 표기(SAMSUNG, KT, DOOSAN ...)로 내려오기 때문에, KboTeamNameMapper로
 * 우리 서비스 전반에서 쓰는 한글 팀명으로 매핑해서 반환한다.
 */
@Slf4j
@Component
public class StandingCrawler {

    private static final String STANDINGS_URL = "https://eng.koreabaseball.com/Standings/TeamStandings.aspx";
    private static final int TIMEOUT_MS = 10_000;

    /**
     * 현재(오늘 기준) KBO 순위표를 크롤링한다.
     */
    public List<CrawledStandingDto> crawlCurrentStandings() {
        List<CrawledStandingDto> results = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(STANDINGS_URL)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .get();

            Elements rows = doc.select("table[summary=\"team standings\"] tbody tr");

            for (Element row : rows) {
                Element gamesCell = row.selectFirst("td[title=GAMES]");
                if (gamesCell == null) {
                    // 팀 스탯표(AVG/ERA...) 쪽 행이라 건너뜀
                    continue;
                }

                String rankText = text(row, "td[title=RK]");
                String teamCode = text(row, "td[title=TEAM]");
                String gamesText = gamesCell.text();
                String winsText = text(row, "td[title=W]");
                String lossesText = text(row, "td[title=L]");
                String drawsText = text(row, "td[title=D]");
                String pctText = text(row, "td[title=PCT]");
                String gbText = text(row, "td[title=GB]");
                String streakText = text(row, "td[title=STREAK]");

                String koreanName = KboTeamNameMapper.toKoreanName(teamCode);
                if (koreanName == null) {
                    log.warn("알 수 없는 팀 코드 - {} (매핑 테이블에 추가 필요)", teamCode);
                    continue;
                }

                results.add(new CrawledStandingDto(
                        teamCode,
                        koreanName,
                        Integer.parseInt(rankText),
                        Integer.parseInt(gamesText),
                        Integer.parseInt(winsText),
                        Integer.parseInt(lossesText),
                        Integer.parseInt(drawsText),
                        Double.parseDouble(pctText),
                        "0.0".equals(gbText) ? 0.0 : Double.parseDouble(gbText),
                        streakText
                ));
            }

            log.info("KBO 순위표 크롤링 완료 - 수집 건수: {}", results.size());

        } catch (IOException e) {
            log.error("KBO 순위표 크롤링 실패", e);
        }

        return results;
    }

    private String text(Element row, String cssQuery) {
        Element cell = row.selectFirst(cssQuery);
        return cell != null ? cell.text() : "";
    }
}
