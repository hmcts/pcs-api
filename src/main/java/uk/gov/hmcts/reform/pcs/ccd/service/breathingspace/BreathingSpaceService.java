package uk.gov.hmcts.reform.pcs.ccd.service.breathingspace;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.breathingspace.BreathingSpaceEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.breathingspace.BreathingSpaceRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

@Service
@AllArgsConstructor
public class BreathingSpaceService {

    private final PcsCaseService pcsCaseService;
    private final BreathingSpaceRepository breathingSpaceRepository;

    public void createBreathingSpace(long caseReference, BreathingSpaceDetails breathingSpaceDetails) {
        ClaimEntity claimEntity = this.pcsCaseService.retrieveClaimEntity(caseReference);
        if (claimEntity == null) {
            throw new IllegalArgumentException("Claim not found for case reference: " + caseReference);
        }
        BreathingSpaceEntity breathingSpaceEntity = this.createBreathingSpaceEntity(breathingSpaceDetails, claimEntity);
        this.breathingSpaceRepository.save(breathingSpaceEntity);
    }

    private BreathingSpaceEntity createBreathingSpaceEntity(
        BreathingSpaceDetails breathingSpaceDetails, ClaimEntity claimEntity) {
        return BreathingSpaceEntity.builder()
            .referenceNumber(breathingSpaceDetails.getReferenceNumber())
            .startDate(breathingSpaceDetails.getStartDate())
            .type(breathingSpaceDetails.getType())
            .claim(claimEntity)
            .expectedEndDate(BreathingSpaceType.STANDARD.equals(breathingSpaceDetails.getType())
                    ? breathingSpaceDetails.getExpectedEndDate() : null
            )
            .build();
    }

}
