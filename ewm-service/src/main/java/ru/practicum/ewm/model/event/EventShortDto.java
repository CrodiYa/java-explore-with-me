package ru.practicum.ewm.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.category.CategoryDto;
import ru.practicum.ewm.model.user.UserDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {

    private Long id;
    private String title;
    private String annotation;
    private String eventDate;
    private CategoryDto category;
    private UserDto initiator;
    private Boolean paid;
    private Integer confirmedRequests;
    private Long views;
}
