package ru.practicum.ewm.controller.integration.pub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.user.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "logging.level.root=ERROR",
        "spring.main.banner-mode=off"
})
public class PublicCompilationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompilationRepository compilationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Compilation compilation1;
    private Compilation compilation2;

    @BeforeEach
    public void setUp() {
        compilationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        User initiator = userRepository.save(User.builder()
                .name("Initiator")
                .email("initiator@test.com")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Test Category")
                .build());

        Instant futureDate = Instant.now().plus(3, ChronoUnit.HOURS);

        Event event1 = eventRepository.save(Event.builder()
                .title("Event 1")
                .annotation("Annotation for event 1 that is long enough to pass validation")
                .description("Description for event 1 that is long enough to pass validation")
                .lat(55.75)
                .lon(37.62)
                .eventDate(futureDate)
                .category(category)
                .initiator(initiator)
                .paid(false)
                .state(EventState.PUBLISHED)
                .participantLimit(10)
                .requestModeration(true)
                .build());

        Event event2 = eventRepository.save(Event.builder()
                .title("Event 2")
                .annotation("Annotation for event 2 that is long enough to pass validation")
                .description("Description for event 2 that is long enough to pass validation")
                .lat(55.76)
                .lon(37.63)
                .eventDate(futureDate)
                .category(category)
                .initiator(initiator)
                .paid(true)
                .state(EventState.PUBLISHED)
                .participantLimit(5)
                .requestModeration(false)
                .build());

        compilation1 = compilationRepository.save(Compilation.builder()
                .title("Pinned Compilation")
                .pinned(true)
                .events(Set.of(event1, event2))
                .build());

        compilation2 = compilationRepository.save(Compilation.builder()
                .title("Not Pinned Compilation")
                .pinned(false)
                .events(Set.of(event1))
                .build());
    }

    @Test
    public void shouldFindAllCompilations() throws Exception {
        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(compilation1.getId().intValue())))
                .andExpect(jsonPath("$[0].title", is("Pinned Compilation")))
                .andExpect(jsonPath("$[0].pinned", is(true)))
                .andExpect(jsonPath("$[0].events", hasSize(2)))
                .andExpect(jsonPath("$[1].id", is(compilation2.getId().intValue())))
                .andExpect(jsonPath("$[1].title", is("Not Pinned Compilation")))
                .andExpect(jsonPath("$[1].pinned", is(false)))
                .andExpect(jsonPath("$[1].events", hasSize(1)));
    }

    @Test
    public void shouldFindPinnedCompilations() throws Exception {
        mockMvc.perform(get("/compilations")
                        .param("pinned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(compilation1.getId().intValue())))
                .andExpect(jsonPath("$[0].pinned", is(true)));
    }

    @Test
    public void shouldFindNotPinnedCompilations() throws Exception {
        mockMvc.perform(get("/compilations")
                        .param("pinned", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(compilation2.getId().intValue())))
                .andExpect(jsonPath("$[0].pinned", is(false)));
    }

    @Test
    public void shouldFindCompilationsWithPagination() throws Exception {
        mockMvc.perform(get("/compilations")
                        .param("from", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/compilations")
                        .param("from", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    public void shouldReturnEmptyListWhenNoCompilationsWithFilter() throws Exception {
        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "10")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void shouldFindCompilationById() throws Exception {
        mockMvc.perform(get("/compilations/{compId}", compilation1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(compilation1.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Pinned Compilation")))
                .andExpect(jsonPath("$.pinned", is(true)))
                .andExpect(jsonPath("$.events", hasSize(2)));
    }

    @Test
    public void shouldFindCompilationByIdWithNoEvents() throws Exception {
        Compilation emptyCompilation = compilationRepository.save(Compilation.builder()
                .title("Empty Compilation")
                .pinned(false)
                .events(Set.of())
                .build());

        mockMvc.perform(get("/compilations/{compId}", emptyCompilation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(emptyCompilation.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Empty Compilation")))
                .andExpect(jsonPath("$.pinned", is(false)))
                .andExpect(jsonPath("$.events", hasSize(0)));
    }

    @Test
    public void shouldReturnNotFoundWhenCompilationDoesNotExist() throws Exception {
        mockMvc.perform(get("/compilations/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestWithNegativeCompilationId() throws Exception {
        mockMvc.perform(get("/compilations/-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestWithZeroCompilationId() throws Exception {
        mockMvc.perform(get("/compilations/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldUseDefaultPaginationValues() throws Exception {
        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
