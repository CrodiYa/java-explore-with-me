package ru.practicum.ewm.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.model.event.Event;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class BaseEventSpecification implements Specification<Event> {
    protected final List<Long> categories;
    protected final Instant rangeStart;
    protected final Instant rangeEnd;

    @Override
    public Predicate toPredicate(Root<Event> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return null;
    }

    protected Predicate getPredicate(Root<Event> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (rangeStart != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }
        if (rangeEnd != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }
        if (categories != null && !categories.isEmpty()) {
            predicates.add(root.get("category").get("id").in(categories));
        }
        if (predicates.isEmpty()) {
            return cb.conjunction();
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
