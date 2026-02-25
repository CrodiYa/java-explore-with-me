package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
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
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id " + id + " не найдена"));

        return CategoryMapper.toDto(category);
    }

    @Override
    public CategoryDto addCategory(CategoryDtoRequest request) {
        try {
            Category category = categoryRepository.save(CategoryMapper.toCategory(request));
            log.info("Successfully saved [{}]", category);
            return CategoryMapper.toDto(category);
        } catch (ConstraintViolationException e) {
            log.debug("Conflict during saving category [{}]", request, e);
            throw new ConflictException("Name is not unique");
        }
    }

    @Override
    public CategoryDto patchCategory(Long id, CategoryDtoRequest request) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Категория с id " + id + " не найдена"));

            log.info("Successfully patched [{}]", category);
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

        } catch (ConstraintViolationException e) {
            log.debug("Conflict during deleting category with id [{}]", id, e);
            throw new ConflictException("Some events have this category");
        }
    }
}
