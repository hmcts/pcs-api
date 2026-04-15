package uk.gov.hmcts.reform.pcs.ccd.event.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardJourneyService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartDashboardViewHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private DefendantAccessValidator accessValidator;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private DashboardJourneyService dashboardJourneyService;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;

    private StartDashboardViewHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new StartDashboardViewHandler(
            pcsCaseService,
            accessValidator,
            securityContextService,
            dashboardJourneyService
        );
    }

    @Test
    void shouldComputeDashboardDataAndAttachToCaseData() {
        UUID defendantUserId = UUID.randomUUID();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        AddressUK propertyAddress = AddressUK.builder().addressLine1("10 Test Road").build();
        PCSCase caseData = PCSCase.builder().propertyAddress(propertyAddress).build();
        DashboardData dashboardData = DashboardData.builder()
            .caseId(String.valueOf(CASE_REFERENCE))
            .notifications(List.of())
            .taskGroups(List.of())
            .build();

        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(accessValidator.validateAndGetDefendant(caseEntity, defendantUserId))
            .thenReturn(PartyEntity.builder().idamId(defendantUserId).build());
        when(dashboardJourneyService.computeDashboardData(CASE_REFERENCE, caseData))
            .thenReturn(dashboardData);

        PCSCase result = underTest.start(eventPayload);

        assertThat(result).isSameAs(caseData);
        assertThat(result.getDashboardData()).isSameAs(dashboardData);
        verify(pcsCaseService).loadCase(CASE_REFERENCE);
        verify(accessValidator).validateAndGetDefendant(caseEntity, defendantUserId);
        verify(dashboardJourneyService).computeDashboardData(CASE_REFERENCE, caseData);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caseAccessExceptionScenarios")
    void shouldPropagateCaseAccessException(String scenario, String exceptionMessage) {
        UUID defendantUserId = UUID.randomUUID();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();

        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(accessValidator.validateAndGetDefendant(caseEntity, defendantUserId))
            .thenThrow(new CaseAccessException(exceptionMessage));

        assertThatThrownBy(() -> underTest.start(eventPayload))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage(exceptionMessage);
    }

    private static Stream<Arguments> caseAccessExceptionScenarios() {
        return Stream.of(
            Arguments.of("No claim found", "No claim found for this case"),
            Arguments.of("No defendants found", "No defendants associated with this case"),
            Arguments.of("User not defendant", "User is not linked as a defendant on this case")
        );
    }
}
