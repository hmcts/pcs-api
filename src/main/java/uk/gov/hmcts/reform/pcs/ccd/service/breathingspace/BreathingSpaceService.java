package uk.gov.hmcts.reform.pcs.ccd.service.breathingspace;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.breathingspace.BreathingSpaceEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.breathingspace.BreathingSpaceRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.Optional;
import java.time.Clock;
import java.time.LocalDate;

@Service
@AllArgsConstructor
public class BreathingSpaceService {

    private final PcsCaseService pcsCaseService;
    private final BreathingSpaceRepository breathingSpaceRepository;
    private final EnforcementOrderService enforcementOrderService;
    @Qualifier("ukClock")
    private final Clock ukClock;

    public void createBreathingSpace(long caseReference, BreathingSpaceDetails breathingSpaceDetails) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        BreathingSpaceEntity breathingSpaceEntity = createBreathingSpaceEntity(breathingSpaceDetails, pcsCaseEntity);
        breathingSpaceRepository.save(breathingSpaceEntity);
    }

    public Optional<BreathingSpaceDetails> getActiveBreathingSpace(long caseReference) {
        return getActiveBreathingSpaceEntity(caseReference)
            .map(entity -> BreathingSpaceDetails.builder()
                .referenceNumber(entity.getReferenceNumber())
                .startDate(entity.getStartDate())
                .type(entity.getType())
                .expectedEndDate(entity.getExpectedEndDate())
                .build());
    }

    @Transactional
    public void liftBreathingSpace(long caseReference) {
        BreathingSpaceEntity activeBreathingSpace = getActiveBreathingSpaceEntity(caseReference)
            .orElseThrow(() -> new IllegalStateException(
                "No active breathing space found for case reference: " + caseReference
            ));

        activeBreathingSpace.setLiftedDate(LocalDate.now(ukClock));
        breathingSpaceRepository.save(activeBreathingSpace);
    }

    private Optional<BreathingSpaceEntity> getActiveBreathingSpaceEntity(long caseReference) {
        return breathingSpaceRepository
            .findFirstByPcsCaseCaseReferenceAndLiftedDateIsNullOrderByStartDateDesc(caseReference);
    }

    private BreathingSpaceEntity createBreathingSpaceEntity(
        BreathingSpaceDetails breathingSpaceDetails, PcsCaseEntity pcsCaseEntity) {
        return BreathingSpaceEntity.builder()
            .referenceNumber(breathingSpaceDetails.getReferenceNumber())
            .startDate(breathingSpaceDetails.getStartDate())
            .type(breathingSpaceDetails.getType())
            .pcsCase(pcsCaseEntity)
            .expectedEndDate(BreathingSpaceType.STANDARD.equals(breathingSpaceDetails.getType())
                    ? breathingSpaceDetails.getExpectedEndDate() : null
            )
            .build();
    }

    @Transactional
    public void enterBreathingSpace(long caseReference, BreathingSpaceDetails breathingSpaceDetails) {
        createBreathingSpace(caseReference, breathingSpaceDetails);
        enforcementOrderService.updateStatesForBreathingSpace(caseReference);
    }

}
