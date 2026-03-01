package ru.practicum.ewm.repository.specification;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminEventSpecificationTest {

    @Mock
    private Root<Event> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        lenient().when(cb.conjunction()).thenReturn(predicate);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(Path.class), any(Instant.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(Path.class), any(Instant.class))).thenReturn(predicate);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(path.get(anyString())).thenReturn(path);
        lenient().when(path.in(anyList())).thenReturn(predicate);
    }

    @Test
    void shouldReturnPredicateWhenUsersAndStatesProvided() {
        AdminEventSpecification specification = new AdminEventSpecification(
                List.of(1L, 2L),
                List.of(EventState.PENDING, EventState.PUBLISHED),
                null,
                null,
                null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(root, atLeast(1)).get("initiator");
        verify(root, atLeast(1)).get("state");
    }

    @Test
    void shouldReturnPredicateWhenCategoriesProvided() {
        AdminEventSpecification specification = new AdminEventSpecification(
                null,
                null,
                List.of(1L, 2L),
                null,
                null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(root, atLeast(1)).get("category");
    }


    @Test
    void shouldReturnPredicateWhenRangeStartProvided() {
        AdminEventSpecification specification = new AdminEventSpecification(
                null,
                null,
                null,
                Instant.now().minusSeconds(3600),
                null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).greaterThanOrEqualTo(any(), any(Instant.class));
    }

    @Test
    void shouldReturnPredicateWhenRangeEndProvided() {
        AdminEventSpecification specification = new AdminEventSpecification(
                null,
                null,
                null,
                null,
                Instant.now().plusSeconds(3600)
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).lessThanOrEqualTo(any(), any(Instant.class));
    }

    @Test
    void shouldReturnConjunctionWhenNoParams() {
        AdminEventSpecification specification = new AdminEventSpecification(
                null,
                null,
                null,
                null,
                null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
    }

    @Test
    void shouldReturnConjunctionWhenNoParamsEmpty() {
        AdminEventSpecification specification = new AdminEventSpecification(
                List.of(),
                List.of(),
                List.of(),
                null,
                null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
    }
}