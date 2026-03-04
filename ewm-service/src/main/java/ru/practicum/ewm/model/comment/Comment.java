package ru.practicum.ewm.model.comment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.practicum.ewm.model.user.User;

import java.time.Instant;

@Entity
@Table(name = "comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "created")
    @CreationTimestamp
    private Instant created;

    @Column(name = "updated")
    @UpdateTimestamp
    private Instant updated;
}
