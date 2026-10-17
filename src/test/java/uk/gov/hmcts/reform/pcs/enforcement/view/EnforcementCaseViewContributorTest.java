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

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class EnforcementCaseViewContributorTest {

    private static final long CASE_REFERENCE = 1234L;

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
    private EnforcementDocumentVisibility enforcementDocumentVisibility;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private PCSCase pcsCase;

    @InjectMocks
    private EnforcementCaseViewContributor underTest;

    @Test
    void shouldHandleEnforcementRequirementsThenSetCaseFieldsAndStatusThenHideDocumentsFromDefendants() {
        // When
        underTest.contribute(pcsCaseEntity, pcsCase, CASE_REFERENCE, State.CASE_ISSUED);

        // Then
        InOrder inOrder = inOrder(enforcementOrderMediator, enforcementOrderView, bailiffNoteView, riskLevelView,
                                  evictionScheduleView, warrantReturnView, enforcementDocumentVisibility);
        inOrder.verify(enforcementOrderMediator).handleEnforcementRequirements(pcsCaseEntity, pcsCase);
        inOrder.verify(enforcementOrderView).setCaseFields(pcsCase, CASE_REFERENCE, State.CASE_ISSUED);
        inOrder.verify(enforcementOrderView).setEnforcementStatusFields(pcsCaseEntity, pcsCase);
        inOrder.verify(bailiffNoteView).setBailiffNotes(pcsCaseEntity, pcsCase);
        inOrder.verify(riskLevelView).setRiskAssessmentFields(pcsCaseEntity, pcsCase);
        inOrder.verify(evictionScheduleView).setEvictionScheduleFields(pcsCaseEntity, pcsCase);
        inOrder.verify(warrantReturnView).setLatestWarrantReturn(pcsCaseEntity, pcsCase);
        inOrder.verify(enforcementDocumentVisibility).hideFromDefendants(pcsCase, CASE_REFERENCE);
        inOrder.verifyNoMoreInteractions();
    }

}
