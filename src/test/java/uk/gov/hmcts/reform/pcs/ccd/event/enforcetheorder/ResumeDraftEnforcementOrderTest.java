package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.WarrantPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution.WarrantOfRestitutionPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ.WritPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumeEnforcementOrder;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_4;

@ExtendWith(MockitoExtension.class)
class ResumeDraftEnforcementOrderTest extends BaseEventTest {

    @Mock
    private EnforcementPageConfigurer enforcementPageConfigurer;
    @Mock
    private WarrantPageConfigurer warrantPageConfigurer;
    @Mock
    private WritPageConfigurer writPageConfigurer;
    @Mock
    private WarrantOfRestitutionPageConfigurer warrantOfRestitutionPageConfigurer;
    @Mock
    private SavingPageBuilderFactory savingPageBuilderFactory;
    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Mock
    private SavingPageBuilder savingPageBuilder;

    @InjectMocks
    private ResumeDraftEnforcementOrder resumeDraftEnforcementOrder;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(savingPageBuilderFactory.create(any(EventBuilder.class), eq(resumeEnforcementOrder)))
            .thenReturn(savingPageBuilder);
        setEventUnderTest(resumeDraftEnforcementOrder);
    }

    @Test
    void shouldConfigurePages() {
        verify(enforcementPageConfigurer).configurePages(savingPageBuilder);
        verify(warrantPageConfigurer).configurePages(savingPageBuilder);
        verify(writPageConfigurer).configurePages(savingPageBuilder);
        verify(warrantOfRestitutionPageConfigurer).configurePages(savingPageBuilder);
    }

    @Test
    void shouldBeConfiguredForCaseIssuedState() {
        assertConfiguredForStates(State.CASE_ISSUED);
    }

    @Test
    void shouldBeConfiguredWithExpectedName() {
        assertThat(configuredEvent.getName()).isEqualTo("Resume Draft Application");
    }

    @Test
    void shouldBeConfiguredWithFeatureFlagAndUnsubmittedEnforcementDataShowCondition() {
        assertConfiguredShowConditions(ShowConditions.and(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_4),
            "hasUnsubmittedEnforcementData=\"Yes\""));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "PCS_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "DEFENDANT_SOLICITOR", "GA_DEFENDANT_SOLICITOR"
    })
    void shouldGrantCrudToSolicitorRoles(UserRole userRole) {
        assertGrants(userRole, Permission.CRUD);
    }

    @Test
    void shouldGrantHistoryOnlyAccessToJudicialHistoryRoles() {
        for (UserRole role : JUDICIAL_HISTORY_ROLES) {
            assertThat(configuredEvent.getGrants().get(role))
                .as("Grants for role %s", role)
                .contains(Permission.R);
        }
    }

    @Test
    void shouldSaveAndClearDraftDataOnSubmit() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
        PCSCase caseData = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        verify(enforcementOrderService).saveAndClearDraftData(TEST_CASE_REFERENCE, enforcementOrder);
        verifyNoMoreInteractions(enforcementOrderService);
        assertThat(response).isEqualTo(SubmitResponse.defaultResponse());
    }
}
