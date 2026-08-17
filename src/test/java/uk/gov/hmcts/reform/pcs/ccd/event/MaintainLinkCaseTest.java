package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.CaseLink;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@ExtendWith(MockitoExtension.class)
class MaintainLinkCaseTest extends BaseEventTest {


    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private MaintainLinkCase underTest;

    @BeforeEach
    void setUp() {

        when(featureToggleService.isEnabled(RELEASE_1_DOT_3)).thenReturn(true);
        setEventUnderTest(underTest);
    }

    @Test
    void shouldBeConfiguredForEventStates() {
        assertConfiguredForStates(EventStates.maintainCaseLink());
    }

    @Test
    void shouldBeConfiguredForPreRelease1dot3EventStatesWhenFeatureFlagDisabled() {
        when(featureToggleService.isEnabled(RELEASE_1_DOT_3)).thenReturn(false);
        setEventUnderTest(underTest);

        assertConfiguredForStates(State.PENDING_CASE_ISSUED, State.CASE_ISSUED);
    }

    @Test
    void shouldModifyCaseLinksInSubmitCallback() {
        // Given
        CaseLink caseLink = CaseLink.builder().build();
        List<ListValue<CaseLink>> caseLists = List.of(
            ListValue.<CaseLink>builder()
                .id(UUID.randomUUID().toString())
                .value(caseLink)
                .build());

        PCSCase pcsCase = PCSCase.builder().caseLinks(caseLists).build();
        doNothing().when(pcsCaseService).patchCaseLinks(TEST_CASE_REFERENCE, pcsCase);

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseService).patchCaseLinks(TEST_CASE_REFERENCE, pcsCase);
    }
}
