package ru.practicum.ewm.mappers;

import org.mapstruct.*;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.request.CategoryDtoRequest;
import ru.practicum.ewm.model.response.CategoryDto;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    @Mapping(target = "id", ignore = true)
    Category toCategory(CategoryDtoRequest dto);

    // @MappingTarget - т к MapStruct находит name у обоих параметров
    // и не может определиться у какого взять конкретно
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void merge(@MappingTarget Category category, CategoryDtoRequest dto);
}
