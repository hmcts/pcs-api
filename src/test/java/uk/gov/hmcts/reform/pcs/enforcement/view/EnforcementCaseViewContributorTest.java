package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementCaseViewContributorTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private EnforcementOrderMediator enforcementOrderMediator;
    @Mock
    private EnforcementOrderView enforcementOrderView;
    @Mock
    private BailiffNoteView bailiffNoteView;
    @Mock
    private RiskLevelView riskLevelView;
    @Mock
    private EvictionScheduleView evictionScheduleView;
    @Mock
    private WarrantReturnView warrantReturnView;
    @Mock
    private EnforcementStatusTabView enforcementStatusTabView;
    @Mock
    private RiskAssessmentTabView riskAssessmentTabView;
    @Mock
    private ClaimantEnforcementDocumentView claimantEnforcementDocumentView;
    @Mock
    private DefendantEnforcementView defendantEnforcementView;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private PCSCase pcsCase;

    @InjectMocks
    private EnforcementCaseViewContributor underTest;

    @Test
    void shouldSetTheEnforcementFieldsThenRestrictThemForClaimantsAndDefendants() {
        // Given
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);

        // When
        underTest.contribute(pcsCaseEntity, pcsCase, CASE_REFERENCE, State.CASE_ISSUED);

        // Then
        InOrder inOrder = inOrder(enforcementOrderMediator, enforcementOrderView, bailiffNoteView, riskLevelView,
                                  evictionScheduleView, warrantReturnView, enforcementStatusTabView,
                                  riskAssessmentTabView, claimantEnforcementDocumentView,
                                  defendantEnforcementView);
        inOrder.verify(enforcementOrderMediator).handleEnforcementRequirements(pcsCaseEntity, pcsCase);
        inOrder.verify(enforcementOrderView).setCaseFields(pcsCase, CASE_REFERENCE, State.CASE_ISSUED);
        inOrder.verify(enforcementOrderView).setEnforcementStatusFields(pcsCaseEntity, pcsCase);
        inOrder.verify(bailiffNoteView).setBailiffNotes(pcsCaseEntity, pcsCase);
        inOrder.verify(riskLevelView).setRiskAssessmentFields(pcsCaseEntity, pcsCase);
        inOrder.verify(evictionScheduleView).setEvictionScheduleFields(pcsCaseEntity, pcsCase);
        inOrder.verify(warrantReturnView).setLatestWarrantReturn(pcsCaseEntity, pcsCase);
        inOrder.verify(enforcementStatusTabView).setEnforcementStatusTab(pcsCaseEntity, pcsCase);
        inOrder.verify(riskAssessmentTabView).setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);
        inOrder.verify(claimantEnforcementDocumentView)
            .restrictForClaimants(pcsCaseEntity, pcsCase, CASE_REFERENCE);
        inOrder.verify(defendantEnforcementView).restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldSetNoEnforcementFieldWhenEnforcementIsNotEnabled() {
        // Given
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(false);
        PCSCase caseView = PCSCase.builder().enforcementOrder(new EnforcementOrder()).build();

        // When
        underTest.contribute(pcsCaseEntity, caseView, CASE_REFERENCE, State.CASE_ISSUED);

        // Then
        assertThat(caseView.getEnforcementOrder()).isNull();
        verifyNoInteractions(enforcementOrderMediator, enforcementOrderView, bailiffNoteView, riskLevelView,
                             evictionScheduleView, warrantReturnView, enforcementStatusTabView,
                             riskAssessmentTabView);
        verify(claimantEnforcementDocumentView).restrictForClaimants(pcsCaseEntity, caseView, CASE_REFERENCE);
        verify(defendantEnforcementView).restrictForDefendants(pcsCaseEntity, caseView, CASE_REFERENCE);
    }

}
