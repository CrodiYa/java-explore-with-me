package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.model.participation.ParticipationRequest;
import ru.practicum.ewm.model.participation.ParticipationStatus;
import ru.practicum.ewm.service.event.EventRequestCount;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequesterId(Long requesterId);

    // для поста написано нельзя добавить повторный запрос
    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    // все заявки на конкретное событие
    List<ParticipationRequest> findByEventId(Long eventId);

    // найти все записи по полю id, где id входит в список (In)
    // нужен для патча в приват event controller для approve/reject запросов на участие
    List<ParticipationRequest> findAllByIdIn(List<Long> requestIds);

    @Modifying
    @Transactional
    @Query("""
            UPDATE ParticipationRequest pr
            SET pr.status = 'REJECTED'
            WHERE pr.event.id = :eventId
            AND pr.status=:status
            """)
    int rejectPendingRequests(Long eventId, ParticipationStatus status);

    // confirmedRequests в EventFullDto/EventShortDto — считаем подтверждённые заявки
    int countByEventIdAndStatus(Long eventId, ParticipationStatus status);

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
