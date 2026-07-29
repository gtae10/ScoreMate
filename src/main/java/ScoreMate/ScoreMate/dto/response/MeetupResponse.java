package ScoreMate.ScoreMate.dto.response;

import ScoreMate.ScoreMate.domain.meetup.Meetup;

public record MeetupResponse(
        Long id,
        Long matchId,
        String hostUsername,
        String title,
        String location,
        int maxParticipants,
        String status
) {
    public static MeetupResponse from(Meetup meetup) {
        return new MeetupResponse(
                meetup.getId(),
                meetup.getMatch().getId(),
                meetup.getHost().getUsername(),
                meetup.getTitle(),
                meetup.getLocation(),
                meetup.getMaxParticipants(),
                meetup.getStatus().name()
        );
    }
}
