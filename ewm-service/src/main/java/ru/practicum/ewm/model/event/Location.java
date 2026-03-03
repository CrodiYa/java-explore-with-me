package ru.practicum.ewm.model.event;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;
import ru.practicum.ewm.validation.OnCreate;
import ru.practicum.ewm.validation.OnUpdate;

public record Location(
        @NotNull(groups = OnCreate.class)
        @Range(groups = {OnCreate.class, OnUpdate.class}, min = -90, max = 90,
                message = "Latitude must be between -90 and 90")
        Double lat,

        @NotNull(groups = OnCreate.class)
        @Range(groups = {OnCreate.class, OnUpdate.class}, min = -180, max = 180,
                message = "Longitude must be between -180 and 180")
        Double lon
) {
}

