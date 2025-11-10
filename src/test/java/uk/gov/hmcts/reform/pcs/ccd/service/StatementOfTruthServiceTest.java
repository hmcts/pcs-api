package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.model.StatementOfTruth;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementOfTruthServiceTest {

    private final StatementOfTruthService statementOfTruthService = new StatementOfTruthService();

    @Mock
    private PCSCase pcsCase;

    @Test
    void shouldReturnNullWhenStatementOfTruthCompletedByIsNull() {
        // Given
        when(pcsCase.getStatementOfTruthCompletedBy()).thenReturn(null);

        // When
        StatementOfTruth result = statementOfTruthService.buildStatementOfTruth(pcsCase);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldBuildStatementOfTruthForClaimant() {
        // Given
        when(pcsCase.getStatementOfTruthCompletedBy()).thenReturn(StatementOfTruthCompletedBy.CLAIMANT);
        when(pcsCase.getStatementOfTruthAgreementClaimant())
            .thenReturn(Arrays.asList(StatementOfTruthAgreementClaimant.BELIEVE_TRUE));
        when(pcsCase.getStatementOfTruthFullNameClaimant()).thenReturn("John Smith");
        when(pcsCase.getStatementOfTruthPositionClaimant()).thenReturn("Director");

        // When
        StatementOfTruth result = statementOfTruthService.buildStatementOfTruth(pcsCase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompletedBy()).isEqualTo("CLAIMANT");
        assertThat(result.getAgreementClaimant()).hasSize(1);
        assertThat(result.getAgreementClaimant().get(0))
            .isEqualTo("I believe that the facts stated in this claim form are true.");
        assertThat(result.getFullNameClaimant()).isEqualTo("John Smith");
        assertThat(result.getPositionClaimant()).isEqualTo("Director");
        // Legal rep fields should be null
        assertThat(result.getAgreementLegalRep()).isNull();
        assertThat(result.getFullNameLegalRep()).isNull();
        assertThat(result.getFirmNameLegalRep()).isNull();
        assertThat(result.getPositionLegalRep()).isNull();
    }

    @Test
    void shouldBuildStatementOfTruthForLegalRepresentative() {
        // Given
        when(pcsCase.getStatementOfTruthCompletedBy())
            .thenReturn(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
        when(pcsCase.getStatementOfTruthAgreementLegalRep())
            .thenReturn(Arrays.asList(StatementOfTruthAgreementLegalRep.AGREED));
        when(pcsCase.getStatementOfTruthFullNameLegalRep()).thenReturn("Jane Doe");
        when(pcsCase.getStatementOfTruthFirmNameLegalRep()).thenReturn("Smith & Co Solicitors");
        when(pcsCase.getStatementOfTruthPositionLegalRep()).thenReturn("Partner");

        // When
        StatementOfTruth result = statementOfTruthService.buildStatementOfTruth(pcsCase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompletedBy()).isEqualTo("LEGAL_REPRESENTATIVE");
        assertThat(result.getAgreementLegalRep()).hasSize(1);
        assertThat(result.getAgreementLegalRep().get(0))
            .isEqualTo("The claimant believes that the facts stated in this claim form are true. "
                + "I am authorised by the claimant to sign this statement.");
        assertThat(result.getFullNameLegalRep()).isEqualTo("Jane Doe");
        assertThat(result.getFirmNameLegalRep()).isEqualTo("Smith & Co Solicitors");
        assertThat(result.getPositionLegalRep()).isEqualTo("Partner");
        // Claimant fields should be null
        assertThat(result.getAgreementClaimant()).isNull();
        assertThat(result.getFullNameClaimant()).isNull();
        assertThat(result.getPositionClaimant()).isNull();
    }

}

