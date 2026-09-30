package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.DefendantsDOB;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.NameAndAddressForEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.AgreementClaimantLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;

import java.util.Collections;
import java.util.List;

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

    @Test
    void shouldMapStatementOfTruth() {
        // Given
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCompletedBy(StatementOfTruthCompletedBy.CLAIMANT);
        statementOfTruth.setFullNameParty("John Doe");
        statementOfTruth.setPositionParty("Owner");
        statementOfTruth.setFullNameLegalRep("Jane Smith");
        statementOfTruth.setFirmNameLegalRep("Smith & Co");
        statementOfTruth.setPositionLegalRep("Solicitor");
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.CLAIMANT);
        assertThat(result.getFullNameClaimant()).isEqualTo("John Doe");
        assertThat(result.getPositionClaimant()).isEqualTo("Owner");
        assertThat(result.getFullNameLegalRep()).isEqualTo("Jane Smith");
        assertThat(result.getFirmNameLegalRep()).isEqualTo("Smith & Co");
        assertThat(result.getPositionLegalRep()).isEqualTo("Solicitor");
    }

    @Test
    void shouldMapAgreementClaimant() {
        // Given
        List<StatementOfTruthAgreementClaimant> agreements = List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE);
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setAgreementClaimant(agreements);
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAgreementClaimant()).isEqualTo(StatementOfTruthAgreementClaimant.BELIEVE_TRUE.name());
    }

    @Test
    void shouldHandleEmptyAgreementClaimant() {
        // Given
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setAgreementClaimant(Collections.emptyList());
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAgreementClaimant()).isNull();
    }

    @Test
    void shouldMapAgreementLegalRep() {
        // Given
        List<AgreementClaimantLegalRep> agreements = List.of(AgreementClaimantLegalRep.AGREED);
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setAgreementClaimantLegalRep(agreements);
        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getAgreementLegalRep()).isEqualTo(AgreementClaimantLegalRep.AGREED.name());
    }

    @Test
    void shouldMapCertification() {
        // Given
        List<StatementOfTruthAgreement> certifications = List.of(StatementOfTruthAgreement.CERTIFY);
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCertification(certifications);

        WarrantDetails warrantDetails = WarrantDetails.builder().statementOfTruth(statementOfTruth).build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().warrantDetails(warrantDetails).build();

        // When
        WarrantEntity result = underTest.toEntity(enforcementOrder, enforcementOrderEntity);

        // Then
        assertThat(result.getCertification()).isEqualTo(StatementOfTruthAgreement.CERTIFY.name());
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
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
    }

    private EnforcementOrder createCompleteEnforcementOrder() {
        StatementOfTruthDetailsEnforcement statementOfTruth = getStatementOfTruthDetailsEnforcement();

        WarrantDetails warrantDetails = WarrantDetails.builder()
            .showChangeNameAddressPage(YesOrNo.YES)
            .additionalInformation(AdditionalInformation.builder()
                                       .additionalInformationSelect(VerticalYesNo.YES)
                                       .additionalInformationDetails("Details")
                                       .build())
            .statementOfTruth(statementOfTruth)
            .build();

        return EnforcementOrder.builder().warrantDetails(warrantDetails).build();
    }

    private static @NotNull StatementOfTruthDetailsEnforcement getStatementOfTruthDetailsEnforcement() {
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCompletedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
        statementOfTruth.setFullNameLegalRep("Legal Rep Name");
        statementOfTruth.setFirmNameLegalRep("Law Firm");
        statementOfTruth.setPositionLegalRep("Senior Partner");
        statementOfTruth.setAgreementClaimantLegalRep(List.of(AgreementClaimantLegalRep.AGREED));
        statementOfTruth.setCertification(List.of(StatementOfTruthAgreement.CERTIFY));
        return statementOfTruth;
    }
}
