package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HouseholdCircumstancesServiceTest {

    private HouseholdCircumstancesService underTest;

    @BeforeEach
    void setUp() {
        underTest = new HouseholdCircumstancesService();
    }

    @Test
    void shouldMapDependantChildrenField() {
        //Given
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .dependantChildren(YesOrNo.YES)
            .build();

        //When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(householdCircumstances);

        //Then
        assertThat(entity.getDependantChildren()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldReturnNullWhenHouseholdCircumstancesIsNull() {
        // When
        HouseholdCircumstancesEntity entity = underTest.createHouseholdCircumstancesEntity(null);

        // Then
        assertThat(entity).isNull();
    }

}

