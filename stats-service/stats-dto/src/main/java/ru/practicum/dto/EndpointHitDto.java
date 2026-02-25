package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.practicum.dto.Formatter.PATTERN;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {

    @NotBlank
    @Size(max = 255)
    private String app;

    @NotBlank
    @Size(max = 512)
    private String uri;

    @NotBlank
    @Size(max = 45)
    private String ip;

    @NotNull
    @JsonFormat(pattern = PATTERN)
    private String timestamp;
}
