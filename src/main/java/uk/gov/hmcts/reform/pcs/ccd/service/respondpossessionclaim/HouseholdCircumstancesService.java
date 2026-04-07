package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;

@Service
public class HouseholdCircumstancesService {

    public HouseholdCircumstancesEntity createHouseholdCircumstancesEntity(HouseholdCircumstances circumstances) {

        if (circumstances == null) {
            return null;
        }
        YesOrNo otherTenants = circumstances.getOtherTenants();
        YesNoNotSure alternativeAccommodation = circumstances.getAlternativeAccommodation();

        return HouseholdCircumstancesEntity.builder()
            .dependantChildren(circumstances.getDependantChildren())
            .dependantChildrenDetails(circumstances.getDependantChildrenDetails())
            .otherDependants(circumstances.getOtherDependants())
            .otherDependantDetails(circumstances.getOtherDependantDetails())
            .otherTenants(otherTenants)
            .otherTenantsDetails(otherTenants == YesOrNo.YES ? circumstances.getOtherTenantsDetails() : null)
            .alternativeAccommodation(alternativeAccommodation)
            .alternativeAccommodationTransferDate(alternativeAccommodation == YesNoNotSure.YES
                                                      ? circumstances.getAlternativeAccommodationTransferDate() : null)
            .build();
    }
}
