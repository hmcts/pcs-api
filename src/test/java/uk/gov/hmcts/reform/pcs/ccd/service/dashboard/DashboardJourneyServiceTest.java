package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DashboardJourneyServiceTest {

    private static final long CASE_REFERENCE = 99_887_766L;

    private DashboardJourneyService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DashboardJourneyService();
    }

    @Test
    void shouldPopulateCaseMetadataForCaseIssued() {
        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 High Street").postCode("SW1A 1AA").build();
        PCSCase submitted = PCSCase.builder().propertyAddress(propertyAddress).build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted, State.CASE_ISSUED);

        assertThat(result.getCaseId()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(result.getPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(result.getNotifications()).hasSize(2);
        assertThat(result.getTaskGroups()).hasSize(2);
    }

    @Test
    void shouldReturnCaseIssuedNotificationsWithExpectedTemplatesAndValues() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted, State.CASE_ISSUED);

        assertThat(ListValueUtils.unwrapListItems(result.getNotifications()))
            .extracting(n -> n.getTemplateId(), n -> n.getTemplateValues().size())
            .containsExactly(
                tuple("Defendant.CaseIssued", 2),
                tuple("Defendant.ResponseToClaim", 1)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getNotifications()).getFirst().getTemplateValues())
            .extracting(lv -> lv.getValue().getKey(), lv -> lv.getValue().getValue())
            .contains(
                tuple("hearingDateTime", "2026-06-15T10:30:00Z"),
                tuple("responseEndDate", "2026-05-15")
            );
    }

    @Test
    void shouldReturnCaseIssuedTaskGroupsWithExpectedStructure() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted, State.CASE_ISSUED);

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()))
            .extracting(g -> g.getGroupId(), g -> g.getTasks().size())
            .containsExactly(
                tuple("CLAIM", 2),
                tuple("RESPONSE", 1)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).getFirst().getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple("Defendant.ViewClaim", "AVAILABLE"),
                tuple("Defendant.ViewDocuments", "NOT_AVAILABLE")
            );
    }

    @Test
    void shouldReturnAwaitingSubmissionNotificationsAndTasks() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(
            CASE_REFERENCE,
            submitted,
            State.AWAITING_SUBMISSION_TO_HMCTS
        );

        assertThat(ListValueUtils.unwrapListItems(result.getNotifications()))
            .extracting(n -> n.getTemplateId())
            .containsExactly("Defendant.AwaitingSubmission");

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()))
            .extracting(g -> g.getGroupId())
            .containsExactly("CLAIM");

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).getFirst().getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(tuple("Task.PCS.Claim.ViewClaim", "NOT_AVAILABLE"));
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names = {"PENDING_CASE_ISSUED"})
    void shouldReturnEmptyNotificationsAndTaskGroupsForUnsupportedStates(State state) {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted, state);

        assertThat(result.getNotifications()).isEmpty();
        assertThat(result.getTaskGroups()).isEmpty();
    }

    @Test
    void shouldOnlyExposeDeclaredPlaceholdersForResponseToClaimNotification() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted, State.CASE_ISSUED);

        List<String> keysForResponseNotification = ListValueUtils.unwrapListItems(result.getNotifications()).stream()
            .filter(n -> "Defendant.ResponseToClaim".equals(n.getTemplateId()))
            .findFirst()
            .orElseThrow()
            .getTemplateValues()
            .stream()
            .map(lv -> lv.getValue().getKey())
            .toList();

        assertThat(keysForResponseNotification).containsExactly("ctaLabel");
    }
}
