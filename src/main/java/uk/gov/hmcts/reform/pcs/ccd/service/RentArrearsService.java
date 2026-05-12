package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;

import java.math.BigDecimal;

@Service
public class RentArrearsService {

    public RentArrearsEntity createRentArrearsEntity(PCSCase pcsCase) {

        RentArrearsSection rentArrears = pcsCase.getRentArrears();
        BigDecimal rentArrearsTotal = rentArrears.getTotal();

        if (rentArrearsTotal == null) {
            return null;
        }

        RentArrearsEntity rentArrearsEntity = new RentArrearsEntity();
        VerticalYesNo recoveryAttempted = rentArrears.getRecoveryAttempted();
        rentArrearsEntity.setTotalRentArrears(rentArrearsTotal);
        rentArrearsEntity.setRecoveryAttempted(recoveryAttempted);
        rentArrearsEntity.setArrearsJudgmentWanted(pcsCase.getArrearsJudgmentWanted());

        if (recoveryAttempted == VerticalYesNo.YES) {
            rentArrearsEntity.setRecoveryAttemptDetails(rentArrears.getRecoveryAttemptDetails());
        }

        return rentArrearsEntity;
    }
}
