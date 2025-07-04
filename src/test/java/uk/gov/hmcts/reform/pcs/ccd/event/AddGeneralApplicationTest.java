package uk.gov.hmcts.reform.pcs.ccd.event;

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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.GAType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.GACaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.D;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;

@ExtendWith(MockitoExtension.class)
class AddGeneralApplicationTest extends PCSBaseEventTest {

    @Mock
    private GeneralApplicationService genAppService;

    @Mock
    private PCSCaseService pcsCaseService;

    private Event<PCSCase, UserRole, State> configuredEvent;

    @BeforeEach
    void setUp() {
        AddGeneralApplication underTest = new AddGeneralApplication(genAppService, pcsCaseService);
        configuredEvent = getEvent(EventId.addGeneralApplication, buildEventConfig(underTest));
    }

    @Test
    void shouldSetEventPermissions() {
        SetMultimap<UserRole, Permission> grants = configuredEvent.getGrants();
        assertThat(grants.keySet()).contains(UserRole.PCS_CASE_WORKER);
        assertThat(grants.get(UserRole.PCS_CASE_WORKER)).contains(C,R,U,D);
    }

    @Test
    void shouldHaveCorrectNameAndStates() {
        assertThat(configuredEvent.getName()).isEqualTo("Make General Application");
        assertThat(configuredEvent.getPreState()).containsExactly(State.CASE_ISSUED);
        assertThat(configuredEvent.isShowSummary()).isTrue();
    }

    @Test
    void shouldAddGeneralApplicationOnSubmit() {
        long caseReference = 1234L;
        GACase currentGA = GACase.builder()
            .gaType(GAType.ADJOURN_HEARING)
            .adjustment("Test Adjustment")
            .additionalInformation("Extra info")
            .build();

        PCSCase caseData = PCSCase.builder()
            .currentGeneralApplication(currentGA)
            .build();

        final EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        Long gaCaseReference = 5678L;
        GACaseEntity gaEntity = new GACaseEntity();
        when(genAppService
                 .createGeneralApplicationInCCD(any(GACase.class), eq(EventId.createGeneralApplication.name())))
            .thenReturn(gaCaseReference);
        when(genAppService.findByCaseReference(gaCaseReference)).thenReturn(gaEntity);

        PcsCaseEntity parentCaseEntity = new PcsCaseEntity();
        when(pcsCaseService.findPCSCase(caseReference)).thenReturn(parentCaseEntity);

        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        verify(genAppService)
            .createGeneralApplicationInCCD(any(GACase.class), eq(EventId.createGeneralApplication.name()));
        verify(genAppService).findByCaseReference(gaCaseReference);
        verify(pcsCaseService).findPCSCase(caseReference);
        verify(pcsCaseService).savePCSCase(parentCaseEntity);

        assertThat(parentCaseEntity.getGeneralApplications()).contains(gaEntity);

        assertThat(caseData.getCurrentGeneralApplication()).isNull();
        assertThat(currentGA.getCaseReference()).isEqualTo(gaCaseReference);
    }
}

