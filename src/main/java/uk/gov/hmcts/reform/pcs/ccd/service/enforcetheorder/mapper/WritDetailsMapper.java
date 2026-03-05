package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;

@Component
@Slf4j
public class WritDetailsMapper {

    public WritEntity toEntity(WritDetails writDetails) {
        WritEntity entity = new WritEntity();

        // Map NameAndAddressForEviction fields
        mapNameAndAddressForEviction(writDetails.getNameAndAddressForEviction(), entity);

        // Map direct fields
        entity.setHasHiredHighCourtEnforcementOfficer(
            convertToVerticalYesNo(writDetails.getHasHiredHighCourtEnforcementOfficer()));
        entity.setHceoDetails(writDetails.getHceoDetails());
        entity.setHasClaimTransferredToHighCourt(convertYesOrNo(writDetails.getHasClaimTransferredToHighCourt()));

        // Map LandRegistryFees fields
        mapLandRegistryFees(writDetails.getLandRegistryFees(), entity);

        // Map LegalCosts fields
        mapLegalCosts(writDetails.getLegalCosts(), entity);

        // Map MoneyOwedByDefendants fields
        mapMoneyOwedByDefendants(writDetails.getMoneyOwedByDefendants(), entity);

        return entity;
    }

    private void mapNameAndAddressForEviction(NameAndAddressForEviction nameAndAddress,
                                              WritEntity entity) {
        if (nameAndAddress != null) {
            entity.setCorrectNameAndAddress(nameAndAddress.getCorrectNameAndAddress());
        }
    }

    private void mapLandRegistryFees(LandRegistryFees landRegistryFees, WritEntity entity) {
        if (landRegistryFees != null) {
            entity.setHaveLandRegistryFeesBeenPaid(landRegistryFees.getHaveLandRegistryFeesBeenPaid());
            entity.setAmountOfLandRegistryFees(landRegistryFees.getAmountOfLandRegistryFees());
        }
    }

    private void mapLegalCosts(LegalCosts legalCosts, WritEntity entity) {
        if (legalCosts != null) {
            entity.setAreLegalCostsToBeClaimed(legalCosts.getAreLegalCostsToBeClaimed());
            entity.setAmountOfLegalCosts(legalCosts.getAmountOfLegalCosts());
        }
    }

    private void mapMoneyOwedByDefendants(MoneyOwedByDefendants moneyOwedByDefendants,
                                          WritEntity entity) {
        if (moneyOwedByDefendants != null) {
            entity.setAmountOwed(moneyOwedByDefendants.getAmountOwed());
        }
    }

    private YesOrNo convertYesOrNo(YesOrNo yesOrNo) {
        return yesOrNo == YesOrNo.YES ? YesOrNo.YES : YesOrNo.NO;
    }

    private VerticalYesNo convertToVerticalYesNo(VerticalYesNo yesOrNo) {
        return yesOrNo == VerticalYesNo.YES ? VerticalYesNo.YES : VerticalYesNo.NO;
    }

}
