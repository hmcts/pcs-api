package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
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
        rentArrearsEntity.setTotalRentArrears(rentArrearsTotal);
        rentArrearsEntity.setRentArrearsRecoveryAttempted(rentArrears.getRentArrearsRecoveryAttempted());
        rentArrearsEntity.setRentArrearsRecoveryAttemptDetails(rentArrears.getRentArrearsRecoveryAttemptDetails());
        rentArrearsEntity.setArrearsJudgmentWanted(pcsCase.getArrearsJudgmentWanted());

        return rentArrearsEntity;
    }
}
