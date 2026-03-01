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
class PublicEventSpecificationTest {

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
    @Mock
    private Join<Object, Object> join;

    @BeforeEach
    void setUp() {
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        lenient().when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        lenient().when(cb.conjunction()).thenReturn(predicate);
        lenient().when(cb.greaterThan(any(Path.class), any(Instant.class))).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(Path.class), any(Instant.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(Path.class), any(Instant.class))).thenReturn(predicate);
        lenient().when(cb.like(any(Expression.class), anyString())).thenReturn(predicate);
        lenient().when(cb.equal(any(Path.class), any())).thenReturn(predicate);
        lenient().when(cb.lessThan(any(Expression.class), any(Expression.class))).thenReturn(predicate);
        lenient().when(cb.count(any(From.class))).thenReturn(mock(Expression.class));
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(path.get(anyString())).thenReturn(path);
        lenient().when(path.in(anyList())).thenReturn(predicate);
        lenient().when(root.join(anyString(), any(JoinType.class))).thenReturn(join);
        lenient().when(cb.lower(any(Expression.class))).thenReturn(mock(Expression.class));
    }

    @Test
    void shouldAddDefaultDateFilterWhenRangeNull() {
        PublicEventSpecification specification = new PublicEventSpecification(
                false, null, null, null, null, null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).greaterThan(any(), any(Instant.class));
    }

    @Test
    void shouldAddTextFilterWhenTextProvided() {
        PublicEventSpecification specification = new PublicEventSpecification(
                false, null, null, null, null, "text"
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb, atLeast(2)).lower(any());
        verify(cb, times(2)).like(any(), anyString());
    }

    @Test
    void shouldAddPaidFilterWhenPaidProvided() {
        PublicEventSpecification specification = new PublicEventSpecification(
                false, null, null, true, null, null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(any(), eq(true));
    }

    @Test
    void shouldAddOnlyAvailableFilterWhenTrue() {
        PublicEventSpecification specification = new PublicEventSpecification(
                true, null, null, null, null, null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(root).join("requests", JoinType.LEFT);
        verify(query).groupBy(any(Expression.class));
    }

    @Test
    void shouldAddPublishedStateFilter() {
        PublicEventSpecification specification = new PublicEventSpecification(
                false, null, null, null, null, null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(any(), eq(EventState.PUBLISHED));
    }

    @Test
    void shouldAddCategoryFilter() {
        PublicEventSpecification specification = new PublicEventSpecification(
                false, null, null, null, List.of(1L, 2L), null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(path, atLeast(1)).in(anyList());
    }

    @Test
    void shouldAddRangeStartFilter() {
        PublicEventSpecification specification = new PublicEventSpecification(
                false, Instant.now(), null, null, null, null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).greaterThanOrEqualTo(any(), any(Instant.class));
    }

    @Test
    void shouldAddRangeEndFilter() {
        PublicEventSpecification specification = new PublicEventSpecification(
                false, null, Instant.now(), null, null, null
        );
        Predicate result = specification.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).lessThanOrEqualTo(any(), any(Instant.class));
    }
}