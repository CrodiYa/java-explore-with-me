package ru.practicum.ewm.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.participation.ParticipationStatus;
import ru.practicum.ewm.service.event.EventRequestCount;

import java.util.Collection;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    Event findEntityById(Long eventId);

    Collection<Event> findByInitiatorId(Long userId, PageRequest pageRequest);

    @Query("""
            SELECT r.event.id as eventId,
            COUNT(r) as count
            FROM ParticipationRequest r
            WHERE r.event.id IN :eventIds
            AND r.status = :status
            GROUP BY r.event.id
            """)
    List<EventRequestCount> countConfirmedRequestsByEventIds(List<Long> eventIds, ParticipationStatus status);
}
