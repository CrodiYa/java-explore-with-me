package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.model.request.CategoryDtoRequest;
import ru.practicum.ewm.model.response.CategoryDto;
import ru.practicum.ewm.service.category.CategoryService;

@Slf4j
@RestController
@RequestMapping(path = "/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto addCategory(@RequestBody @Valid CategoryDtoRequest request) {
        return categoryService.addCategory(request);
    }

    @PatchMapping("/{catId}")
    public CategoryDto patchCategory(@PathVariable @Positive Long catId,
                                     @RequestBody @Valid CategoryDtoRequest request) {
        return categoryService.patchCategory(catId, request);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable @Positive Long catId) {
        categoryService.deleteCategory(catId);
    }
}
