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

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class DraftResponseDeletionService {

    private final DraftCaseDataRepository draftCaseDataRepository;

    public List<DraftCaseDataEntity> findExpiredDraftResponses(long discardDays, int sqlLimit) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(discardDays);
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
