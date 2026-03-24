package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;

@Service
public class HouseholdCircumstancesService {

    public HouseholdCircumstancesEntity createHouseholdCircumstancesEntity(HouseholdCircumstances circumstances) {

        if (circumstances == null) {
            return null;
        }

        HouseholdCircumstancesEntity householdCircumstancesEntity = HouseholdCircumstancesEntity.builder()
            .dependantChildren(circumstances.getDependantChildren())
            .shareAdditionalCircumstances(circumstances.getShareAdditionalCircumstances())
            .additionalCircumstancesDetails(circumstances.getAdditionalCircumstancesDetails())
            .exceptionalHardship(circumstances.getExceptionalHardship())
            .exceptionalHardshipDetails(circumstances.getExceptionalHardshipDetails())
            .build();

        return householdCircumstancesEntity;
    }
}
