package uk.gov.hmcts.reform.pcs.ccd.event;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;
import static uk.gov.hmcts.ccd.sdk.api.Permission.D;

@ExtendWith(MockitoExtension.class)
class UploadDocumentPocTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    private Event<PCSCase, UserRole, State> configuredEvent;

    @BeforeEach
    void setUp() {
        UploadDocumentPoc underTest = new UploadDocumentPoc(pcsCaseService);
        configuredEvent = getEvent(EventId.uploadDocumentPoc, buildEventConfig(underTest));
    }

    @Test
    void shouldSetEventPermissions() {
        SetMultimap<UserRole, Permission> grants = configuredEvent.getGrants();
        assertThat(grants.keySet()).hasSize(1);
        assertThat(grants.get(UserRole.PCS_CASE_WORKER)).contains(C, R, U, D);
    }

    @Test
    void shouldInitializeCaseDataOnStart() {
        long caseReference = 1234L;
        PCSCase caseData = PCSCase.builder().build();
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        Start<PCSCase, State> startHandler = configuredEvent.getStartHandler();
        PCSCase result = startHandler.start(eventPayload);

        assertThat(result.getApplicantForename()).isEqualTo("Enter your name");
    }

    @Test
    void shouldCreateCaseOnSubmit() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        verify(pcsCaseService).createCase(caseReference, caseData);
    }
}
