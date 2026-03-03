package ru.practicum.ewm.model.compilation;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.model.event.Event;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "compilation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false)
    private Boolean pinned;

    @ManyToMany
    @JoinTable(
            name = "compilation_event",
            // колонка в связующей таблице, ссылающаяся на ЭТУ сущность (compilation)
            joinColumns = @JoinColumn(name = "compilation_id"),
            // колонка в связующей таблице, ссылающаяся на ДРУГУЮ сущность (event)
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    // Set а не List — потому что uniqueItems: true в спеке,
    // и составной ключ в схеме запрещает дубли
    @Builder.Default
    private Set<Event> events = new HashSet<>();
}
