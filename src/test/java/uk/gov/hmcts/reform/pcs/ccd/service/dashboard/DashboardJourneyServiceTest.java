package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.SectionStatusEntry;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ClaimTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ResponseTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

class DashboardJourneyServiceTest {

    private static final long CASE_REFERENCE = 99_887_766L;

    private DashboardJourneyService underTest;
    private DraftCaseDataService draftCaseDataService;
    private DefendantResponseRepository defendantResponseRepository;
    private SecurityContextService securityContextService;

    @BeforeEach
    void setUp() {
        draftCaseDataService = mock(DraftCaseDataService.class);
        defendantResponseRepository = mock(DefendantResponseRepository.class);
        securityContextService = mock(SecurityContextService.class);
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.empty());
        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            org.mockito.ArgumentMatchers.eq(CASE_REFERENCE),
            org.mockito.ArgumentMatchers.any(UUID.class)
        )).thenReturn(false);

        underTest = new DashboardJourneyService(
            new ClaimTaskGroupEvaluator(),
            new ResponseTaskGroupEvaluator(draftCaseDataService, defendantResponseRepository, securityContextService)
        );
    }

    @Test
    void shouldPopulateCaseMetadata() {
        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 High Street").postCode("SW1A 1AA").build();
        PCSCase submitted = PCSCase.builder().propertyAddress(propertyAddress).build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        assertThat(result.getCaseId()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(result.getPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(result.getNotifications()).hasSize(2);
        assertThat(result.getTaskGroups()).hasSize(2);
    }

    @Test
    void shouldReturnNotificationsWithExpectedTemplatesAndValues() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

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
    void shouldReturnTaskGroupsWithExpectedStructure() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()))
            .extracting(g -> g.getGroupId(), g -> g.getTasks().size())
            .containsExactly(
                tuple(TaskGroupId.CLAIM, 2),
                tuple(TaskGroupId.RESPONSE, 7)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).getFirst().getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple("Defendant.ViewClaim", TaskStatus.AVAILABLE),
                tuple("Defendant.ViewDocuments", TaskStatus.NOT_AVAILABLE)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(1).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple("Defendant.RTC.StartNowAndDetails", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.PersonalDetails", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.DisputeAndTenancy", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.SituationAndCircumstances", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.IncomeAndExpenditure", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.UploadFiles", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.CheckYourAnswersAndSubmit", TaskStatus.NOT_AVAILABLE)
            );
    }

    @Test
    void shouldMarkSectionInProgressWhenDraftSectionStatusExists() {
        DraftCaseDataService draftCaseDataService = mock(DraftCaseDataService.class);
        DefendantResponseRepository defendantResponseRepository = mock(DefendantResponseRepository.class);
        SecurityContextService securityContextService = mock(SecurityContextService.class);

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draftWithSectionStatuses(Map.of("payments", "IN_PROGRESS"))));
        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            org.mockito.ArgumentMatchers.eq(CASE_REFERENCE),
            org.mockito.ArgumentMatchers.any(UUID.class)
        )).thenReturn(false);

        DashboardJourneyService service = new DashboardJourneyService(
            new ClaimTaskGroupEvaluator(),
            new ResponseTaskGroupEvaluator(draftCaseDataService, defendantResponseRepository, securityContextService)
        );

        DashboardData result = service.computeDashboardData(CASE_REFERENCE, rentArrearsCase());

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(1).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple("Defendant.RTC.StartNowAndDetails", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.PersonalDetails", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.DisputeAndTenancy", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.Payments", TaskStatus.IN_PROGRESS),
                tuple("Defendant.RTC.SituationAndCircumstances", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.IncomeAndExpenditure", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.UploadFiles", TaskStatus.AVAILABLE),
                tuple("Defendant.RTC.CheckYourAnswersAndSubmit", TaskStatus.NOT_AVAILABLE)
            );
    }

    @Test
    void shouldMakeCheckYourAnswersAvailableWhenAllApplicableSectionsCompleted() {
        DraftCaseDataService draftCaseDataService = mock(DraftCaseDataService.class);
        DefendantResponseRepository defendantResponseRepository = mock(DefendantResponseRepository.class);
        SecurityContextService securityContextService = mock(SecurityContextService.class);

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draftWithSectionStatuses(Map.of(
                "startNowAndDetails", "COMPLETED",
                "personalDetails", "COMPLETED",
                "disputeAndTenancy", "COMPLETED",
                "payments", "COMPLETED",
                "situationAndCircumstances", "COMPLETED",
                "incomeAndExpenditure", "COMPLETED",
                "uploadFiles", "COMPLETED"
            ))));
        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());

        DashboardJourneyService service = new DashboardJourneyService(
            new ClaimTaskGroupEvaluator(),
            new ResponseTaskGroupEvaluator(draftCaseDataService, defendantResponseRepository, securityContextService)
        );

        DashboardData result = service.computeDashboardData(CASE_REFERENCE, rentArrearsCase());

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(1).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple("Defendant.RTC.StartNowAndDetails", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.PersonalDetails", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.DisputeAndTenancy", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.Payments", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.SituationAndCircumstances", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.IncomeAndExpenditure", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.UploadFiles", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.CheckYourAnswersAndSubmit", TaskStatus.AVAILABLE)
            );
    }

    @Test
    void shouldIgnorePaymentsSectionForNonRentArrearsClaims() {
        DraftCaseDataService draftCaseDataService = mock(DraftCaseDataService.class);
        DefendantResponseRepository defendantResponseRepository = mock(DefendantResponseRepository.class);
        SecurityContextService securityContextService = mock(SecurityContextService.class);

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draftWithSectionStatuses(Map.of(
                "startNowAndDetails", "COMPLETED",
                "personalDetails", "COMPLETED",
                "disputeAndTenancy", "COMPLETED",
                "situationAndCircumstances", "COMPLETED",
                "incomeAndExpenditure", "COMPLETED",
                "uploadFiles", "COMPLETED"
            ))));
        when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());

        DashboardJourneyService service = new DashboardJourneyService(
            new ClaimTaskGroupEvaluator(),
            new ResponseTaskGroupEvaluator(draftCaseDataService, defendantResponseRepository, securityContextService)
        );

        DashboardData result = service.computeDashboardData(CASE_REFERENCE, nonRentArrearsCase());

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(1).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple("Defendant.RTC.StartNowAndDetails", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.PersonalDetails", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.DisputeAndTenancy", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.SituationAndCircumstances", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.IncomeAndExpenditure", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.UploadFiles", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.CheckYourAnswersAndSubmit", TaskStatus.AVAILABLE)
            );
    }

    @Test
    void shouldMarkResponseTasksCompletedWhenSubmittedResponseExists() {
        DraftCaseDataService draftCaseDataService = mock(DraftCaseDataService.class);
        DefendantResponseRepository defendantResponseRepository = mock(DefendantResponseRepository.class);
        SecurityContextService securityContextService = mock(SecurityContextService.class);
        UUID userId = UUID.randomUUID();

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draftWithSectionStatuses(Map.of())));
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(CASE_REFERENCE, userId))
            .thenReturn(true);

        DashboardJourneyService service = new DashboardJourneyService(
            new ClaimTaskGroupEvaluator(),
            new ResponseTaskGroupEvaluator(draftCaseDataService, defendantResponseRepository, securityContextService)
        );

        DashboardData result = service.computeDashboardData(CASE_REFERENCE, PCSCase.builder().build());

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(1).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple("Defendant.RTC.StartNowAndDetails", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.PersonalDetails", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.DisputeAndTenancy", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.SituationAndCircumstances", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.IncomeAndExpenditure", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.UploadFiles", TaskStatus.COMPLETED),
                tuple("Defendant.RTC.CheckYourAnswersAndSubmit", TaskStatus.AVAILABLE)
            );
    }

    @Test
    void shouldOnlyExposeDeclaredPlaceholdersForResponseToClaimNotification() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

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

    private PCSCase draftWithSectionStatuses(Map<String, String> sectionStatuses) {
        List<ListValue<SectionStatusEntry>> list = sectionStatuses.entrySet().stream()
            .map(e -> ListValue.<SectionStatusEntry>builder()
                .value(SectionStatusEntry.builder()
                    .sectionId(e.getKey())
                    .status(e.getValue())
                    .build())
                .build())
            .toList();

        return PCSCase.builder()
            .possessionClaimResponse(
                PossessionClaimResponse.builder()
                    .defendantResponses(
                        DefendantResponses.builder()
                            .sectionStatuses(list)
                            .build()
                    )
                    .build()
            )
            .build();
    }

    private PCSCase rentArrearsCase() {
        return PCSCase.builder()
            .claimGroundSummaries(List.of(ListValue.<ClaimGroundSummary>builder()
                .value(ClaimGroundSummary.builder().isRentArrears(YesOrNo.YES).build())
                .build()))
            .build();
    }

    private PCSCase nonRentArrearsCase() {
        return PCSCase.builder()
            .claimGroundSummaries(List.of(ListValue.<ClaimGroundSummary>builder()
                .value(ClaimGroundSummary.builder().isRentArrears(YesOrNo.NO).build())
                .build()))
            .build();
    }
}
