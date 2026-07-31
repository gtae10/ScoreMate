package ScoreMate.ScoreMate.domain.player;

import ScoreMate.ScoreMate.crawler.dto.CrawledPlayerDto;
import ScoreMate.ScoreMate.crawler.dto.CrawledPlayerRecordDto;
import ScoreMate.ScoreMate.domain.match.League;
import ScoreMate.ScoreMate.domain.team.Team;
import ScoreMate.ScoreMate.domain.team.TeamRepository;
import ScoreMate.ScoreMate.dto.response.PlayerRecordResponse;
import ScoreMate.ScoreMate.dto.response.PlayerResponse;
import ScoreMate.ScoreMate.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerRecordRepository playerRecordRepository;
    private final TeamRepository teamRepository;

    public List<PlayerResponse> search(String name) {
        return playerRepository.findByNameContaining(name).stream()
                .map(PlayerResponse::from)
                .toList();
    }

    public List<PlayerResponse> getByTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException("팀을 찾을 수 없습니다."));
        return playerRepository.findByTeam(team).stream()
                .map(PlayerResponse::from)
                .toList();
    }

    public PlayerResponse getPlayer(Long playerId) {
        Player player = getPlayerOrThrow(playerId);
        return PlayerResponse.from(player);
    }

    public List<PlayerRecordResponse> getPlayerRecords(Long playerId) {
        Player player = getPlayerOrThrow(playerId);
        return playerRecordRepository.findByPlayer(player).stream()
                .map(PlayerRecordResponse::from)
                .toList();
    }

    /**
     * PlayerCrawler가 수집한 타자/투수 리더보드 결과를 반영한다.
     * 선수/팀이 아직 DB에 없으면 여기서 새로 만든다.
     */
    @Transactional
    public void syncCrawledPlayerRecords(League league, int season, List<CrawledPlayerRecordDto> crawledRecords) {
        for (CrawledPlayerRecordDto dto : crawledRecords) {
            Team team = getOrCreateTeam(league, dto.externalTeamCode(), dto.teamNameKorean());
            Player player = getOrCreatePlayer(team, dto);
            upsertPlayerRecord(player, season, dto);
        }
        log.info("선수 기록 동기화 완료 - league: {}, season: {}, 건수: {}", league, season, crawledRecords.size());
    }

    /**
     * PlayerRosterCrawler가 수집한 팀별 전체 로스터를 반영한다 (기록 없이 프로필만).
     * 선수/팀이 아직 DB에 없으면 여기서 새로 만들고, 있으면 등번호 등 프로필만 갱신한다.
     */
    @Transactional
    public void syncCrawledRoster(League league, List<CrawledPlayerDto> crawledPlayers) {
        for (CrawledPlayerDto dto : crawledPlayers) {
            Team team = getOrCreateTeam(league, dto.externalTeamCode(), dto.teamNameKorean());
            playerRepository.findByExternalId(dto.externalId())
                    .ifPresentOrElse(
                            existing -> existing.updateProfile(dto.backNumber()),
                            () -> playerRepository.save(
                                    Player.builder()
                                            .team(team)
                                            .name(dto.playerName())
                                            .position(dto.position())
                                            .backNumber(dto.backNumber())
                                            .externalId(dto.externalId())
                                            .build())
                    );
        }
        log.info("로스터 동기화 완료 - league: {}, 건수: {}", league, crawledPlayers.size());
    }

    private Player getOrCreatePlayer(Team team, CrawledPlayerRecordDto dto) {
        return playerRepository.findByExternalId(dto.externalId())
                .orElseGet(() -> playerRepository.save(
                        Player.builder()
                                .team(team)
                                .name(dto.playerName())
                                .position(dto.position())
                                .externalId(dto.externalId())
                                .build()
                ));
    }

    private void upsertPlayerRecord(Player player, int season, CrawledPlayerRecordDto dto) {
        PlayerRecord record = playerRecordRepository.findByPlayerAndSeason(player, season)
                .orElseGet(() -> PlayerRecord.builder()
                        .player(player)
                        .season(season)
                        .build());

        record.update(
                dto.gamesPlayed(),
                dto.battingAverage(), dto.hits(), dto.homeRuns(), dto.rbi(),
                dto.walks(), dto.intentionalWalks(), dto.hitByPitch(),
                dto.groundIntoDoublePlay(), dto.errors(), dto.stolenBasePercentage(),
                dto.onBasePercentage(), dto.sluggingPercentage(), dto.ops(),
                dto.runnersInScoringPositionAvg(), dto.pinchHitAvg(), dto.multiHits(),
                dto.era(), dto.wins(), dto.losses(), dto.saves(), dto.strikeouts()
        );
        playerRecordRepository.save(record);
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

    private Player getPlayerOrThrow(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new CustomException("선수를 찾을 수 없습니다."));
    }
}
