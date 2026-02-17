package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.writ;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.writ.EnforcementWritEntity;

@Component
@Slf4j
public class WritDetailsMapper {

    public EnforcementWritEntity toEntity(WritDetails writDetails) {
        EnforcementWritEntity entity = new EnforcementWritEntity();

        // Map NameAndAddressForEviction fields
        mapNameAndAddressForEviction(writDetails.getNameAndAddressForEviction(), entity);

        // Map direct fields
        entity.setShowChangeNameAddressPage(writDetails.getShowChangeNameAddressPage());
        entity.setShowPeopleWhoWillBeEvictedPage(writDetails.getShowPeopleWhoWillBeEvictedPage());
        entity.setHasHiredHighCourtEnforcementOfficer(writDetails.getHasHiredHighCourtEnforcementOfficer());
        entity.setHceoDetails(writDetails.getHceoDetails());
        entity.setHasClaimTransferredToHighCourt(writDetails.getHasClaimTransferredToHighCourt());

        // Map LandRegistryFees fields
        mapLandRegistryFees(writDetails.getLandRegistryFees(), entity);

        // Map LegalCosts fields
        mapLegalCosts(writDetails.getLegalCosts(), entity);

        // Map MoneyOwedByDefendants fields
        mapMoneyOwedByDefendants(writDetails.getMoneyOwedByDefendants(), entity);

        return entity;
    }

    private void mapNameAndAddressForEviction(NameAndAddressForEviction nameAndAddress,
                                              EnforcementWritEntity entity) {
        if (nameAndAddress != null) {
            entity.setCorrectNameAndAddress(nameAndAddress.getCorrectNameAndAddress());
        }
    }

    private void mapLandRegistryFees(LandRegistryFees landRegistryFees, EnforcementWritEntity entity) {
        if (landRegistryFees != null) {
            entity.setHaveLandRegistryFeesBeenPaid(landRegistryFees.getHaveLandRegistryFeesBeenPaid());
            entity.setAmountOfLandRegistryFees(landRegistryFees.getAmountOfLandRegistryFees());
        }
    }

    private void mapLegalCosts(LegalCosts legalCosts, EnforcementWritEntity entity) {
        if (legalCosts != null) {
            entity.setAreLegalCostsToBeClaimed(legalCosts.getAreLegalCostsToBeClaimed());
            entity.setAmountOfLegalCosts(legalCosts.getAmountOfLegalCosts());
        }
    }

    private void mapMoneyOwedByDefendants(MoneyOwedByDefendants moneyOwedByDefendants,
                                          EnforcementWritEntity entity) {
        if (moneyOwedByDefendants != null) {
            entity.setAmountOwed(moneyOwedByDefendants.getAmountOwed());
        }
    }

}
