package ru.practicum.ewm.mappers;

import ru.practicum.ewm.model.entity.Category;
import ru.practicum.ewm.model.request.CategoryDtoRequest;
import ru.practicum.ewm.model.response.CategoryDto;

public class CategoryMapper {
    public static CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static Category toCategory(CategoryDtoRequest dto) {
        return Category.builder().name(dto.getName()).build();
    }

    public static Category merge(Category category, CategoryDtoRequest dto) {
        if (dto.getName() != null) {
            category.setName(dto.getName());
        }

        return category;
    }
}
