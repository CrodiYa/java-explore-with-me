package ru.practicum.ewm.mappers;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.event.EventStateAction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EventStateMapperTest {

    private final EventStateMapper mapper = new EventStateMapper();

    @Nested
    class MappingUserEventAction {

        @Test
        public void shouldMapCancelReviewToCanceled() {
            EventState result = mapper.mapUserEventAction(EventStateAction.CANCEL_REVIEW);

            assertEquals(EventState.CANCELED, result);
        }

        @Test
        public void shouldMapSendToReviewToPending() {
            EventState result = mapper.mapUserEventAction(EventStateAction.SEND_TO_REVIEW);

            assertEquals(EventState.PENDING, result);
        }

        @Test
        public void shouldReturnNullForNullInput() {
            EventState result = mapper.mapUserEventAction(null);

            assertNull(result);
        }

        @Test
        public void shouldReturnNullForPublishEvent() {
            EventState result = mapper.mapUserEventAction(EventStateAction.PUBLISH_EVENT);

            assertNull(result);
        }

        @Test
        public void shouldReturnNullForRejectEvent() {
            EventState result = mapper.mapUserEventAction(EventStateAction.REJECT_EVENT);

            assertNull(result);
        }
    }

    @Nested
    class MappingAdminEventAction {

        @Test
        public void shouldMapPublishEventToPublished() {
            EventState result = mapper.mapAdminEventAction(EventStateAction.PUBLISH_EVENT);

            assertEquals(EventState.PUBLISHED, result);
        }

        @Test
        public void shouldMapRejectEventToCanceled() {
            EventState result = mapper.mapAdminEventAction(EventStateAction.REJECT_EVENT);

            assertEquals(EventState.CANCELED, result);
        }

        @Test
        public void shouldReturnNullForNullInput() {
            EventState result = mapper.mapAdminEventAction(null);

            assertNull(result);
        }

        @Test
        public void shouldReturnNullForCancelReview() {
            EventState result = mapper.mapAdminEventAction(EventStateAction.CANCEL_REVIEW);

            assertNull(result);
        }

        @Test
        public void shouldReturnNullForSendToReview() {
            EventState result = mapper.mapAdminEventAction(EventStateAction.SEND_TO_REVIEW);

            assertNull(result);
        }
    }
}
