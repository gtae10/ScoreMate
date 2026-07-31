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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KBO 영문 사이트의 타자/투수 리더보드(Batting Leaders / Pitching Leaders)를 크롤링한다.
 * StandingCrawler와 마찬가지로 자바스크립트 없이 서버에서 표를 바로 렌더링해주는 페이지라
 * GET + Jsoup 셀렉터로 충분하다.
 *
 * 타자는 1페이지(기본 스탯) + 2페이지(볼넷/삼진/출루율/장타율/OPS 등 세부 스탯)를
 * pcode 기준으로 합쳐서 하나의 DTO로 만든다. Batting Leaders는 1/2페이지가
 * 같은 상위 20명을 다른 컬럼으로 보여주는 구조라 안전하게 병합 가능하다.
 *
 * 투수는 2페이지가 1페이지와 다른 정렬 기준(탈삼진 오름차순)이라 선수 구성 자체가
 * 달라서 병합하지 않는다 — 투수 세부 스탯(탈삼진 포함)은 항상 null.
 */
@Slf4j
@Component
public class PlayerCrawler {

    private static final String BATTING_LEADERS_URL = "https://eng.koreabaseball.com/stats/BattingLeaders.aspx";
    private static final String BATTING_LEADERS_PAGE2_URL = "https://eng.koreabaseball.com/stats/BattingLeaders02.aspx";
    private static final String PITCHING_LEADERS_URL = "https://eng.koreabaseball.com/stats/PitchingLeaders.aspx";
    private static final int TIMEOUT_MS = 10_000;
    private static final Pattern PCODE_PATTERN = Pattern.compile("pcode=(\\d+)");

    public List<CrawledPlayerRecordDto> crawlBattingLeaders() {
        List<CrawledPlayerRecordDto> results = new ArrayList<>();

        try {
            Document doc = fetch(BATTING_LEADERS_URL);
            Elements rows = doc.select("table:has(td[title=PLAYER]) tbody tr");
            Map<String, BattingDetail> detailsByPcode = fetchBattingDetails();

            for (Element row : rows) {
                String externalId = extractPcode(row);
                String playerName = text(row, "td[title=PLAYER] a");
                String teamCode = text(row, "td[title=TEAM]");
                String koreanName = KboTeamNameMapper.toKoreanName(teamCode);

                if (externalId == null || koreanName == null) {
                    log.warn("타자 리더보드 파싱 실패 - pcode: {}, teamCode: {}", externalId, teamCode);
                    continue;
                }

                BattingDetail detail = detailsByPcode.get(externalId);
                if (detail == null) {
                    log.warn("타자 2페이지에서 세부 스탯을 못 찾음 - pcode: {}, name: {}", externalId, playerName);
                    detail = BattingDetail.EMPTY;
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
                        detail.walks, detail.intentionalWalks, detail.hitByPitch,
                        detail.groundIntoDoublePlay, detail.errors, detail.stolenBasePercentage,
                        detail.onBasePercentage, detail.sluggingPercentage, detail.ops,
                        detail.runnersInScoringPositionAvg, detail.pinchHitAvg, detail.multiHits,
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
                        null, null, null, null, null, null, null, null, null, null, null, null,
                        parseDouble(text(row, "td[title=ERA]")),
                        parseInt(text(row, "td[title=W]")),
                        parseInt(text(row, "td[title=L]")),
                        parseInt(text(row, "td[title=SV]")),
                        null // 탈삼진 컬럼 없음 (2페이지는 다른 선수 집합이라 병합 안 함)
                ));
            }

            log.info("KBO 투수 리더보드 크롤링 완료 - 수집 건수: {}", results.size());

        } catch (IOException e) {
            log.error("KBO 투수 리더보드 크롤링 실패", e);
        }

        return results;
    }

    /**
     * 타자 리더보드 2페이지(세부 스탯)를 pcode -> 세부스탯 맵으로 가져온다.
     * 실패해도 예외를 던지지 않고 빈 맵을 반환 — 기본 스탯 수집은 이어가도록.
     */
    private Map<String, BattingDetail> fetchBattingDetails() {
        Map<String, BattingDetail> result = new HashMap<>();

        try {
            Document doc = fetch(BATTING_LEADERS_PAGE2_URL);
            Elements rows = doc.select("table:has(td[title=PLAYER]) tbody tr");

            for (Element row : rows) {
                String pcode = extractPcode(row);
                if (pcode == null) {
                    continue;
                }

                String sbpctText = text(row, "td[title=SBPCT]").replace("%", "");

                result.put(pcode, new BattingDetail(
                        parseInt(text(row, "td[title=BB]")),
                        parseInt(text(row, "td[title=IBB]")),
                        parseInt(text(row, "td[title=HBP]")),
                        parseInt(text(row, "td[title=GIDP]")),
                        parseInt(text(row, "td[title=E]")),
                        parsePercent(sbpctText),
                        parseDouble(text(row, "td[title=OBP]")),
                        parseDouble(text(row, "td[title=SLG]")),
                        parseDouble(text(row, "td[title=OPS]")),
                        parseDouble(text(row, "td[title=RISP]")),
                        parseDouble(text(row, "td[title=PH]")),
                        parseInt(text(row, "td[title=MH]"))
                ));
            }

            log.info("타자 리더보드 2페이지(세부 스탯) 크롤링 완료 - 수집 건수: {}", result.size());

        } catch (IOException e) {
            log.error("타자 리더보드 2페이지 크롤링 실패 - 세부 스탯 없이 진행", e);
        }

        return result;
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

    /** "66.7" 같은 퍼센트 숫자 문자열을 0.667 형태(비율)로 변환 */
    private Double parsePercent(String text) {
        Double value = parseDouble(text);
        return value != null ? value / 100.0 : null;
    }

    /** 타자 리더보드 2페이지에서 뽑은 세부 스탯 묶음 (내부 전달용) */
    private record BattingDetail(
            Integer walks,
            Integer intentionalWalks,
            Integer hitByPitch,
            Integer groundIntoDoublePlay,
            Integer errors,
            Double stolenBasePercentage,
            Double onBasePercentage,
            Double sluggingPercentage,
            Double ops,
            Double runnersInScoringPositionAvg,
            Double pinchHitAvg,
            Integer multiHits
    ) {
        static final BattingDetail EMPTY = new BattingDetail(
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
