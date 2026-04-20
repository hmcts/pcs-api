package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import java.util.Optional;

@Component
public class RentArrearsView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .map(ClaimEntity::getRentArrears)
            .ifPresent(rentArrearsEntity -> setRentArrearsFields(pcsCase, rentArrearsEntity));
    }

    private void setRentArrearsFields(PCSCase pcsCase, RentArrearsEntity rentArrearsEntity) {
        RentArrearsSection rentArrears = new RentArrearsSection();

        rentArrears.setTotal(rentArrearsEntity.getTotalRentArrears());
        rentArrears.setRentArrearsRecoveryAttempted(rentArrearsEntity.getRentArrearsRecoveryAttempted());
        rentArrears.setRentArrearsRecoveryAttemptDetails(rentArrearsEntity.getRentArrearsRecoveryAttemptDetails());

        pcsCase.setRentArrears(rentArrears);

        pcsCase.setArrearsJudgmentWanted(rentArrearsEntity.getArrearsJudgmentWanted());
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

}
