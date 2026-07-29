package ScoreMate.ScoreMate.domain.meetup;

import ScoreMate.ScoreMate.common.BaseEntity;
import ScoreMate.ScoreMate.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "meetup_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meetup_id", "user_id"}) // 중복 신청 방지
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetupParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetup_id", nullable = false)
    private Meetup meetup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Builder
    public MeetupParticipant(Meetup meetup, User user) {
        this.meetup = meetup;
        this.user = user;
        this.status = Status.PENDING;
    }

    public void accept() {
        this.status = Status.ACCEPTED;
    }

    public void reject() {
        this.status = Status.REJECTED;
    }

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }
}
