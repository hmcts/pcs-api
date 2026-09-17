package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.exception.DraftResponseDataDeletionException;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class DraftResponseDeletionService {

    private final DraftCaseDataRepository draftCaseDataRepository;

    @Transactional
    public void deleteRespondPossessionClaimBatch(long discardDays) {
        Instant cutoff = Instant.now().minus(discardDays, java.time.temporal.ChronoUnit.DAYS);
        draftCaseDataRepository.deleteByEventIdAndCutoff(EventId.respondPossessionClaim.name(), cutoff);
    }

    public List<DraftCaseDataEntity> findExpiredDraftResponses(long discardDays, int sqlLimit) {
        Instant cutoff = Instant.now().minus(discardDays, java.time.temporal.ChronoUnit.DAYS);
        return draftCaseDataRepository.findExpiredDraftResponses(
            EventId.respondPossessionClaim,
            cutoff,
            PageRequest.of(0, sqlLimit)
        );
    }

    @Transactional
    public void deleteDraftData(DraftCaseDataEntity draftCaseDataEntity) {
        try {
            draftCaseDataRepository.delete(draftCaseDataEntity);
        } catch (Exception e) {
            log.error("Unexpected Error occurred while deleting DraftData with reference: "
                    + draftCaseDataEntity.getCaseReference() + ", event: " + draftCaseDataEntity.getEventId()
                    + ", party: " + draftCaseDataEntity.getPartyId() + ", organisation: "
                    + draftCaseDataEntity.getOrganisationId(), e);

            throw new DraftResponseDataDeletionException(draftCaseDataEntity.getCaseReference(),
                    draftCaseDataEntity.getEventId().toString(), draftCaseDataEntity.getPartyId(),
                    draftCaseDataEntity.getOrganisationId());
        }
    }
}
