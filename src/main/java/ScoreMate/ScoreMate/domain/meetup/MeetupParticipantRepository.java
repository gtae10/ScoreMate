package ScoreMate.ScoreMate.domain.meetup;

import ScoreMate.ScoreMate.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetupParticipantRepository extends JpaRepository<MeetupParticipant, Long> {

    boolean existsByMeetupAndUser(Meetup meetup, User user);

    List<MeetupParticipant> findByMeetup(Meetup meetup);

    long countByMeetupAndStatus(Meetup meetup, MeetupParticipant.Status status);
}
