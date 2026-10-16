package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.DefendantsDOB;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WarrantDetailsMapperTest {

    @InjectMocks
    private WarrantDetailsMapper underTest;

    private EnforcementOrderEntity enforcementOrderEntity;

    @BeforeEach
    void setUp() {
        enforcementOrderEntity = new EnforcementOrderEntity();
    }

    @Test
    void shouldMapToEntityWithNullWarrantDetails() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
    }

    @Test
    void shouldMapAdditionalInformation() {
        // Given
        AdditionalInformation additionalInfo = AdditionalInformation.builder()
            .additionalInformationSelect(VerticalYesNo.YES)
            .additionalInformationDetails("Additional details")
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder().additionalInformation(additionalInfo).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAdditionalInformationSelect()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getAdditionalInformationDetails()).isEqualTo("Additional details");
    }

    @Test
    void shouldHandleNullAdditionalInformation() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder()
            .additionalInformation(null)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantDetails(warrantDetails)
            .build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAdditionalInformationSelect()).isNull();
        assertThat(result.getAdditionalInformationDetails()).isNull();
    }

    @Test
    void shouldMapNameAndAddressForEviction() {
        // Given
        NameAndAddressForEviction nameAndAddress = NameAndAddressForEviction.builder()
            .correctNameAndAddress(VerticalYesNo.NO)
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder()
            .nameAndAddressForEviction(nameAndAddress)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantDetails(warrantDetails)
            .build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getCorrectNameAndAddress()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldMapPropertyAccessDetails() {
        // Given
        PropertyAccessDetails accessDetails = PropertyAccessDetails.builder()
            .isDifficultToAccessProperty(VerticalYesNo.YES)
            .clarificationOnAccessDifficultyText("Hard to access")
            .build();

        WarrantDetails warrantDetails = WarrantDetails.builder().propertyAccessDetails(accessDetails).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getIsDifficultToAccessProperty()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getClarificationOnAccessDifficultyText()).isEqualTo("Hard to access");
    }

    @Test
    void shouldMapDefendantsDOB() {
        // Given
        DefendantsDOB defendantsDOB = DefendantsDOB.builder().defendantsDOBDetails("01/01/1980").build();
        WarrantDetails warrantDetails = WarrantDetails.builder().defendantsDOBKnown(VerticalYesNo.YES)
            .defendantsDOB(defendantsDOB).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getDefendantsDOBKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getDefendantsDOBDetails()).isEqualTo("01/01/1980");
    }

    @Test
    void shouldHandleNullDefendantsDOB() {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder().defendantsDOBKnown(VerticalYesNo.NO)
            .defendantsDOB(null).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getDefendantsDOBKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(result.getDefendantsDOBDetails()).isNull();
    }

    @ParameterizedTest
    @EnumSource(YesNoNotSure.class)
    void shouldMapDefendantInBreathingSpace(YesNoNotSure defendantInBreathingSpace) {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder()
            .defendantInBreathingSpace(defendantInBreathingSpace)
            .build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getDefendantInBreathingSpace()).isEqualTo(defendantInBreathingSpace);
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldMapDefendantsMissedPayments(VerticalYesNo defendantMissedPayments) {
        // Given
        WarrantDetails warrantDetails = WarrantDetails.builder()
            .defendantMissedPayments(defendantMissedPayments)
            .build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getDefendantMissedPayments()).isEqualTo(defendantMissedPayments);
    }

    @Test
    void shouldMapCompleteWarrantDetails() {
        // Given
        EnforcementOrder enforcementOrder = createCompleteEnforcementOrder();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnforcementOrder()).isEqualTo(enforcementOrderEntity);
        assertThat(result.getAdditionalInformationSelect()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getAdditionalInformationDetails()).isEqualTo("Details");
    }

    private EnforcementOrder createCompleteEnforcementOrder() {
        WarrantDetails warrantDetails = WarrantDetails.builder()
            .showChangeNameAddressPage(YesOrNo.YES)
            .additionalInformation(AdditionalInformation.builder()
                                       .additionalInformationSelect(VerticalYesNo.YES)
                                       .additionalInformationDetails("Details")
                                       .build())
            .build();

        return EnforcementOrder.builder().warrantDetails(warrantDetails).build();
    }
}
