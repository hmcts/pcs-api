package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.addcasereviewdate.AddCaseReviewDateConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReviewDateService;
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddCaseReviewDateTest extends BaseEventTest {

    @Mock
    private AddCaseReviewDateConfigurer addCaseReviewDateConfigurer;
    @Mock
    private CaseReviewDateService caseReviewDateService;
    @Mock
    private ConfirmationPanelBuilder confirmationPanelBuilder;

    @InjectMocks
    private AddCaseReviewDate addCaseReviewDate;

    @BeforeEach
    void setUp() {
        setEventUnderTest(addCaseReviewDate);
    }

    @Test
    void shouldConfigurePages() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(addCaseReviewDateConfigurer).configurePages(any(PageBuilder.class));
    }


    @Test
    void shouldConfigureCaseworkerEventAccessAndStates() {
        assertThat(configuredEvent.getPreState()).containsExactlyInAnyOrder(
            State.CASE_ISSUED,
            State.CASE_PROGRESSION,
            State.CASE_STAYED,
            State.BREATHING_SPACE,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME,
            State.ALL_FINAL_ORDERS_ISSUED
        );
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_TEAM_LEADER))
            .containsExactlyInAnyOrder(Permission.C, Permission.R, Permission.U, Permission.D);
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_ADMIN))
            .containsExactlyInAnyOrder(Permission.C, Permission.R, Permission.U, Permission.D);
        assertThat(configuredEvent.getGrants().get(UserRole.CTSC_ADMIN)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.CTSC_TEAM_LEADER)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.CIRCUIT_JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.FEE_PAID_JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.LEADERSHIP_JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.WLU_ADMIN)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.WLU_TEAM_LEADER)).containsExactly(Permission.R);
    }

    @Test
    void shouldCallCaseReviewDateServiceOnSubmit() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(caseReviewDateService).addCaseReviewDates(TEST_CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldReturnConfirmationScreen() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        String confirmationPanelMarkup = stubConfirmationPanelBuilder();

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

        // Then

        verify(confirmationPanelBuilder)
            .buildStandardConfirmationPanel("Review dates added", pcsCase, TEST_CASE_REFERENCE);

        assertThat(submitResponse.getConfirmationBody()).isEqualTo(
            """
            ---
            %s

            <h3>What happens next</h3>

            Work allocation tasks will be created for court staff to complete on the review dates.
            """, confirmationPanelMarkup);

    }

    private String stubConfirmationPanelBuilder() {
        String confirmationPanelMarkup = "confirmation panel markup";
        when(confirmationPanelBuilder
                 .buildStandardConfirmationPanel(anyString(), any(PCSCase.class), anyLong()))
            .thenReturn(confirmationPanelMarkup);

        return confirmationPanelMarkup;
    }
}
