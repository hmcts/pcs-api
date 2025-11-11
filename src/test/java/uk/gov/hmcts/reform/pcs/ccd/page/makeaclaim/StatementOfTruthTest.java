package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatementOfTruthTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new StatementOfTruth());
    }

    @Test
    void shouldInitializeStatementOfTruthDetailsWhenNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(null)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getStatementOfTruth()).isNotNull();
    }

    @Test
    void shouldAutoSetAgreementClaimantWhenCompletedByIsClaimant() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getStatementOfTruth().getAgreementClaimant())
            .containsExactly(StatementOfTruthAgreementClaimant.BELIEVE_TRUE);
    }

    @Test
    void shouldClearLegalRepFieldsWhenCompletedByIsClaimant() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
                .agreementLegalRep(List.of(StatementOfTruthAgreementLegalRep.AGREED))
                .fullNameLegalRep("Jane Doe")
                .firmNameLegalRep("Smith & Co Solicitors")
                .positionLegalRep("Partner")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        StatementOfTruthDetails statementOfTruth = response.getData().getStatementOfTruth();
        assertThat(statementOfTruth.getAgreementLegalRep()).isNull();
        assertThat(statementOfTruth.getFullNameLegalRep()).isNull();
        assertThat(statementOfTruth.getFirmNameLegalRep()).isNull();
        assertThat(statementOfTruth.getPositionLegalRep()).isNull();
    }

    @Test
    void shouldPreserveClaimantFieldsWhenCompletedByIsClaimant() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
                .fullNameClaimant("John Smith")
                .positionClaimant("Director")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        StatementOfTruthDetails statementOfTruth = response.getData().getStatementOfTruth();
        assertThat(statementOfTruth.getFullNameClaimant()).isEqualTo("John Smith");
        assertThat(statementOfTruth.getPositionClaimant()).isEqualTo("Director");
    }

    @Test
    void shouldAutoSetAgreementLegalRepWhenCompletedByIsLegalRepresentative() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getStatementOfTruth().getAgreementLegalRep())
            .containsExactly(StatementOfTruthAgreementLegalRep.AGREED);
    }

    @Test
    void shouldClearClaimantFieldsWhenCompletedByIsLegalRepresentative() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
                .agreementClaimant(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE))
                .fullNameClaimant("John Smith")
                .positionClaimant("Director")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        StatementOfTruthDetails statementOfTruth = response.getData().getStatementOfTruth();
        assertThat(statementOfTruth.getAgreementClaimant()).isNull();
        assertThat(statementOfTruth.getFullNameClaimant()).isNull();
        assertThat(statementOfTruth.getPositionClaimant()).isNull();
    }

    @Test
    void shouldPreserveLegalRepFieldsWhenCompletedByIsLegalRepresentative() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
                .fullNameLegalRep("Jane Doe")
                .firmNameLegalRep("Smith & Co Solicitors")
                .positionLegalRep("Partner")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        StatementOfTruthDetails statementOfTruth = response.getData().getStatementOfTruth();
        assertThat(statementOfTruth.getFullNameLegalRep()).isEqualTo("Jane Doe");
        assertThat(statementOfTruth.getFirmNameLegalRep()).isEqualTo("Smith & Co Solicitors");
        assertThat(statementOfTruth.getPositionLegalRep()).isEqualTo("Partner");
    }

    @Test
    void shouldHandleNullCompletedBy() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(StatementOfTruthDetails.builder()
                .completedBy(null)
                .agreementClaimant(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE))
                .agreementLegalRep(List.of(StatementOfTruthAgreementLegalRep.AGREED))
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        StatementOfTruthDetails statementOfTruth = response.getData().getStatementOfTruth();
        assertThat(statementOfTruth.getCompletedBy()).isNull();
        // Should not auto-set agreements when completedBy is null
        assertThat(statementOfTruth.getAgreementClaimant())
            .containsExactly(StatementOfTruthAgreementClaimant.BELIEVE_TRUE);
        assertThat(statementOfTruth.getAgreementLegalRep())
            .containsExactly(StatementOfTruthAgreementLegalRep.AGREED);
    }

    @Test
    void shouldHandleExistingDataWhenSwitchingFromClaimantToLegalRep() {
        // Given - Initially set as claimant
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
                .agreementClaimant(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE))
                .fullNameClaimant("John Smith")
                .positionClaimant("Director")
                .fullNameLegalRep("Jane Doe")
                .firmNameLegalRep("Smith & Co Solicitors")
                .positionLegalRep("Partner")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        StatementOfTruthDetails statementOfTruth = response.getData().getStatementOfTruth();
        assertThat(statementOfTruth.getAgreementLegalRep())
            .containsExactly(StatementOfTruthAgreementLegalRep.AGREED);
        assertThat(statementOfTruth.getAgreementClaimant()).isNull();
        assertThat(statementOfTruth.getFullNameClaimant()).isNull();
        assertThat(statementOfTruth.getPositionClaimant()).isNull();
        // Legal rep fields should be preserved
        assertThat(statementOfTruth.getFullNameLegalRep()).isEqualTo("Jane Doe");
        assertThat(statementOfTruth.getFirmNameLegalRep()).isEqualTo("Smith & Co Solicitors");
        assertThat(statementOfTruth.getPositionLegalRep()).isEqualTo("Partner");
    }

    @Test
    void shouldHandleExistingDataWhenSwitchingFromLegalRepToClaimant() {
        // Given - Initially set as legal rep
        PCSCase caseData = PCSCase.builder()
            .statementOfTruth(StatementOfTruthDetails.builder()
                .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
                .agreementLegalRep(List.of(StatementOfTruthAgreementLegalRep.AGREED))
                .fullNameLegalRep("Jane Doe")
                .firmNameLegalRep("Smith & Co Solicitors")
                .positionLegalRep("Partner")
                .fullNameClaimant("John Smith")
                .positionClaimant("Director")
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        StatementOfTruthDetails statementOfTruth = response.getData().getStatementOfTruth();
        assertThat(statementOfTruth.getAgreementClaimant())
            .containsExactly(StatementOfTruthAgreementClaimant.BELIEVE_TRUE);
        assertThat(statementOfTruth.getAgreementLegalRep()).isNull();
        assertThat(statementOfTruth.getFullNameLegalRep()).isNull();
        assertThat(statementOfTruth.getFirmNameLegalRep()).isNull();
        assertThat(statementOfTruth.getPositionLegalRep()).isNull();
        // Claimant fields should be preserved
        assertThat(statementOfTruth.getFullNameClaimant()).isEqualTo("John Smith");
        assertThat(statementOfTruth.getPositionClaimant()).isEqualTo("Director");
    }

}

