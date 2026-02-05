package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthAgreementLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE;

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
        StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenStatementOfTruthCompletedByIsNull() {
        // Given
        when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
        when(statementOfTruthDetails.getCompletedBy()).thenReturn(null);

        // When
        StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldBuildStatementOfTruthForClaimant() {
        // Given
        when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
        when(statementOfTruthDetails.getCompletedBy()).thenReturn(CLAIMANT);
        when(statementOfTruthDetails.getAgreementClaimant())
            .thenReturn(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE));
        when(statementOfTruthDetails.getFullNameClaimant()).thenReturn("John Smith");
        when(statementOfTruthDetails.getPositionClaimant()).thenReturn("Director");

        // When
        StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompletedBy()).isEqualTo(CLAIMANT);
        assertThat(result.getAccepted()).isEqualTo(YesOrNo.YES);
        assertThat(result.getFullName()).isEqualTo("John Smith");
        assertThat(result.getPositionHeld()).isEqualTo("Director");
        assertThat(result.getFirmName()).isNull();
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
        StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
        assertThat(result.getAccepted()).isEqualTo(YesOrNo.YES);
        assertThat(result.getFullName()).isEqualTo("Jane Doe");
        assertThat(result.getFirmName()).isEqualTo("Smith & Co Solicitors");
        assertThat(result.getPositionHeld()).isEqualTo("Partner");
    }

    @Test
    void shouldNotSetAcceptedFlagWhenClaimantDidNotAgree() {
        // Given
        when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
        when(statementOfTruthDetails.getCompletedBy()).thenReturn(CLAIMANT);
        when(statementOfTruthDetails.getAgreementClaimant()).thenReturn(List.of());

        // When
        StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

        // Then
        assertThat(result.getAccepted()).isNull();
    }

    @Test
    void shouldNotSetAcceptedFlagWhenLegalRepDidNotAgree() {
        // Given
        when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
        when(statementOfTruthDetails.getCompletedBy()).thenReturn(LEGAL_REPRESENTATIVE);
        when(statementOfTruthDetails.getAgreementLegalRep()).thenReturn(List.of());

        // When
        StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

        // Then
        assertThat(result.getAccepted()).isNull();
    }
}

