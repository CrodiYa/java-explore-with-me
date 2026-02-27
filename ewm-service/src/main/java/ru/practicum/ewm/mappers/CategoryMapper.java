package ru.practicum.ewm.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.ewm.model.entity.Category;
import ru.practicum.ewm.model.request.CategoryDtoRequest;
import ru.practicum.ewm.model.response.CategoryDto;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    Category toCategory(CategoryDtoRequest dto);

    // @MappingTarget - т к MapStruct находит name у обоих параметров
    // и не может определиться у какого взять конкретно
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void merge(@MappingTarget Category category, CategoryDtoRequest dto);
}
