package ru.practicum.ewm.model.compilation;

import lombok.*;
import ru.practicum.ewm.model.event.EventShortDto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompilationDto {

    private Long id;

    private Boolean pinned;

    private String title;

    // список кратких DTO событий, не id
    private List<EventShortDto> events;
}
