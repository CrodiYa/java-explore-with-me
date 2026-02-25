package ru.practicum.ewm.service.category;

import ru.practicum.ewm.model.request.CategoryDtoRequest;
import ru.practicum.ewm.model.response.CategoryDto;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> findAll(Integer from, Integer size);

    CategoryDto findById(Long id);

    CategoryDto addCategory(CategoryDtoRequest request);

    CategoryDto patchCategory(Long id, CategoryDtoRequest request);

    void deleteCategory(Long id);
}
