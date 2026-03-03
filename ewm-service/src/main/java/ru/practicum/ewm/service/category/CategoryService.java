package ru.practicum.ewm.service.category;

import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.category.CategoryDtoRequest;
import ru.practicum.ewm.model.category.CategoryDto;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> findAll(Integer from, Integer size);

    CategoryDto findById(Long id);

    Category findEntityById(Long id);

    CategoryDto addCategory(CategoryDtoRequest request);

    CategoryDto patchCategory(Long id, CategoryDtoRequest request);

    void deleteCategory(Long id);

    void throwIfCategoryNotFound(Long categoryId);
}
