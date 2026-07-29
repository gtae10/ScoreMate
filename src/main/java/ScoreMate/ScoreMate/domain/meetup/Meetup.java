package ScoreMate.ScoreMate.domain.meetup;

import ScoreMate.ScoreMate.common.BaseEntity;
import ScoreMate.ScoreMate.domain.match.Match;
import ScoreMate.ScoreMate.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meetups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meetup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false)
    private int maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetupStatus status;

    @Builder
    public Meetup(Match match, User host, String title, String location, int maxParticipants) {
        this.match = match;
        this.host = host;
        this.title = title;
        this.location = location;
        this.maxParticipants = maxParticipants;
        this.status = MeetupStatus.OPEN;
    }

    public void close() {
        this.status = MeetupStatus.CLOSED;
    }

    public enum MeetupStatus {
        OPEN, CLOSED, CANCELLED
    }
}
