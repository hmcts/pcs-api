package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftEventEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftEventRepository;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.idam.User;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class DraftEventService {

    private final DraftEventRepository draftEventRepository;
    private final IdamService idamService;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> getDraftEventData(long caseReference, EventId eventId, Class<T> eventDataClass) {
        UUID userId = getCurrentUserUuid();

        return draftEventRepository.findDraft(caseReference, userId, eventId)
            .map(draftEvent -> parseEventDataJson(draftEvent.getEventData(), eventDataClass));
    }

    public boolean draftExists(long caseReference, EventId eventId) {
        UUID userId = getCurrentUserUuid();

        return draftEventRepository.findDraft(caseReference, userId, eventId).isPresent();
    }

    public <T> void saveDraftEventData(long caseReference, EventId eventId, T eventData) {
        String eventDataString;
        try {
            eventDataString = objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            log.error("Unable to write draft event data JSON", e);
            return;
        }

        UUID userId = getCurrentUserUuid();

        DraftEventEntity draftEventEntity = draftEventRepository.findDraft(caseReference, userId, eventId)
            .map(existingDraft -> {
                existingDraft.setEventData(eventDataString);
                return existingDraft;
            }).orElseGet(() -> {
                DraftEventEntity newDraft = new DraftEventEntity();
                newDraft.setCaseReference(caseReference);
                newDraft.setUserId(userId);
                newDraft.setEventId(eventId);
                newDraft.setEventData(eventDataString);
                return newDraft;
            });

        draftEventRepository.save(draftEventEntity);
    }

    public void deleteDraftEventData(long caseReference, EventId eventId) {
        UUID userId = getCurrentUserUuid();

        draftEventRepository.deleteDraft(caseReference, userId, eventId);
    }

    private <T> T parseEventDataJson(String eventDataJson, Class<T> eventDataClass) {
        try {
            return objectMapper.readValue(eventDataJson, eventDataClass);
        } catch (JsonProcessingException e) {
            log.warn("Unable to parse draft event data JSON", e);
            return null;
        }
    }

    private UUID getCurrentUserUuid() {
        User currentUser = idamService.getCurrentUser();
        return UUID.fromString(currentUser.getUserDetails().getUid());
    }

}
