package ScoreMate.ScoreMate.domain.team;

import ScoreMate.ScoreMate.crawler.dto.CrawledStandingDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.dto.response.StandingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandingService {

    private final StandingRepository standingRepository;
    private final TeamRepository teamRepository;

    public List<StandingResponse> getStandings(League league, int season) {
        return standingRepository.findByTeam_LeagueAndSeasonOrderByRankAsc(league, season).stream()
                .map(StandingResponse::from)
                .toList();
    }

    /**
     * 크롤러가 수집한 순위표를 반영한다. team은 미리 저장돼 있어야 한다.
     */
    @Transactional
    public void upsertStanding(Team team, int season, int rank, int wins, int losses, int draws,
                                double winRate, Double gamesBehind) {
        Standing standing = standingRepository.findByTeamAndSeason(team, season)
                .orElseGet(() -> Standing.builder()
                        .team(team)
                        .season(season)
                        .rank(rank)
                        .wins(wins)
                        .losses(losses)
                        .draws(draws)
                        .winRate(winRate)
                        .gamesBehind(gamesBehind)
                        .build());

        standing.update(rank, wins, losses, draws, winRate, gamesBehind);
        standingRepository.save(standing);
    }

    /**
     * StandingCrawler가 크롤링한 결과를 통째로 반영한다.
     * 팀이 아직 DB에 없으면 여기서 새로 만든다 (externalId = 영문 팀 코드 기준).
     */
    @Transactional
    public void syncCrawledStandings(League league, int season, List<CrawledStandingDto> crawledStandings) {
        for (CrawledStandingDto dto : crawledStandings) {
            Team team = getOrCreateTeam(league, dto.externalTeamCode(), dto.teamNameKorean());
            upsertStanding(team, season, dto.rank(), dto.wins(), dto.losses(), dto.draws(),
                    dto.winRate(), dto.gamesBehind());
        }
        log.info("순위표 동기화 완료 - league: {}, season: {}, 건수: {}", league, season, crawledStandings.size());
    }

    private Team getOrCreateTeam(League league, String externalId, String name) {
        return teamRepository.findByExternalId(externalId)
                .orElseGet(() -> teamRepository.save(
                        Team.builder()
                                .league(league)
                                .name(name)
                                .shortName(externalId)
                                .externalId(externalId)
                                .build()
                ));
    }
}
