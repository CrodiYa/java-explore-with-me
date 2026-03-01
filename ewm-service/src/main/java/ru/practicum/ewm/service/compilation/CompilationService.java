package ru.practicum.ewm.service.compilation;

import ru.practicum.ewm.model.compilation.CompilationDto;
import ru.practicum.ewm.model.compilation.NewCompilationDto;
import ru.practicum.ewm.model.compilation.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto addCompilation(NewCompilationDto dto);

    CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest dto);

    void deleteCompilation(Long compilationId);

    List<CompilationDto> findCompilations(Boolean pinned, Integer from, Integer size);

    CompilationDto findCompilationById(Long compilationId);
}
