package ru.practicum.ewm.repository.specification;

import jakarta.persistence.criteria.*;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.participation.ParticipationRequest;
import ru.practicum.ewm.model.participation.ParticipationStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PublicEventSpecification extends BaseEventSpecification {

    private final String text;
    private final Boolean paid;
    private final boolean onlyAvailable;

    public PublicEventSpecification(boolean onlyAvailable,
                                    Instant rangeStart, Instant rangeEnd, Boolean paid, List<Long> categories, String text) {
        super(categories, rangeStart, rangeEnd);
        this.onlyAvailable = onlyAvailable;
        this.paid = paid;
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

        if (paid != null) {
            predicates.add(cb.equal(root.get("paid"), paid));
        }

        if (onlyAvailable) {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ParticipationRequest> subRoot = subquery.from(ParticipationRequest.class);

            subquery.select(cb.count(subRoot))
                    .where(
                            cb.equal(subRoot.get("event").get("id"), root.get("id")),
                            cb.equal(subRoot.get("status"), ParticipationStatus.CONFIRMED)
                    );

            Predicate unlimited = cb.equal(root.get("participantLimit"), 0);
            Predicate available = cb.lessThan(subquery, root.get("participantLimit").as(Long.class));

            predicates.add(cb.or(unlimited, available));
        }

        predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

        return getPredicate(root, cb, predicates);
    }
}
