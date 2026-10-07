package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseNoteRoles;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.addcasenote.AddCaseNoteConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNoteService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddCaseNoteTest extends BaseEventTest {

    @Mock
    private CaseNoteService caseNoteService;

    @Mock
    private AddCaseNoteConfigurer addCaseNoteConfigurer;

    @InjectMocks
    private AddCaseNote addCaseNote;

    @BeforeEach
    void setUp() {
        setEventUnderTest(addCaseNote);
    }

    @Test
    void shouldBeConfiguredForEventStates() {
        assertConfiguredForStates(EventStates.addCaseNote());
    }

    @Test
    void shouldBeConfiguredForClosedState() {
        assertThat(configuredEvent.getPreState()).contains(State.CLOSED);
    }

    @Test
    void shouldGrantEventAccessToCaseNoteRolesOnly() {
        for (UserRole role : CaseNoteRoles.CASE_NOTE_ROLES) {
            assertThat(configuredEvent.getGrants().get(role))
                .containsExactlyInAnyOrder(Permission.C, Permission.R, Permission.U, Permission.D);
        }

        assertThat(configuredEvent.getGrants().get(UserRole.PCS_SOLICITOR)).isEmpty();
        assertThat(configuredEvent.getGrants().get(UserRole.CLAIMANT)).isEmpty();
        assertThat(configuredEvent.getGrants().get(UserRole.GA_CLAIMANT_SOLICITOR)).isEmpty();
        assertThat(configuredEvent.getGrants().get(UserRole.DEFENDANT)).isEmpty();
        assertThat(configuredEvent.getGrants().get(UserRole.GA_DEFENDANT_SOLICITOR)).isEmpty();
    }

    @Test
    void shouldConfigurePages() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(addCaseNoteConfigurer).configurePages(any(PageBuilder.class));
    }

    @Test
    void shouldCallCaseNoteServiceOnSubmit() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(caseNoteService).addCaseNote(TEST_CASE_REFERENCE, pcsCase);
    }
}
