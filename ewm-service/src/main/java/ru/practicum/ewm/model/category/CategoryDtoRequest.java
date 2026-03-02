package ru.practicum.ewm.model.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDtoRequest {

    @NotBlank(message = "Name can`t be empty or null")
    @Size(min = 1, max = 50, message = "Min length = 1, Max length = 50")
    private String name;

}
