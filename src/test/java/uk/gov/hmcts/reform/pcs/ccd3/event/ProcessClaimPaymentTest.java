package uk.gov.hmcts.reform.pcs.ccd3.event;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd3.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd3.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.domain.State;
import uk.gov.hmcts.reform.pcs.ccd3.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;


@ExtendWith(MockitoExtension.class)
public class ProcessClaimPaymentTest extends BaseEventTest {

    @Mock
    private PostCodeCourtService postCodeCourtService;
    @Mock
    private PcsCaseService pcsCaseService;

    private Event<PCSCase, UserRole, State> configuredEvent;

    @BeforeEach
    void setUp() {
        ProcessClaimPayment underTest = new ProcessClaimPayment(postCodeCourtService,pcsCaseService);
        configuredEvent = getEvent(EventId.processClaimPayment, buildEventConfig(underTest));
    }

    @Test
    void shouldSetEventPermissions() {
        SetMultimap<UserRole, Permission> grants = configuredEvent.getGrants();
        assertThat(grants.keySet()).hasSize(1);
        assertThat(grants.get(UserRole.PCS_CASE_WORKER)).contains(C, R, U);

    }

    @Test
    void shouldNotShowEvent() {
        assertThat(configuredEvent.getShowCondition()).isEqualTo(ShowConditions.NEVER_SHOW);
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);

        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(propertyAddress.getPostCode()).thenReturn("M37 5SF");

        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        verify(pcsCaseService).patchCase(caseReference, caseData);
    }

}
