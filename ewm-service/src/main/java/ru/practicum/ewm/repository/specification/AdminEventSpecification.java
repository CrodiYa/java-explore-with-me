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

import static ru.practicum.ewm.repository.specification.PublicEventSpecification.getPredicate;

public class AdminEventSpecification implements Specification<Event> {

    private final List<Long> users;
    private final List<EventState> states;
    private final List<Long> categories;
    private final Instant rangeStart;
    private final Instant rangeEnd;

    public AdminEventSpecification(List<Long> users, List<EventState> states, List<Long> categories,
                                   Instant rangeStart, Instant rangeEnd) {
        super();
        this.users = users;
        this.states = states;
        this.categories = categories;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    @Override
    public Predicate toPredicate(Root<Event> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (users != null && !users.isEmpty()) {
            predicates.add(root.get("initiator").get("id").in(users));
        }

        if (states != null && !states.isEmpty()) {
            predicates.add(root.get("state").in(states));
        }

        return getPredicate(root, cb, predicates, rangeStart, rangeEnd, categories);
    }
}
