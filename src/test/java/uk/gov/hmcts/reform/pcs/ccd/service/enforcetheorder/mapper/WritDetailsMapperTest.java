package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("WritDetailsMapper Tests")
class WritDetailsMapperTest {

    @InjectMocks
    private WritDetailsMapper underTest;

    private WritDetails writDetails;
    private NameAndAddressForEviction nameAndAddressForEviction;

    @BeforeEach
    void setUp() {
        nameAndAddressForEviction = NameAndAddressForEviction.builder()
            .correctNameAndAddress(VerticalYesNo.YES)
            .build();

        writDetails = WritDetails.builder()
            .nameAndAddressForEviction(nameAndAddressForEviction)
            .showChangeNameAddressPage(YesOrNo.NO)
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.YES)
            .hceoDetails("John Smith, Enforcement Ltd")
            .hasClaimTransferredToHighCourt(YesOrNo.YES)
            .build();
    }

    @Test
    void shouldMapAllFieldsSuccessfully() {
        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getCorrectNameAndAddress()).isEqualTo(VerticalYesNo.YES);
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(VerticalYesNo.YES);
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
        assertThat(entity.getHasClaimTransferredToHighCourt()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldHandleNullNameAndAddressForEviction() {
        // Given
        writDetails.setNameAndAddressForEviction(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getCorrectNameAndAddress()).isNull();
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
    }



    @Test
    void shouldHandleAllNullCompositeObjects() {
        // Given
        writDetails.setNameAndAddressForEviction(null);
        writDetails.setLandRegistryFees(null);
        writDetails.setLegalCosts(null);
        writDetails.setMoneyOwedByDefendants(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        assertThat(entity.getCorrectNameAndAddress()).isNull();
        assertThat(entity.getHceoDetails()).isEqualTo("John Smith, Enforcement Ltd");
    }

    @Test
    @DisplayName("Should map with all NO/false values")
    void shouldMapWithAllNoValues() {
        // Given
        WritDetails noWritDetails = WritDetails.builder()
            .nameAndAddressForEviction(NameAndAddressForEviction.builder()
                                           .correctNameAndAddress(VerticalYesNo.NO)
                                           .build())
            .showChangeNameAddressPage(YesOrNo.NO)
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.NO)
            .hasClaimTransferredToHighCourt(YesOrNo.NO)
            .build();

        // When
        WritEntity entity = underTest.toEntity(noWritDetails);

        // Then
        assertThat(entity.getCorrectNameAndAddress()).isEqualTo(VerticalYesNo.NO);
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(VerticalYesNo.NO);
        assertThat(entity.getHasClaimTransferredToHighCourt()).isEqualTo(YesOrNo.NO);
    }


    @Test
    void shouldMapWithMaxLengthHceoDetails() {
        // Given
        String maxLengthHceoDetails = "A".repeat(120);
        writDetails.setHceoDetails(maxLengthHceoDetails);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getHceoDetails()).hasSize(120);
        assertThat(entity.getHceoDetails()).isEqualTo(maxLengthHceoDetails);
    }

    @Test
    void shouldMapWithNullHceoDetails() {
        // Given
        writDetails.setHceoDetails(null);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getHceoDetails()).isNull();
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldCreateNewEntityInstanceOnEachCall() {
        // When
        WritEntity entity1 = underTest.toEntity(writDetails);
        WritEntity entity2 = underTest.toEntity(writDetails);

        // Then
        assertThat(entity1).isNotSameAs(entity2);
        assertThat(entity1.getHceoDetails()).isEqualTo(entity2.getHceoDetails());
    }

    @Test
    void shouldMapPartialDataCorrectly() {
        // Given
        WritDetails partialWritDetails = WritDetails.builder()
            .hasHiredHighCourtEnforcementOfficer(VerticalYesNo.NO)
            .hasClaimTransferredToHighCourt(YesOrNo.YES)
            .build();

        // When
        WritEntity entity = underTest.toEntity(partialWritDetails);

        // Then
        assertThat(entity.getHasHiredHighCourtEnforcementOfficer()).isEqualTo(VerticalYesNo.NO);
        assertThat(entity.getHasClaimTransferredToHighCourt()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getCorrectNameAndAddress()).isNull();
    }

    @Test
    void shouldMapLanguageUsed() {
        // Given
        writDetails.setLanguageUsed(LanguageUsed.ENGLISH);

        // When
        WritEntity entity = underTest.toEntity(writDetails);

        // Then
        assertThat(entity.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);
    }


}
