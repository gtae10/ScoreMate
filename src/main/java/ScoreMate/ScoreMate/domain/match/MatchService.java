package ScoreMate.ScoreMate.domain.match;

import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import ScoreMate.ScoreMate.dto.response.MatchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;

    public List<MatchResponse> getMatchesByLeagueAndDate(League league, LocalDate date) {
        return matchRepository.findByLeagueAndDate(league, date).stream()
                .map(MatchResponse::from)
                .toList();
    }

    public List<MatchResponse> getAllMatches() {
        return matchRepository.findAll().stream()
                .map(MatchResponse::from)
                .toList();
    }

    /**
     * 크롤러가 수집한 경기 목록을 DB에 반영한다.
     * externalId 기준으로 신규 저장 / 기존 결과 업데이트를 나눈다.
     */
    @Transactional
    public void syncCrawledMatches(League league, List<CrawledMatchDto> crawledMatches) {
        for (CrawledMatchDto dto : crawledMatches) {
            Match match = matchRepository.findByExternalId(dto.externalId())
                    .orElseGet(() -> matchRepository.save(
                            Match.builder()
                                    .league(league)
                                    .homeTeam(dto.homeTeam())
                                    .awayTeam(dto.awayTeam())
                                    .matchDate(dto.matchDate())
                                    .externalId(dto.externalId())
                                    .build()
                    ));

            if (dto.finished() && dto.homeScore() != null && dto.awayScore() != null) {
                match.updateResult(dto.homeScore(), dto.awayScore());
            } else if (dto.live() && dto.homeScore() != null && dto.awayScore() != null) {
                match.markLive(dto.homeScore(), dto.awayScore());
            }
        }
        log.info("경기 데이터 동기화 완료 - league: {}, 건수: {}", league, crawledMatches.size());
    }
}
