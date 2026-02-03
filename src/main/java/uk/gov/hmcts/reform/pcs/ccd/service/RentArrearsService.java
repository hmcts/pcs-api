package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.ThirdPartyPaymentSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsPaymentSourceEntity;

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

        VerticalYesNo thirdPartyPaymentsMade = rentArrears.getThirdPartyPayments();
        rentArrearsEntity.setThirdPartyPaymentsMade(thirdPartyPaymentsMade);
        if (thirdPartyPaymentsMade == VerticalYesNo.YES) {
            rentArrears.getThirdPartyPaymentSources().stream()
                .map(paymentSource -> createPaymentSourceEntity(paymentSource, rentArrears))
                .forEach(rentArrearsEntity::addThirdPartyPaymentSource);

        }
        rentArrearsEntity.setArrearsJudgmentWanted(pcsCase.getArrearsJudgmentWanted());

        return rentArrearsEntity;
    }

    private static RentArrearsPaymentSourceEntity createPaymentSourceEntity(ThirdPartyPaymentSource paymentSource,
                                                                            RentArrearsSection rentArrears) {

        RentArrearsPaymentSourceEntity paymentSourceEntity = new RentArrearsPaymentSourceEntity();

        paymentSourceEntity.setName(paymentSource);
        if (paymentSource == ThirdPartyPaymentSource.OTHER) {
            paymentSourceEntity.setDescription(rentArrears.getPaymentSourceOther());
        }

        return paymentSourceEntity;
    }

}
