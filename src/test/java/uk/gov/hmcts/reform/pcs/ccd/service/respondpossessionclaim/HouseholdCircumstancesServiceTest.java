package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;

import static org.assertj.core.api.Assertions.assertThat;

class HouseholdCircumstancesServiceTest {

    private final HouseholdCircumstancesService service = new HouseholdCircumstancesService();

    @Test
    void shouldMapDependantChildrenField() {
        HouseholdCircumstances model = HouseholdCircumstances.builder()
            .dependantChildren(YesOrNo.YES)
            .build();

        HouseholdCircumstancesEntity entity = service.createHouseholdCircumstancesEntity(model);

        assertThat(entity.getDependantChildren()).isEqualTo(YesOrNo.YES);
    }

}

