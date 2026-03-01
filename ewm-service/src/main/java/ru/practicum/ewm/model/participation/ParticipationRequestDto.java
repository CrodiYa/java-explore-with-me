package ru.practicum.ewm.model.participation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto {
    private Long id;

    private Long event;

    private Long requester;

    private String created;

    private ParticipationStatus status;
}
