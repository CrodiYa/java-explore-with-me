package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.CategoryMapper;
import ru.practicum.ewm.model.entity.Category;
import ru.practicum.ewm.model.request.CategoryDtoRequest;
import ru.practicum.ewm.model.response.CategoryDto;
import ru.practicum.ewm.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDto> findAll(Integer from, Integer size) {
        int page = from / size;

        return categoryRepository.findAll(PageRequest.of(page, size)).stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto findById(Long id) {
        Category category = findEntityById(id);

        return CategoryMapper.toDto(category);
    }

    @Override
    public Category findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id " + id + " не найдена"));
    }


    @Override
    public CategoryDto addCategory(CategoryDtoRequest request) {
        try {
            Category category = categoryRepository.save(CategoryMapper.toCategory(request));
            log.info("Successfully saved category with id: {}", category.getId());
            return CategoryMapper.toDto(category);
        } catch (DataIntegrityViolationException e) {
            log.debug("Conflict during saving category [{}]", request, e);
            throw new ConflictException("Name is not unique");
        }
    }

    @Override
    public CategoryDto patchCategory(Long id, CategoryDtoRequest request) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Категория с id " + id + " не найдена"));

            log.info("Successfully patched category with id: {}", category.getId());
            CategoryMapper.merge(category, request);

            return CategoryMapper.toDto(categoryRepository.save(category));
        } catch (DataIntegrityViolationException e) {
            log.debug("Conflict during patching category [{}]", request, e);
            throw new ConflictException("Name is not unique");
        }
    }

    @Override
    public void deleteCategory(Long id) {
        try {
            if (!categoryRepository.existsById(id)) {
                throw new NotFoundException("Категория с id " + id + " не найдена");
            }

            categoryRepository.deleteById(id);

        } catch (DataIntegrityViolationException e) {
            log.debug("Conflict during deleting category with id [{}]", id, e);
            throw new ConflictException("Some events have this category");
        }
    }

    @Override
    public void throwIfCategoryNotFound(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category with id " + categoryId + " not found");
        }
    }
}
