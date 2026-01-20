package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.ThirdPartyPaymentSource;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsPaymentSourceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        rentArrears.setThirdPartyPayments(rentArrearsEntity.getThirdPartyPaymentsMade());

        Set<RentArrearsPaymentSourceEntity> thirdPartyPaymentSourceEntities
            = rentArrearsEntity.getThirdPartyPaymentSources();

        List<ThirdPartyPaymentSource> thirdPartyPaymentSources = new ArrayList<>();

        thirdPartyPaymentSourceEntities.forEach(
            thirdPartyPaymentSourceEntity -> {
                thirdPartyPaymentSources.add(thirdPartyPaymentSourceEntity.getName());
                if (thirdPartyPaymentSourceEntity.getName() == ThirdPartyPaymentSource.OTHER) {
                    rentArrears.setThirdPartyPaymentSourceOther(thirdPartyPaymentSourceEntity.getDescription());
                }
            }
        );

        rentArrears.setThirdPartyPaymentSources(thirdPartyPaymentSources);

        pcsCase.setRentArrears(rentArrears);
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

}
