package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.WelshDynamicListInterpolationPoc;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.model.RoleAssignmentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.task.CaseRoleAssignmentTaskComponent;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreatePossessionClaimTest extends BaseEventTest {

    private static final String USER_ID = UUID.randomUUID().toString();

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private FeeApplier feeApplier;
    @Mock
    private EnterPropertyAddress enterPropertyAddress;
    @Mock
    private CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    @Mock
    private PropertyNotEligible propertyNotEligible;
    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private SecurityContextService securityContextService;

    @BeforeEach
    void setUp() {
        UserInfo userDetails = mock(UserInfo.class);
        lenient().when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        lenient().when(userDetails.getUid()).thenReturn(USER_ID);

        CreatePossessionClaim underTest = new CreatePossessionClaim(
            pcsCaseService, feeApplier, enterPropertyAddress,
            crossBorderPostcodeSelection, propertyNotEligible,
            schedulerClient, securityContextService
        );

        setEventUnderTest(underTest);
    }

    @Test
    void shouldCreateCaseFromTheAddressAndCountryEntered() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 Test Street").build();
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        // When
        callSubmitHandler(caseData);

        // Then
        verify(pcsCaseService).createCase(TEST_CASE_REFERENCE, propertyAddress,
                                          LegislativeCountry.ENGLAND);
    }

    @Test
    void shouldScheduleCreatorRoleRevocationOnSubmit() {
        // When
        callSubmitHandler(PCSCase.builder().build());

        // Then
        RoleAssignmentTaskData taskData = getCapturedRoleAssignmentTaskData();
        assertThat(taskData.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE));
        assertThat(taskData.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void shouldSetWelshDynamicListInterpolationPocOptionsOnStart() {
        // When
        PCSCase result = callStartHandler(PCSCase.builder().build());

        // Then
        assertThat(result.getWelshPocGaRank()).isEqualTo("3");
        assertThat(result.getWelshPocDynamicRadioList().getListItems())
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly(WelshDynamicListInterpolationPoc.TEMPLATE, WelshDynamicListInterpolationPoc.RESOLVED);
    }

    @SuppressWarnings("unchecked")
    private RoleAssignmentTaskData getCapturedRoleAssignmentTaskData() {
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        return captor.getAllValues().stream()
            .filter(t -> t.getTaskInstance().getTaskName()
                .equals(CaseRoleAssignmentTaskComponent.ROLE_ASSIGNMENT_TASK_DESCRIPTOR.getTaskName()))
            .map(SchedulableInstance::getTaskInstance)
            .map(TaskInstance::getData)
            .map(RoleAssignmentTaskData.class::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No role assignment task found"));
    }
}
