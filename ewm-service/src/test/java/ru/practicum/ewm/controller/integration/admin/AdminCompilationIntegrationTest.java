package ru.practicum.ewm.controller.integration.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.compilation.CompilationDto;
import ru.practicum.ewm.model.compilation.NewCompilationDto;
import ru.practicum.ewm.model.compilation.UpdateCompilationRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
public class AdminCompilationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompilationRepository compilationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Event event1;
    private Event event2;
    private NewCompilationDto newCompilationDto;

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

        event1 = eventRepository.save(Event.builder()
                .title("Event 1")
                .annotation("Annotation for event 1 that is long enough")
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

        event2 = eventRepository.save(Event.builder()
                .title("Event 2")
                .annotation("Annotation for event 2 that is long enough")
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

        newCompilationDto = NewCompilationDto.builder()
                .title("Test Compilation")
                .pinned(true)
                .events(Set.of(event1.getId(), event2.getId()))
                .build();
    }

    @Test
    public void shouldAddCompilation() throws Exception {
        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCompilationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Test Compilation")))
                .andExpect(jsonPath("$.pinned", is(true)))
                .andExpect(jsonPath("$.events", hasSize(2)));
    }

    @Test
    public void shouldAddCompilationWithoutEvents() throws Exception {
        NewCompilationDto dto = NewCompilationDto.builder()
                .title("Empty Compilation")
                .pinned(false)
                .events(null)
                .build();

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Empty Compilation")))
                .andExpect(jsonPath("$.pinned", is(false)))
                .andExpect(jsonPath("$.events", hasSize(0)));
    }

    @Test
    public void shouldReturnBadRequestWhenAddingCompilationWithBlankTitle() throws Exception {
        NewCompilationDto dto = NewCompilationDto.builder()
                .title("")
                .pinned(true)
                .build();

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldUpdateCompilation() throws Exception {
        CompilationDto created = objectMapper.readValue(
                mockMvc.perform(post("/admin/compilations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newCompilationDto)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                CompilationDto.class);

        UpdateCompilationRequest updateRequest = UpdateCompilationRequest.builder()
                .title("Updated Title")
                .pinned(false)
                .events(Set.of(event1.getId()))
                .build();

        mockMvc.perform(patch("/admin/compilations/{compId}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.pinned", is(false)))
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].id", is(event1.getId().intValue())));
    }

    @Test
    public void shouldUpdateCompilationOnlyTitle() throws Exception {
        CompilationDto created = objectMapper.readValue(
                mockMvc.perform(post("/admin/compilations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newCompilationDto)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                CompilationDto.class);

        UpdateCompilationRequest updateRequest = UpdateCompilationRequest.builder()
                .title("Only Title Update")
                .build();

        mockMvc.perform(patch("/admin/compilations/{compId}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Only Title Update")))
                .andExpect(jsonPath("$.pinned", is(true)))
                .andExpect(jsonPath("$.events", hasSize(2)));
    }

    @Test
    public void shouldDeleteCompilation() throws Exception {
        CompilationDto created = objectMapper.readValue(
                mockMvc.perform(post("/admin/compilations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newCompilationDto)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                CompilationDto.class);

        mockMvc.perform(delete("/admin/compilations/{compId}", created.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/compilations/{compId}", created.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnNotFoundWhenDeletingNonExistingCompilation() throws Exception {
        mockMvc.perform(delete("/admin/compilations/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnBadRequestWhenUpdatingWithNegativeId() throws Exception {
        UpdateCompilationRequest updateRequest = UpdateCompilationRequest.builder()
                .title("Test")
                .build();

        mockMvc.perform(patch("/admin/compilations/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }
}
