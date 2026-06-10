package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.RelatedApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ApplicationsTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ClaimTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.DocumentsTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.HearingsTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.NoticesTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ResponseTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardJourneyServiceTest {

    private static final long CASE_REFERENCE = 99_887_766L;

    private DashboardJourneyService underTest;

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private DefendantResponseService defendantResponseService;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private LegalRepresentativeRepository legalRepresentativeRepository;

    private GenAppVisibilityService genAppVisibilityService;

    @BeforeEach
    void setUp() {
        genAppVisibilityService = new GenAppVisibilityService(legalRepresentativeRepository);
        underTest = new DashboardJourneyService(
            draftCaseDataService, defendantResponseService, List.of(
                new ClaimTaskGroupEvaluator(),
                new DocumentsTaskGroupEvaluator(),
                new ResponseTaskGroupEvaluator(),
                new ApplicationsTaskGroupEvaluator(securityContextService, genAppVisibilityService),
                new HearingsTaskGroupEvaluator(),
                new NoticesTaskGroupEvaluator()
        ));
    }

    @Test
    void shouldPopulateCaseMetadata() {
        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 High Street").postCode("SW1A 1AA").build();
        PCSCase submitted = PCSCase.builder().propertyAddress(propertyAddress).build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        assertThat(result.getCaseId()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(result.getPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(result.getNotifications()).hasSize(2);
        assertThat(result.getTaskGroups()).hasSize(6);
        assertThat(result.getRelatedApplications()).isEmpty();

    }

    @Test
    void shouldReturnNotificationsWithExpectedTemplatesAndValues() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        assertThat(ListValueUtils.unwrapListItems(result.getNotifications()))
            .extracting(n -> n.getTemplateId(), n -> n.getTemplateValues().size())
            .containsExactly(
                tuple("Defendant.NoHearingArranged", 0),
                tuple("Defendant.ResponseNotStarted", 0)
            );
    }

    @Test
    void shouldReturnTaskGroupsWithExpectedStructure() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()))
            .extracting(g -> g.getGroupId(), g -> g.getTasks().size())
            .containsExactly(
                tuple(TaskGroupId.CLAIM, 1),
                tuple(TaskGroupId.DOCUMENTS, 2),
                tuple(TaskGroupId.RESPONSE, 2),
                tuple(TaskGroupId.HEARING, 1),
                tuple(TaskGroupId.NOTICE, 1),
                tuple(TaskGroupId.APPLICATIONS, 2)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(0).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.VIEW_CLAIM, TaskStatus.AVAILABLE)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(1).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.UPLOAD_DOCUMENTS, TaskStatus.AVAILABLE),
                tuple(DashboardTaskTemplateIds.VIEW_DOCUMENTS, TaskStatus.NOT_AVAILABLE)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(2).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.RESPOND_TO_CLAIM, TaskStatus.NOT_STARTED),
                tuple(DashboardTaskTemplateIds.VIEW_RESPONSE, TaskStatus.NOT_AVAILABLE)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(3).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.VIEW_HEARING_DOCUMENTS, TaskStatus.AVAILABLE)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(4).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.VIEW_ORDERS_AND_NOTICES, TaskStatus.AVAILABLE)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(5).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.MAKE_GENERAL_APPLICATION, TaskStatus.AVAILABLE),
                tuple(DashboardTaskTemplateIds.VIEW_ALL_APPLICATIONS, TaskStatus.NOT_AVAILABLE)
            );
    }

    @Test
    void shouldShowViewApplicationsTaskWhenAtLeastOneGeneralApplicationExists() {
        PCSCase submitted = PCSCase.builder().build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .genApps(Set.of(GenAppEntity.builder().build()))
            .build();

        PartyEntity defendant = PartyEntity.builder().idamId(UUID.randomUUID()).build();
        DashboardData result = underTest.computeDashboardData(
            CASE_REFERENCE,
            submitted,
            caseEntity,
            defendant
        );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(5).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.MAKE_GENERAL_APPLICATION, TaskStatus.AVAILABLE),
                tuple(DashboardTaskTemplateIds.VIEW_ALL_APPLICATIONS, TaskStatus.AVAILABLE)
            );
    }

    @Test
    void shouldPopulateRelatedApplicationsFromGeneralApplications() {
        PCSCase submitted = PCSCase.builder().build();

        GenAppEntity genApp = GenAppEntity.builder()
            .id(UUID.randomUUID())
            .type(GenAppType.ADJOURN)
            .applicationSubmittedDate(LocalDateTime.of(2026, 4, 28, 10, 30))
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .genApps(Set.of(genApp))
            .build();

        PartyEntity defendant = PartyEntity.builder().idamId(UUID.randomUUID()).build();
        DashboardData result = underTest.computeDashboardData(
            CASE_REFERENCE,
            submitted,
            caseEntity,
            defendant
        );

        assertThat(ListValueUtils.unwrapListItems(result.getRelatedApplications()))
            .extracting(RelatedApplication::getType, RelatedApplication::getApplicationSubmittedDate)
            .containsExactly(
                tuple(GenAppType.ADJOURN, LocalDateTime.of(2026, 4, 28, 10, 30))
            );

        assertThat(ListValueUtils.unwrapListItems(result.getRelatedApplications()))
            .extracting(RelatedApplication::getId)
            .allMatch(id -> id != null && !id.isBlank());
    }

    @Test
    void shouldOrderRelatedApplicationsBySubmittedDateNewestFirst() {
        PCSCase submitted = PCSCase.builder().build();

        GenAppEntity olderGenApp = GenAppEntity.builder()
            .id(UUID.randomUUID())
            .type(GenAppType.SET_ASIDE)
            .applicationSubmittedDate(LocalDateTime.of(2026, 3, 1, 9, 0))
            .build();

        GenAppEntity newerGenApp = GenAppEntity.builder()
            .id(UUID.randomUUID())
            .type(GenAppType.ADJOURN)
            .applicationSubmittedDate(LocalDateTime.of(2026, 5, 15, 14, 30))
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .genApps(Set.of(olderGenApp, newerGenApp))
            .build();

        PartyEntity defendant = PartyEntity.builder().idamId(UUID.randomUUID()).build();
        DashboardData result = underTest.computeDashboardData(
            CASE_REFERENCE,
            submitted,
            caseEntity,
            defendant
        );

        assertThat(ListValueUtils.unwrapListItems(result.getRelatedApplications()))
            .extracting(RelatedApplication::getType, RelatedApplication::getApplicationSubmittedDate)
            .containsExactly(
                tuple(GenAppType.ADJOURN, LocalDateTime.of(2026, 5, 15, 14, 30)),
                tuple(GenAppType.SET_ASIDE, LocalDateTime.of(2026, 3, 1, 9, 0))
            );
    }

    @Test
    void shouldOmitWithoutNoticeApplicationsRaisedByAnotherUser() {
        PCSCase submitted = PCSCase.builder().build();
        UUID applicantId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();

        PartyEntity applicant = PartyEntity.builder().idamId(applicantId).build();
        GenAppEntity hiddenGenApp = GenAppEntity.builder()
            .id(UUID.randomUUID())
            .type(GenAppType.ADJOURN)
            .withoutNotice(VerticalYesNo.YES)
            .party(applicant)
            .applicationSubmittedDate(LocalDateTime.of(2026, 4, 28, 10, 30))
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .genApps(Set.of(hiddenGenApp))
            .build();

        PartyEntity defendant = PartyEntity.builder().idamId(viewerId).build();
        DashboardData result = underTest.computeDashboardData(
            CASE_REFERENCE,
            submitted,
            caseEntity,
            defendant
        );

        assertThat(ListValueUtils.unwrapListItems(result.getRelatedApplications())).isEmpty();
        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(5).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.MAKE_GENERAL_APPLICATION, TaskStatus.AVAILABLE),
                tuple(DashboardTaskTemplateIds.VIEW_ALL_APPLICATIONS, TaskStatus.NOT_AVAILABLE)
            );
    }

    @Test
    void shouldIncludeWithoutNoticeApplicationForApplicantIdamUser() {
        PCSCase submitted = PCSCase.builder().build();
        UUID applicantId = UUID.randomUUID();

        PartyEntity applicant = PartyEntity.builder().idamId(applicantId).build();
        GenAppEntity ownGenApp = GenAppEntity.builder()
            .id(UUID.randomUUID())
            .type(GenAppType.ADJOURN)
            .withoutNotice(VerticalYesNo.YES)
            .party(applicant)
            .applicationSubmittedDate(LocalDateTime.of(2026, 4, 28, 10, 30))
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .genApps(Set.of(ownGenApp))
            .build();

        PartyEntity defendant = PartyEntity.builder().idamId(applicantId).build();
        DashboardData result = underTest.computeDashboardData(
            CASE_REFERENCE,
            submitted,
            caseEntity,
            defendant
        );

        assertThat(ListValueUtils.unwrapListItems(result.getRelatedApplications()))
            .extracting(RelatedApplication::getType)
            .containsExactly(GenAppType.ADJOURN);
    }

    @Test
    void shouldOnlyExposeDeclaredPlaceholdersForResponseToClaimNotification() {
        when(draftCaseDataService.hasMeaningfulRespondDraft(CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(true);
        when(defendantResponseService.hasSubmittedResponse(CASE_REFERENCE)).thenReturn(false);

        PCSCase submitted = PCSCase.builder().build();
        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        assertThat(ListValueUtils.unwrapListItems(result.getNotifications()))
            .extracting(n -> n.getTemplateId())
            .containsExactly(
                "Defendant.NoHearingArranged",
                "Defendant.ResponseInProgress"
            );
    }

    @Test
    void shouldMarkRespondToClaimInProgressWhenDraftExists() {
        when(draftCaseDataService.hasMeaningfulRespondDraft(CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(true);
        when(defendantResponseService.hasSubmittedResponse(CASE_REFERENCE)).thenReturn(false);

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, PCSCase.builder().build());

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(2).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .contains(tuple(DashboardTaskTemplateIds.RESPOND_TO_CLAIM, TaskStatus.IN_PROGRESS));
    }

    @Test
    void shouldMarkRespondCompletedAndViewResponseAvailableWhenSubmittedExists() {
        when(draftCaseDataService.hasMeaningfulRespondDraft(CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false);
        when(defendantResponseService.hasSubmittedResponse(CASE_REFERENCE)).thenReturn(true);

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, PCSCase.builder().build());

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(2).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.RESPOND_TO_CLAIM, TaskStatus.COMPLETED),
                tuple(DashboardTaskTemplateIds.VIEW_RESPONSE, TaskStatus.AVAILABLE)
            );
    }

}
