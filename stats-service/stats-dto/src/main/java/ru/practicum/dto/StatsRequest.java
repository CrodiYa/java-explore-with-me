package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static ru.practicum.dto.Formatter.PATTERN;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatsRequest {

    @NotNull
    @JsonFormat(pattern = PATTERN)
    private String start;

    @NotNull
    @JsonFormat(pattern = PATTERN)
    private String end;

    private List<String> uris;

    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean unique = false;
}
