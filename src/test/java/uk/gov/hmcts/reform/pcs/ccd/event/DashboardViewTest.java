package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.dashboard.StartDashboardViewHandler;
import uk.gov.hmcts.reform.pcs.ccd.event.dashboard.SubmitDashboardViewHandler;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardJourneyService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ClaimTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.HearingsTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.NoticesTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardViewTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private DefendantAccessValidator accessValidator;

    @Mock
    private SecurityContextService securityContextService;

    private DashboardJourneyService dashboardJourneyService;

    @BeforeEach
    void setUp() {
        dashboardJourneyService = new DashboardJourneyService(
            new ClaimTaskGroupEvaluator(),
            new HearingsTaskGroupEvaluator(),
            new NoticesTaskGroupEvaluator()
        );
        StartDashboardViewHandler startHandler = new StartDashboardViewHandler(
            pcsCaseService,
            accessValidator,
            securityContextService,
            dashboardJourneyService
        );
        SubmitDashboardViewHandler submitHandler = new SubmitDashboardViewHandler();
        setEventUnderTest(new DashboardView(startHandler, submitHandler));
    }

    @Test
    void shouldExposeConfiguredStartHandlerThatPopulatesDashboardFromJourneyService() {
        UUID defendantUserId = UUID.randomUUID();
        AddressUK propertyAddress = AddressUK.builder().addressLine1("2 River Lane").build();
        PCSCase caseData = PCSCase.builder().propertyAddress(propertyAddress).build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(caseEntity);
        when(accessValidator.validateAndGetDefendant(caseEntity, defendantUserId))
            .thenReturn(PartyEntity.builder().idamId(defendantUserId).build());

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getDashboardData()).isNotNull();
        assertThat(result.getDashboardData().getCaseId()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE));
        assertThat(result.getDashboardData().getPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(ListValueUtils.unwrapListItems(result.getDashboardData().getNotifications()))
            .extracting(n -> n.getTemplateId())
            .containsExactly("Defendant.CaseIssued", "Defendant.ResponseToClaim");
        verify(pcsCaseService).loadCase(TEST_CASE_REFERENCE);
        verify(accessValidator).validateAndGetDefendant(eq(caseEntity), eq(defendantUserId));
    }

    @Test
    void shouldExposeConfiguredSubmitHandlerThatReturnsDefaultResponse() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getState()).isNull();
    }
}
