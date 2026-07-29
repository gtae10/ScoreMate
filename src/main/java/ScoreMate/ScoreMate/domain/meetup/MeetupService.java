package ScoreMate.ScoreMate.domain.meetup;

import ScoreMate.ScoreMate.domain.match.Match;
import ScoreMate.ScoreMate.domain.match.MatchRepository;
import ScoreMate.ScoreMate.domain.user.User;
import ScoreMate.ScoreMate.domain.user.UserRepository;
import ScoreMate.ScoreMate.dto.request.MeetupCreateRequest;
import ScoreMate.ScoreMate.dto.response.MeetupResponse;
import ScoreMate.ScoreMate.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetupService {

    private final MeetupRepository meetupRepository;
    private final MeetupParticipantRepository participantRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    @Transactional
    public MeetupResponse create(Long hostId, MeetupCreateRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다."));
        Match match = matchRepository.findById(request.matchId())
                .orElseThrow(() -> new CustomException("경기를 찾을 수 없습니다."));

        Meetup meetup = Meetup.builder()
                .match(match)
                .host(host)
                .title(request.title())
                .location(request.location())
                .maxParticipants(request.maxParticipants())
                .build();

        return MeetupResponse.from(meetupRepository.save(meetup));
    }

    @Transactional
    public void join(Long meetupId, Long userId) {
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new CustomException("모임을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다."));

        if (meetup.getStatus() != Meetup.MeetupStatus.OPEN) {
            throw new CustomException("모집이 마감된 모임입니다.");
        }
        if (participantRepository.existsByMeetupAndUser(meetup, user)) {
            throw new CustomException("이미 신청한 모임입니다.");
        }

        long accepted = participantRepository.countByMeetupAndStatus(meetup, MeetupParticipant.Status.ACCEPTED);
        if (accepted >= meetup.getMaxParticipants()) {
            throw new CustomException("인원이 모두 찼습니다.");
        }

        participantRepository.save(MeetupParticipant.builder()
                .meetup(meetup)
                .user(user)
                .build());
    }

    public List<MeetupResponse> getOpenMeetups() {
        return meetupRepository.findByStatus(Meetup.MeetupStatus.OPEN).stream()
                .map(MeetupResponse::from)
                .toList();
    }
}
