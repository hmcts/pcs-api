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
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy.CitizenSubmissionEventStrategy;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy.LegalRepSubmissionEventStrategy;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitEventHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private EventPayload<PCSCase, State> eventPayload;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private UserInfo userInfo;
    @Mock
    private LegalRepSubmissionEventStrategy legalRepSubmissionEventStrategy;
    @Mock
    private CitizenSubmissionEventStrategy citizenSubmissionEventStrategy;

    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitEventHandler(
            List.of(legalRepSubmissionEventStrategy, citizenSubmissionEventStrategy),
            securityContextService
        );
    }

    @Test
    void shouldLoadCitizenStrategyForCitizenUser() {
        // given
        EventPayload<PCSCase, State> eventPayload = createEventPayload();
        when(citizenSubmissionEventStrategy.supports(true)).thenReturn(true);
        when(legalRepSubmissionEventStrategy.supports(true)).thenReturn(false);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));

        // when
        underTest.submit(eventPayload);

        // then
        verify(citizenSubmissionEventStrategy).process(CASE_REFERENCE);
        verify(legalRepSubmissionEventStrategy, never()).process(CASE_REFERENCE);
    }

    @Test
    void shouldLoadLegalRepStrategyForNoCitizenUser() {
        // given
        EventPayload<PCSCase, State> eventPayload = createEventPayload();
        when(legalRepSubmissionEventStrategy.supports(false)).thenReturn(true);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));

        // when
        underTest.submit(eventPayload);

        // then
        verify(citizenSubmissionEventStrategy, never()).process(CASE_REFERENCE);
        verify(legalRepSubmissionEventStrategy).process(CASE_REFERENCE);
    }

    @Test
    void shouldExceptionWhenNoStrategyApplies() {
        // given
        EventPayload<PCSCase, State> eventPayload = createEventPayload();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));

        // when
        assertThat(assertThrows(
            IllegalStateException.class,
            () -> underTest.submit(eventPayload)
        )).hasMessage("No submit event strategy found");

        // then
        verify(citizenSubmissionEventStrategy, never()).process(CASE_REFERENCE);
        verify(legalRepSubmissionEventStrategy, never()).process(CASE_REFERENCE);
    }

    private EventPayload<PCSCase, State> createEventPayload() {
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        return eventPayload;
    }
}
