package ScoreMate.ScoreMate.domain.match;

import ScoreMate.ScoreMate.crawler.LiveBoxCrawler;
import ScoreMate.ScoreMate.crawler.dto.CrawledMatchDto;
import ScoreMate.ScoreMate.dto.response.LiveBoxResponse;
import ScoreMate.ScoreMate.dto.response.MatchResponse;
import ScoreMate.ScoreMate.exception.CustomException;
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

    public MatchResponse getMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .map(MatchResponse::from)
                .orElseThrow(() -> new CustomException("경기를 찾을 수 없습니다."));
    }

    public LiveBoxResponse getLiveBox(Long matchId) {
        return matchRepository.findById(matchId)
                .map(LiveBoxResponse::from)
                .orElseThrow(() -> new CustomException("경기를 찾을 수 없습니다."));
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
            match.updateStartingPitchers(dto.awayStartingPitcher(), dto.homeStartingPitcher());
            match.updateLineScore(dto.awayInnings(), dto.homeInnings(), dto.awayHits(), dto.homeHits(), dto.awayErrors(), dto.homeErrors());

            if (dto.finished() && dto.homeScore() != null && dto.awayScore() != null) {
                match.updateResult(dto.homeScore(), dto.awayScore());
            } else if (dto.live() && dto.homeScore() != null && dto.awayScore() != null) {
                match.markLive(dto.homeScore(), dto.awayScore(), dto.liveStatusText());
            } else if (dto.postponed()) {
                match.markPostponed(dto.cancelReason());
            } else if (dto.cancelled()) {
                match.markCancelled(dto.cancelReason());
            }
        }
        log.info("경기 데이터 동기화 완료 - league: {}, 건수: {}", league, crawledMatches.size());
    }

    /**
     * 진행 중인 경기의 "지금 던지는 투수" + 타자 + 최근 문자중계를 한 번에 갱신한다
     * (LiveTextCrawler 전용). 투수/타자 둘 다 null이고 문자중계도 없으면
     * (네트워크 오류 등 파싱 완전 실패로 보고) 기존 값을 건드리지 않는다.
     */
    @Transactional
    public void updateLiveGameState(String externalId, String awayCurrentPitcher, String homeCurrentPitcher,
                                     String currentBatter, List<String> recentPlays) {
        boolean gotNothing = awayCurrentPitcher == null && homeCurrentPitcher == null
                && currentBatter == null && (recentPlays == null || recentPlays.isEmpty());
        if (gotNothing) {
            return;
        }
        matchRepository.findByExternalId(externalId).ifPresent(match -> {
            match.updateCurrentPitchers(awayCurrentPitcher, homeCurrentPitcher);
            match.updateLiveState(currentBatter, recentPlays);
        });
    }

    /**
     * 진행 중인 경기의 라이브박스(이닝/주자/B-S-O/수비 포지션/타석 타자)를 갱신한다 (LiveBoxCrawler 전용).
     * state가 null이면(경기 시작 전이라 ".economy" 블록이 없거나 크롤링 실패) 기존 값을 건드리지 않는다.
     */
    @Transactional
    public void updateLiveBoxState(String externalId, LiveBoxCrawler.LiveBoxState state) {
        if (state == null) {
            return;
        }
        matchRepository.findByExternalId(externalId).ifPresent(match -> match.updateLiveBox(
                state.inningText(), state.currentBatter(),
                state.runnerOnFirst(), state.runnerOnSecond(), state.runnerOnThird(),
                state.ballCount(), state.strikeCount(), state.outCount(),
                state.pitcher(), state.catcher(), state.firstBase(), state.secondBase(),
                state.thirdBase(), state.shortstop(), state.leftFielder(),
                state.centerFielder(), state.rightFielder()
        ));
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
