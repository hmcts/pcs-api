package uk.gov.hmcts.reform.pcs.ccd.event;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.GeneralApplicationService;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.D;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;

@ExtendWith(MockitoExtension.class)
class DeleteGeneralApplicationTest extends PCSBaseEventTest {

    @Mock
    private PCSCaseService pcsCaseService;

    @Mock
    private GeneralApplicationService gaService;

    private Event<PCSCase, UserRole, State> configuredEvent;

    private DeleteGeneralApplication underTest;

    @BeforeEach
    void setUp() {
        underTest = new DeleteGeneralApplication(pcsCaseService, gaService);
        configuredEvent = getEvent(EventId.deleteGeneralApplication, buildEventConfig(underTest));
    }

    @Test
    void shouldSetEventPermissions() {
        SetMultimap<UserRole, Permission> grants = configuredEvent.getGrants();
        assertThat(grants.keySet()).hasSize(1);
        assertThat(grants.get(UserRole.PCS_CASE_WORKER)).contains(C, R, U, D);
    }

    @Test
    void shouldNotShowEvent() {
        assertThat(configuredEvent.getShowCondition()).isEqualTo(ShowConditions.NEVER_SHOW);
    }

    @Test
    void shouldDeleteGeneralApplicationOnSubmit() {
        long caseReference = 1234L;
        long gaReference = 5678L;

        PCSCase pcsCase = new PCSCase();
        ListValue<GACase> gaListValue = ListValue.<GACase>builder()
            .id("1")
            .value(GACase.builder().caseReference(gaReference).build())
            .build();

        pcsCase.setCaseReference(caseReference);
        pcsCase.setGeneralApplications(List.of(gaListValue));

        PcsCaseEntity pcsEntity = mock(PcsCaseEntity.class);
        when(pcsCaseService.findPCSCase(caseReference)).thenReturn(pcsEntity);
        MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
        urlParams.add("genAppId", String.valueOf(gaReference));
        EventPayload<PCSCase, State> payload = new EventPayload<>(
            caseReference,
            pcsCase,
            urlParams
        );
        configuredEvent.getSubmitHandler().submit(payload);

        assertThat(pcsCase.getGeneralApplications()).isEmpty();
        verify(pcsCaseService).savePCSCase(pcsEntity);
        verify(gaService).deleteGenApp(gaReference);
    }

}
