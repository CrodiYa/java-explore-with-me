package ru.practicum.ewm.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PublicEventSpecification implements Specification<Event> {

    private final String text;
    private final List<Long> categories;
    private final Boolean paid;
    private final Instant rangeStart;
    private final Instant rangeEnd;
    private final boolean onlyAvailable;

    public PublicEventSpecification(boolean onlyAvailable,
             Instant rangeStart, Instant rangeEnd, Boolean paid, List<Long> categories, String text) {
        super();
        this.onlyAvailable = onlyAvailable;
        this.rangeEnd = rangeEnd;
        this.rangeStart = rangeStart;
        this.paid = paid;
        this.categories = categories;
        this.text = text;
    }

    @Override
    public Predicate toPredicate(Root<Event> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (rangeStart == null && rangeEnd == null) {
            predicates.add(cb.greaterThan(root.get("eventDate"), Instant.now()));
        }

        if (text != null) {
            String pattern = "%" + text.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("annotation")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }

        // сравниваем поле entity (root - само entity) и значение paid
        if (paid != null)
            predicates.add(cb.equal(root.get("paid"), paid));

        predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

        return getPredicate(root, cb, predicates, rangeStart, rangeEnd, categories);
    }

    static Predicate getPredicate(Root<Event> root, CriteriaBuilder cb, List<Predicate> predicates, Instant rangeStart,
                                  Instant rangeEnd, List<Long> categories) {
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
