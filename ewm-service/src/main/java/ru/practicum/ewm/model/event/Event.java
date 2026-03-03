package ru.practicum.ewm.model.event;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.user.User;

import java.time.Instant;

@Entity
@Table(name = "event", schema = "public")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "annotation", nullable = false)
    private String annotation;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lon", nullable = false)
    private Double lon;

    @Column(name = "eventDate", nullable = false)
    private Instant eventDate;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "initiator_id")
    private User initiator;

    @Column(name = "paid", nullable = false)
    private Boolean paid;

    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventState state;

    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit;

    // пре-модерация, true владелец события вручную подтверждает заявки
    // false - автоматически
    @Column(name = "request_moderation", nullable = false)
    private Boolean requestModeration;

    @Column(name = "published_on")
    private Instant publishedOn;

    @Column(name = "created_on", nullable = false)
    @CreationTimestamp
    private Instant createdOn;
}
