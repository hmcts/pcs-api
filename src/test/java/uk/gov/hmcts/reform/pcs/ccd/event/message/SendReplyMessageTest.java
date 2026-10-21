package uk.gov.hmcts.reform.pcs.ccd.event.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageAction;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.message.MessageService;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendReplyMessageTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private MessageService messageService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private TextAreaValidationService textAreaValidationService;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private SendReplyMessage underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }

    @Test
    void shouldOnlyShowEventWhenMakeOrderIsEnabled() {
        assertConfiguredShowConditions("featureFlags.makeOrderEnabled=\"YES\"");
    }

    @Test
    void shouldRefuseToStartWhenMakeOrderIsDisabled() {
        when(featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)).thenReturn(false);

        assertThatThrownBy(() -> callStartHandler(PCSCase.builder().build()))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage(SendReplyMessage.NOT_AVAILABLE);
        verifyNoInteractions(pcsCaseService, messageService);
    }

    @Test
    void shouldRejectSubmitWhenMakeOrderIsDisabled() {
        when(featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)).thenReturn(false);

        SubmitResponse<State> response = callSubmitHandler(
            PCSCase.builder().messageAction(MessageAction.SEND).build());

        assertThat(response.getErrors()).containsExactly(SendReplyMessage.NOT_AVAILABLE);
        verifyNoInteractions(pcsCaseService, messageService);
    }

    @Test
    void shouldStartWhenMakeOrderIsEnabled() {
        when(featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)).thenReturn(true);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder().build());
        when(userRoleService.isJudge()).thenReturn(true);

        PCSCase started = callStartHandler(PCSCase.builder().build());

        assertThat(started.getMessageGenAppList().getListItems()).isEmpty();
        assertThat(started.getMessageReplyList().getListItems()).isEmpty();
    }
}
