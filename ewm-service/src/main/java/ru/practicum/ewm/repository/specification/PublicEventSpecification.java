package ru.practicum.ewm.repository.specification;

import jakarta.persistence.criteria.*;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.participation.ParticipationRequest;

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

        // сравниваем поле entity (root - само entity) и значение paid
        if (paid != null) {
            predicates.add(cb.equal(root.get("paid"), paid));
        }

        if (onlyAvailable) {
            // "присоединяем" таблицу заявок к событиям
            // root — это таблица events
            // "requests" — название поля связи в твоей Entity (Event)
            Join<Event, ParticipationRequest> requests = root.join("requests", JoinType.LEFT);

            // группировка нужна чтобы COUNT работал правильно —
            // считай заявки ОТДЕЛЬНО для каждого события
            query.groupBy(root.get("id"));

            Predicate unlimited = cb.equal(root.get("participantLimit"), 0);

            // количество заявок < лимита - места есть
            // requests.get("id") - избавляет от дубликатов вместо просто requests
            Predicate available = cb.lessThan(cb.count(requests.get("id")), root.get("participantLimit"));

            // HAVING — фильтрует после GROUP BY и COUNT
            query.having(cb.or(unlimited, available));
        }

        predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

        return getPredicate(root, cb, predicates);
    }
}
