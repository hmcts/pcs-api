package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.StatementOfTruth;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementOfTruthServiceTest {

    private final StatementOfTruthService statementOfTruthService = new StatementOfTruthService();

    @Mock
    private PCSCase pcsCase;

    @Mock
    private StatementOfTruthDetails statementOfTruthDetails;

    @Test
    void shouldReturnNullWhenStatementOfTruthIsNull() {
        // Given
        when(pcsCase.getStatementOfTruth()).thenReturn(null);

        // When
        StatementOfTruth result = statementOfTruthService.buildStatementOfTruth(pcsCase);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenStatementOfTruthCompletedByIsNull() {
        // Given
        when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
        when(statementOfTruthDetails.getCompletedBy()).thenReturn(null);

        // When
        StatementOfTruth result = statementOfTruthService.buildStatementOfTruth(pcsCase);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldBuildStatementOfTruthForClaimant() {
        // Given
        when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
        when(statementOfTruthDetails.getCompletedBy()).thenReturn(StatementOfTruthCompletedBy.CLAIMANT);
        when(statementOfTruthDetails.getAgreementClaimant())
            .thenReturn(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE));
        when(statementOfTruthDetails.getFullNameClaimant()).thenReturn("John Smith");
        when(statementOfTruthDetails.getPositionClaimant()).thenReturn("Director");

        // When
        StatementOfTruth result = statementOfTruthService.buildStatementOfTruth(pcsCase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.CLAIMANT);
        assertThat(result.getAgreementClaimant()).isEqualTo(StatementOfTruthAgreementClaimant.BELIEVE_TRUE);
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
        when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
        when(statementOfTruthDetails.getCompletedBy())
            .thenReturn(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
        when(statementOfTruthDetails.getAgreementLegalRep())
            .thenReturn(List.of(StatementOfTruthAgreementLegalRep.AGREED));
        when(statementOfTruthDetails.getFullNameLegalRep()).thenReturn("Jane Doe");
        when(statementOfTruthDetails.getFirmNameLegalRep()).thenReturn("Smith & Co Solicitors");
        when(statementOfTruthDetails.getPositionLegalRep()).thenReturn("Partner");

        // When
        StatementOfTruth result = statementOfTruthService.buildStatementOfTruth(pcsCase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
        assertThat(result.getAgreementLegalRep()).isEqualTo(StatementOfTruthAgreementLegalRep.AGREED);
        assertThat(result.getFullNameLegalRep()).isEqualTo("Jane Doe");
        assertThat(result.getFirmNameLegalRep()).isEqualTo("Smith & Co Solicitors");
        assertThat(result.getPositionLegalRep()).isEqualTo("Partner");
        // Claimant fields should be null
        assertThat(result.getAgreementClaimant()).isNull();
        assertThat(result.getFullNameClaimant()).isNull();
        assertThat(result.getPositionClaimant()).isNull();
    }

}

