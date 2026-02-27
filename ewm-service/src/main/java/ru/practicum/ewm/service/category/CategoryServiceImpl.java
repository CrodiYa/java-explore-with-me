package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
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
        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Name is not unique");
        }

        Category category = categoryRepository.save(CategoryMapper.toCategory(request));
        log.info("Successfully saved category with id: {}", category.getId());
        return CategoryMapper.toDto(category);
    }

    @Override
    public CategoryDto patchCategory(Long id, CategoryDtoRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Name is not unique");
        }

        Category category = findEntityById(id);
        CategoryMapper.merge(category, request);

        Category patched = categoryRepository.save(category);
        log.info("Successfully patched category with id: {}", patched.getId());

        return CategoryMapper.toDto(patched);
    }

    @Override
    public void deleteCategory(Long id) {
        try {
            throwIfCategoryNotFound(id);

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
