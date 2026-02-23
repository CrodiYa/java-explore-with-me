package ru.practicum.stats.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hit")
@ToString @Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class EndpointHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hit_id")
    private Long id;

    private String app;

    private String uri;

    private String ip;

    @Column(name = "create_date")
    private LocalDateTime createDate;
}
