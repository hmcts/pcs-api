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
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartEventHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private CitizenCaseDraftLoader citizenCaseDraftLoader;
    @Mock
    private LegalRepresentativeCaseDraftLoader legalRepresentativeCaseDraftLoader;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;
    @Mock
    private UserInfo userInfo;

    private StartEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new StartEventHandler(
            securityContextService,
            citizenCaseDraftLoader,
            legalRepresentativeCaseDraftLoader
        );
    }

    @Test
    void shouldLoadCitizenDraftWhenUserHasCitizenRole() {
        // Given
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(citizenCaseDraftLoader).loadDraft(CASE_REFERENCE, eventPayload.caseData());
        verify(legalRepresentativeCaseDraftLoader, never()).loadDraft(CASE_REFERENCE, eventPayload.caseData());
    }

    @Test
    void shouldLoadLegalRepresentativeDraftWhenUserHasNoCitizenRole() {
        // Given
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(legalRepresentativeCaseDraftLoader).loadDraft(CASE_REFERENCE, eventPayload.caseData());
        verify(citizenCaseDraftLoader, never()).loadDraft(CASE_REFERENCE, eventPayload.caseData());
    }

    private EventPayload<PCSCase, State> createEventPayload() {
        PCSCase caseData = PCSCase.builder().build();
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        return eventPayload;
    }


}
