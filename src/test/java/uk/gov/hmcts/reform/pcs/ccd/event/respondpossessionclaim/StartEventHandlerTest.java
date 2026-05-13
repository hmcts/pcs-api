package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy.CitizenStartEventStrategy;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy.LegalRepStartEventStrategy;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartEventHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private EventPayload<PCSCase, State> eventPayload;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private UserInfo userInfo;
    @Mock
    private LegalRepStartEventStrategy legalRepStartEventStrategy;
    @Mock
    private CitizenStartEventStrategy citizenStartEventStrategy;

    private StartEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new StartEventHandler(
            securityContextService,
            List.of(legalRepStartEventStrategy, citizenStartEventStrategy)
        );
    }

    @Test
    void shouldLoadCitizenStrategyForCitizenUser() {
        // given
        List<String> userRoles = List.of(UserRole.CITIZEN.getRole());
        PCSCase pcsCase = PCSCase.builder().build();
        when(citizenStartEventStrategy.supports(userRoles)).thenReturn(true);
        when(legalRepStartEventStrategy.supports(userRoles)).thenReturn(false);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(userRoles);
        EventPayload<PCSCase, State> eventPayload = createEventPayload(pcsCase);

        // when
        underTest.start(eventPayload);

        // then
        verify(citizenStartEventStrategy).loadDraft(CASE_REFERENCE, pcsCase);
        verify(legalRepStartEventStrategy, never()).loadDraft(CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldLoadLegalRepStrategyForNoCitizenUser() {
        // given
        List<String> userRoles = List.of(UserRole.DEFENDANT_SOLICITOR.getRole());
        PCSCase pcsCase = PCSCase.builder().build();
        when(legalRepStartEventStrategy.supports(userRoles)).thenReturn(true);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(userRoles);
        EventPayload<PCSCase, State> eventPayload = createEventPayload(pcsCase);
        // when
        underTest.start(eventPayload);

        // then
        verify(citizenStartEventStrategy, never()).loadDraft(CASE_REFERENCE, pcsCase);
        verify(legalRepStartEventStrategy).loadDraft(CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldExceptionWhenNoStrategyApplies() {
        // given
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));

        // when
        assertThat(assertThrows(
            IllegalStateException.class,
            () -> underTest.start(eventPayload)
        )).hasMessage("No start event strategy found");

        // then
        verify(citizenStartEventStrategy, never()).loadDraft(anyLong(), any());
        verify(legalRepStartEventStrategy, never()).loadDraft(anyLong(), any());
    }

    private EventPayload<PCSCase, State> createEventPayload(PCSCase caseData) {
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        return eventPayload;
    }

}
