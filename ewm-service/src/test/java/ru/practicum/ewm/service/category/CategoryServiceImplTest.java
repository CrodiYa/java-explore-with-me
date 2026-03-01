package ru.practicum.ewm.service.category;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mappers.CategoryMapper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.request.CategoryDtoRequest;
import ru.practicum.ewm.model.response.CategoryDto;
import ru.practicum.ewm.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
public class CategoryServiceImplTest {
    @Mock
    private CategoryRepository repository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private final CategoryDtoRequest request = new CategoryDtoRequest("name");
    private final Category category = new Category(1L, "name");
    private final CategoryDto categoryDto = new CategoryDto(1L, "name");

    @Nested
    class SavingCategory {
        @Test
        public void shouldSave() {
            when(categoryMapper.toCategory(request)).thenReturn(category);
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);
            when(repository.save(any(Category.class))).thenReturn(category);
            CategoryDto saved = categoryService.addCategory(request);
            assertEquals(request.getName(), saved.getName());
            verify(repository).save(any(Category.class));
            verify(categoryMapper).toCategory(any(CategoryDtoRequest.class));
            verify(categoryMapper).toDto(any(Category.class));
        }

        @Test
        public void shouldThrowConflict() {
            when(repository.existsByName(anyString())).thenReturn(true);
            assertThrows(ConflictException.class, () -> categoryService.addCategory(request));
            verify(repository).existsByName(anyString());
            verify(repository, never()).save(any(Category.class));
        }
    }

    @Nested
    class FindingCategoryById {
        @Test
        public void shouldFind() {
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);
            when(repository.findById(any(Long.class))).thenReturn(Optional.of(category));
            CategoryDto found = categoryService.findById(1L);
            assertEquals(1L, found.getId());
            assertEquals("name", found.getName());

            verify(repository).findById(any(Long.class));
            verify(categoryMapper).toDto(any(Category.class));
        }

        @Test
        public void shouldThrowNotFound() {
            when(repository.findById(any(Long.class))).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> categoryService.findById(1L));
            verify(repository).findById(any(Long.class));
        }

        @Test
        public void shouldFindEntity() {
            when(repository.findById(any(Long.class))).thenReturn(Optional.of(category));
            Category found = categoryService.findEntityById(1L);
            assertEquals(1L, found.getId());
            assertEquals("name", found.getName());

            verify(repository).findById(any(Long.class));
        }

        @Test
        public void shouldThrowNotFoundWhenEntity() {
            when(repository.findById(any(Long.class))).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> categoryService.findEntityById(1L));
            verify(repository).findById(any(Long.class));
        }

        @Test
        public void shouldFindAll() {
            int from = 0;
            int size = 10;
            int page = from / size;

            List<Category> categories = List.of(
                    new Category(1L, "name1"),
                    new Category(2L, "name2")
            );

            List<CategoryDto> expected = List.of(
                    new CategoryDto(1L, "name1"),
                    new CategoryDto(2L, "name2")
            );

            Page<Category> categoryPage = new PageImpl<>(categories);
            Pageable pageable = PageRequest.of(page, size);

            when(categoryMapper.toDto(categories.getFirst())).thenReturn(expected.getFirst());
            when(categoryMapper.toDto(categories.get(1))).thenReturn(expected.get(1));

            when(repository.findAll(pageable)).thenReturn(categoryPage);

            List<CategoryDto> actual = categoryService.findAll(from, size);

            assertEquals(expected, actual);
            verify(repository).findAll(pageable);
            verify(categoryMapper, times(2)).toDto(any(Category.class));
        }
    }

    @Nested
    class DeletingCategory {

        @Test
        public void shouldDelete() {
            when(repository.existsById(1L)).thenReturn(true);
            assertDoesNotThrow(() -> categoryService.deleteCategory(1L));
            verify(repository).existsById(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        public void shouldThrowNotFound() {
            when(repository.existsById(1L)).thenReturn(false);
            assertThrows(NotFoundException.class, () -> categoryService.deleteCategory(1L));
            verify(repository).existsById(1L);
        }

        @Test
        public void shouldThrowConflict() {
            when(repository.existsById(1L)).thenReturn(true);
            doThrow(DataIntegrityViolationException.class).when(repository).deleteById(1L);
            assertThrows(ConflictException.class, () -> categoryService.deleteCategory(1L));
        }
    }

    @Nested
    class PatchCategory {

        @Test
        public void shouldPath() {
            when(categoryMapper.toDto(category)).thenReturn(categoryDto);
            when(repository.findById(any(Long.class))).thenReturn(Optional.of(category));
            when(repository.save(any(Category.class))).thenReturn(category);
            assertDoesNotThrow(() -> categoryService.patchCategory(1L, new CategoryDtoRequest("name")));
            verify(repository).findById(any(Long.class));
            verify(repository).save(any(Category.class));
            verify(categoryMapper).toDto(any(Category.class));
        }

        @Test
        public void shouldNotFound() {
            when(repository.findById(any(Long.class))).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> categoryService.patchCategory(1L, new CategoryDtoRequest("name")));
            verify(repository).findById(any(Long.class));
            verify(repository, never()).save(any(Category.class));
        }

        @Test
        public void shouldThrowConflict() {
            when(repository.existsByNameAndIdNot("name", 1L)).thenReturn(true);
            assertThrows(ConflictException.class,
                    () -> categoryService.patchCategory(1L, new CategoryDtoRequest("name")));
            verify(repository).existsByNameAndIdNot("name", 1L);
            verify(repository, never()).findById(any(Long.class));
            verify(repository, never()).save(any(Category.class));
        }
    }

    @Nested
    class ExistsCategory {

        @Test
        public void shouldThrowWhenNotFound() {
            when(repository.existsById(any(Long.class))).thenReturn(false);
            assertThrows(NotFoundException.class, () -> categoryService.throwIfCategoryNotFound(1L));
            verify(repository).existsById(1L);
        }

        @Test
        public void shouldDoNothingWhenExists() {
            when(repository.existsById(any(Long.class))).thenReturn(true);
            assertDoesNotThrow(() -> categoryService.throwIfCategoryNotFound(1L));
            verify(repository).existsById(1L);
        }
    }
}
