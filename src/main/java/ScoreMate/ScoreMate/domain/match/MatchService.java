package ScoreMate.ScoreMate.domain.match;

import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import ScoreMate.ScoreMate.dto.response.MatchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

            match.updateStadium(dto.stadium());
            match.updatePitchers(dto.winPitcher(), dto.losePitcher());

            if (dto.finished() && dto.homeScore() != null && dto.awayScore() != null) {
                match.updateResult(dto.homeScore(), dto.awayScore());
            } else if (dto.live() && dto.homeScore() != null && dto.awayScore() != null) {
                match.markLive(dto.homeScore(), dto.awayScore(), dto.liveStatusText());
            } else if (dto.postponed()) {
                match.markPostponed();
            } else if (dto.cancelled()) {
                match.markCancelled();
            }
        }
        log.info("경기 데이터 동기화 완료 - league: {}, 건수: {}", league, crawledMatches.size());
    }

    /**
     * externalId 형식이 바뀌면서(gameId 기반 → 날짜_홈팀_원정팀 기반) 예전에 이미
     * 중복 저장돼버린 경기들을 자동으로 정리한다. 같은 리그+날짜+홈팀+원정팀 조합이
     * 여러 행으로 있으면, 가장 진행 상태가 앞선(종료 > 진행중 > 취소 > 예정) 것 하나만
     * 남기고 나머지는 지운다 (동점이면 가장 최근에 갱신된 것을 남김).
     */
    @Transactional
    public void deduplicateMatches() {
        List<Match> all = matchRepository.findAll();

        Map<String, List<Match>> grouped = all.stream()
                .collect(Collectors.groupingBy(m ->
                        m.getLeague() + "_" + m.getMatchDate().toLocalDate() + "_" + m.getHomeTeam() + "_" + m.getAwayTeam()
                ));

        int removed = 0;
        for (List<Match> group : grouped.values()) {
            if (group.size() <= 1) {
                continue;
            }
            Match keep = group.stream()
                    .max(Comparator.<Match>comparingInt(m -> statusPriority(m.getStatus()))
                            .thenComparing(Match::getUpdatedAt))
                    .orElseThrow();

            List<Match> toRemove = group.stream()
                    .filter(m -> !m.getId().equals(keep.getId()))
                    .toList();
            matchRepository.deleteAll(toRemove);
            removed += toRemove.size();
        }

        if (removed > 0) {
            log.info("중복 경기 정리 완료 - 삭제된 행: {}", removed);
        }
    }

    private int statusPriority(Match.MatchStatus status) {
        return switch (status) {
            case FINISHED -> 4;
            case LIVE -> 3;
            case CANCELLED -> 2;
            case POSTPONED -> 1;
            case SCHEDULED -> 0;
        };
    }
}
