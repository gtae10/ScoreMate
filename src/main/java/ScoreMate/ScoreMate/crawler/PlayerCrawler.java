package ScoreMate.ScoreMate.crawler;

import ScoreMate.ScoreMate.crawler.dto.CrawledPlayerRecordDto;
import ScoreMate.ScoreMate.domain.player.Player;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KBO 영문 사이트의 타자/투수 리더보드(Batting Leaders / Pitching Leaders)를 크롤링한다.
 * StandingCrawler와 마찬가지로 자바스크립트 없이 서버에서 표를 바로 렌더링해주는 페이지라
 * GET + Jsoup 셀렉터로 충분하다.
 *
 * 현재는 각 페이지의 1페이지(상위 20명)만 가져온다. 페이지네이션(Leaders02.aspx)은
 * 필요해지면 이어서 추가.
 *
 * 투수 리더보드에는 탈삼진(SO) 컬럼이 없어서 PlayerRecord.strikeouts는 항상 null로 채워진다.
 */
@Slf4j
@Component
public class PlayerCrawler {

    private static final String BATTING_LEADERS_URL = "https://eng.koreabaseball.com/stats/BattingLeaders.aspx";
    private static final String PITCHING_LEADERS_URL = "https://eng.koreabaseball.com/stats/PitchingLeaders.aspx";
    private static final int TIMEOUT_MS = 10_000;
    private static final Pattern PCODE_PATTERN = Pattern.compile("pcode=(\\d+)");

    public List<CrawledPlayerRecordDto> crawlBattingLeaders() {
        List<CrawledPlayerRecordDto> results = new ArrayList<>();

        try {
            Document doc = fetch(BATTING_LEADERS_URL);
            Elements rows = doc.select("table:has(td[title=PLAYER]) tbody tr");

            for (Element row : rows) {
                String externalId = extractPcode(row);
                String playerName = text(row, "td[title=PLAYER] a");
                String teamCode = text(row, "td[title=TEAM]");
                String koreanName = KboTeamNameMapper.toKoreanName(teamCode);

                if (externalId == null || koreanName == null) {
                    log.warn("타자 리더보드 파싱 실패 - pcode: {}, teamCode: {}", externalId, teamCode);
                    continue;
                }

                results.add(new CrawledPlayerRecordDto(
                        externalId,
                        playerName,
                        teamCode,
                        koreanName,
                        Player.Position.BATTER,
                        parseInt(text(row, "td[title=G]")),
                        parseDouble(text(row, "td[title=AVG]")),
                        parseInt(text(row, "td[title=H]")),
                        parseInt(text(row, "td[title=HR]")),
                        parseInt(text(row, "td[title=RBI]")),
                        null, null, null, null, null
                ));
            }

            log.info("KBO 타자 리더보드 크롤링 완료 - 수집 건수: {}", results.size());

        } catch (IOException e) {
            log.error("KBO 타자 리더보드 크롤링 실패", e);
        }

        return results;
    }

    public List<CrawledPlayerRecordDto> crawlPitchingLeaders() {
        List<CrawledPlayerRecordDto> results = new ArrayList<>();

        try {
            Document doc = fetch(PITCHING_LEADERS_URL);
            Elements rows = doc.select("table:has(td[title=PLAYER]) tbody tr");

            for (Element row : rows) {
                String externalId = extractPcode(row);
                String playerName = text(row, "td[title=PLAYER] a");
                String teamCode = text(row, "td[title=TEAM]");
                String koreanName = KboTeamNameMapper.toKoreanName(teamCode);

                if (externalId == null || koreanName == null) {
                    log.warn("투수 리더보드 파싱 실패 - pcode: {}, teamCode: {}", externalId, teamCode);
                    continue;
                }

                results.add(new CrawledPlayerRecordDto(
                        externalId,
                        playerName,
                        teamCode,
                        koreanName,
                        Player.Position.PITCHER,
                        parseInt(text(row, "td[title=G]")),
                        null, null, null, null,
                        parseDouble(text(row, "td[title=ERA]")),
                        parseInt(text(row, "td[title=W]")),
                        parseInt(text(row, "td[title=L]")),
                        parseInt(text(row, "td[title=SV]")),
                        null // 탈삼진 컬럼 없음
                ));
            }

            log.info("KBO 투수 리더보드 크롤링 완료 - 수집 건수: {}", results.size());

        } catch (IOException e) {
            log.error("KBO 투수 리더보드 크롤링 실패", e);
        }

        return results;
    }

    private Document fetch(String url) throws IOException {
        return Jsoup.connect(url)
                .timeout(TIMEOUT_MS)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .get();
    }

    private String extractPcode(Element row) {
        Element link = row.selectFirst("td[title=PLAYER] a");
        if (link == null) {
            return null;
        }
        Matcher matcher = PCODE_PATTERN.matcher(link.attr("href"));
        return matcher.find() ? matcher.group(1) : null;
    }

    private String text(Element row, String cssQuery) {
        Element cell = row.selectFirst(cssQuery);
        return cell != null ? cell.text() : "";
    }

    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
