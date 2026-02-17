package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant.BELIEVE_TRUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementLegalRep.AGREED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE;

@ExtendWith(MockitoExtension.class)
class StatementOfTruthViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock(strictness = LENIENT)
    private ClaimEntity mainClaimEntity;
    @Mock
    private StatementOfTruthEntity statementOfTruthEntity;

    private StatementOfTruthView underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getStatementOfTruth()).thenReturn(statementOfTruthEntity);

        underTest = new StatementOfTruthView();
    }

    @Test
    void shouldNotSetAnythingIfNoMainClaim() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldNotSetAnythingIfNoStatementOfTruth() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getStatementOfTruth()).thenReturn(null);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldSetStatementOfTruthFieldsForClaimantAgreement() {
        // Given
        String positionHeld = "position held";
        String fullName = "some full name";

        when(statementOfTruthEntity.getAccepted()).thenReturn(YesOrNo.YES);
        when(statementOfTruthEntity.getCompletedBy()).thenReturn(CLAIMANT);
        when(statementOfTruthEntity.getFullName()).thenReturn(fullName);
        when(statementOfTruthEntity.getPositionHeld()).thenReturn(positionHeld);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<StatementOfTruthDetails> statementOfTruthCaptor
            = ArgumentCaptor.forClass(StatementOfTruthDetails.class);

        verify(pcsCase).setStatementOfTruth(statementOfTruthCaptor.capture());

        StatementOfTruthDetails statementOfTruthDetails = statementOfTruthCaptor.getValue();
        assertThat(statementOfTruthDetails.getCompletedBy()).isEqualTo(CLAIMANT);

        assertThat(statementOfTruthDetails.getAgreementClaimant()).containsExactly(BELIEVE_TRUE);
        assertThat(statementOfTruthDetails.getFullNameClaimant()).isEqualTo(fullName);
        assertThat(statementOfTruthDetails.getPositionClaimant()).isEqualTo(positionHeld);

        assertThat(statementOfTruthDetails.getAgreementLegalRep()).isNull();
        assertThat(statementOfTruthDetails.getFirmNameLegalRep()).isNull();
        assertThat(statementOfTruthDetails.getFullNameLegalRep()).isNull();
    }

    @Test
    void shouldSetStatementOfTruthFieldsForLegalRepAgreement() {
        // Given
        String positionHeld = "position held";
        String fullName = "some full name";
        String firmName = "some firm name";

        when(statementOfTruthEntity.getAccepted()).thenReturn(YesOrNo.YES);
        when(statementOfTruthEntity.getCompletedBy()).thenReturn(LEGAL_REPRESENTATIVE);
        when(statementOfTruthEntity.getFullName()).thenReturn(fullName);
        when(statementOfTruthEntity.getFirmName()).thenReturn(firmName);
        when(statementOfTruthEntity.getPositionHeld()).thenReturn(positionHeld);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<StatementOfTruthDetails> statementOfTruthCaptor
            = ArgumentCaptor.forClass(StatementOfTruthDetails.class);

        verify(pcsCase).setStatementOfTruth(statementOfTruthCaptor.capture());

        StatementOfTruthDetails statementOfTruthDetails = statementOfTruthCaptor.getValue();
        assertThat(statementOfTruthDetails.getCompletedBy()).isEqualTo(LEGAL_REPRESENTATIVE);

        assertThat(statementOfTruthDetails.getAgreementClaimant()).isNull();
        assertThat(statementOfTruthDetails.getFullNameClaimant()).isNull();
        assertThat(statementOfTruthDetails.getPositionClaimant()).isNull();

        assertThat(statementOfTruthDetails.getAgreementLegalRep()).containsExactly(AGREED);
        assertThat(statementOfTruthDetails.getFullNameLegalRep()).isEqualTo(fullName);
        assertThat(statementOfTruthDetails.getFirmNameLegalRep()).isEqualTo(firmName);
    }
}
