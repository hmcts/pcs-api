package uk.gov.hmcts.reform.pcs.ccd.service.breathingspace;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.breathingspace.BreathingSpaceEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.breathingspace.BreathingSpaceRepository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class BreathingSpaceService {

    private final PcsCaseService pcsCaseService;
    private final BreathingSpaceRepository breathingSpaceRepository;
    private final EnforcementOrderService enforcementOrderService;

    public void createBreathingSpace(long caseReference, BreathingSpaceDetails breathingSpaceDetails) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        BreathingSpaceEntity breathingSpaceEntity = createBreathingSpaceEntity(breathingSpaceDetails, pcsCaseEntity);
        breathingSpaceRepository.save(breathingSpaceEntity);
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

    public List<BreathingSpaceDetails> getBreathingSpaces(long caseReference) {
        return breathingSpaceRepository.findByCaseReference(caseReference)
            .stream()
            .map(entity -> BreathingSpaceDetails.builder()
                .referenceNumber(entity.getReferenceNumber())
                .startDate(entity.getStartDate())
                .type(entity.getType())
                .expectedEndDate(entity.getExpectedEndDate())
                .build())
            .toList();
    }


    public Optional<BreathingSpaceDetails> getLatestBreathingSpace(long caseReference) {
        return getBreathingSpaces(caseReference)
            .stream()
            .findFirst();
    }

    @Transactional
    public void enterBreathingSpace(long caseReference, BreathingSpaceDetails breathingSpaceDetails) {
        createBreathingSpace(caseReference, breathingSpaceDetails);
        enforcementOrderService.updateStatesForBreathingSpace(caseReference);
    }



}
