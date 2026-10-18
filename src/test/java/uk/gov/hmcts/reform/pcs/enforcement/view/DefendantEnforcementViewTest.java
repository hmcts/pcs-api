package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.EnforcementViewerService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.BailiffNote;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementEventAvailability;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementSummary;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PAYMENT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

@ExtendWith(MockitoExtension.class)
class DefendantEnforcementViewTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String ENFORCEMENT = FeatureCaseFileCategory.ENFORCEMENT.getId();
    private static final String STATEMENTS_OF_CASE = CaseFileCategory.STATEMENTS_OF_CASE.getId();

    @Mock
    private UserRoleService userRoleService;
    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private EnforcementStatusTabView enforcementStatusTabView;
    @Mock
    private SecurityContextService securityContextService;

    private final PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

    private DefendantEnforcementView underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantEnforcementView(
            new EnforcementViewerService(userRoleService, securityContextService), currentEnforcementService,
            enforcementStatusTabView);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"DEFENDANT", "DEFENDANT_SOLICITOR", "GA_DEFENDANT_SOLICITOR"})
    void shouldShowDefendantsNothingBeforeTheNoticeOfEvictionIsServed(UserRole defendantRole) {
        // Given
        EnforcementOrderEntity warrant = warrant(AWAITING_CLAIMANT_CONFIRMATION, false);
        DocumentEntity notice = documentOn(warrant, DocumentType.NOTICE_OF_EVICTION);
        ListValue<Document> claimForm = document(UUID.randomUUID().toString(), STATEMENTS_OF_CASE);
        PCSCase pcsCase = caseView(List.of(warrant), claimForm, document(notice.getId().toString(), ENFORCEMENT));
        givenTheCaseHas(warrant);
        givenCurrentUserHasRoles("caseworker-pcs-solicitor", defendantRole.getRole());

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getEnforcementOrder()).isNull();
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"DEFENDANT", "DEFENDANT_SOLICITOR", "GA_DEFENDANT_SOLICITOR"})
    void shouldShowDefendantsTheServedWarrantAndItsNoticeOnly(UserRole defendantRole) {
        // Given
        EnforcementOrderEntity served = warrant(CLOSED, true);
        EnforcementOrderEntity notServed = warrant(AWAITING_CLAIMANT_CONFIRMATION, false);
        DocumentEntity notice = documentOn(served, DocumentType.NOTICE_OF_EVICTION);
        DocumentEntity otherNotice = documentOn(notServed, DocumentType.NOTICE_OF_EVICTION);
        ListValue<Document> noticeDocument = document(notice.getId().toString(), ENFORCEMENT);
        ListValue<Document> claimForm = document(UUID.randomUUID().toString(), STATEMENTS_OF_CASE);
        ListValue<Document> evidence = document(UUID.randomUUID().toString(), ENFORCEMENT);
        PCSCase pcsCase = caseView(List.of(notServed, served), claimForm, noticeDocument, evidence,
                                   document(otherNotice.getId().toString(), ENFORCEMENT));
        givenTheCaseHas(notServed, served);
        givenCurrentUserHasRoles(defendantRole.getRole());

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getEnforcementOrder().getEnforcements())
            .extracting(ListValue::getId)
            .containsExactly(served.getId().toString());
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm, noticeDocument);
    }

    @Test
    void shouldLeaveDefendantsNoEnforcementFieldButTheVisibleWarrants() {
        // Given
        EnforcementOrderEntity served = warrant(AWAITING_CLAIMANT_CONFIRMATION, true);
        PCSCase pcsCase = caseView(List.of(served));
        EnforcementOrder full = pcsCase.getEnforcementOrder();
        full.setEnforcementStatus(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);
        full.setShowRiskAssessmentTab(YesOrNo.YES);
        full.setDefendantsCanSeeEnforcement(YesOrNo.YES);
        full.setShowConfirmEvictionJourney(YesOrNo.YES);
        full.setEventAvailability(EnforcementEventAvailabilityView.availability(served));
        full.setBailiffNotes(List.of(ListValue.<BailiffNote>builder().id("1").value(new BailiffNote()).build()));
        givenTheCaseHas(served);
        givenCurrentUserHasRoles(UserRole.GA_DEFENDANT_SOLICITOR.getRole());

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        EnforcementOrder expected = new EnforcementOrder();
        expected.setEnforcements(full.getEnforcements());
        assertThat(pcsCase.getEnforcementOrder()).isEqualTo(expected);
    }

    @Test
    void shouldGiveDefendantsTheStatusTabRenderedFromTheVisibleWarrantsAndNoRiskAssessment() {
        // Given
        EnforcementOrderEntity served = warrant(CLOSED, true);
        EnforcementOrderEntity notServed = warrant(AWAITING_CLAIMANT_CONFIRMATION, false);
        PCSCase pcsCase = caseView(List.of(notServed, served));
        EnforcementOrder full = pcsCase.getEnforcementOrder();
        full.setEnforcementStatusTabMarkdown("every warrant, with the claimant's answers");
        full.setShowRiskAssessmentTab(YesOrNo.YES);
        full.setRiskAssessmentPrintMarkdown("print");
        full.setRiskAssessmentInternalMarkdown("internal");
        full.setRiskAssessmentClaimantMarkdown("claimant answers");
        givenTheCaseHas(notServed, served);
        givenCurrentUserHasRoles(UserRole.GA_DEFENDANT_SOLICITOR.getRole());
        when(enforcementStatusTabView.defendantTabMarkdown(List.of(served))).thenReturn("the served warrant");

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        EnforcementOrder defendantView = pcsCase.getEnforcementOrder();
        assertThat(defendantView.getEnforcementStatusTabMarkdown()).isEqualTo("the served warrant");
        assertThat(defendantView.getShowRiskAssessmentTab()).isNull();
        assertThat(defendantView.getRiskAssessmentPrintMarkdown()).isNull();
        assertThat(defendantView.getRiskAssessmentInternalMarkdown()).isNull();
        assertThat(defendantView.getRiskAssessmentClaimantMarkdown()).isNull();
    }

    @Test
    void shouldHideEverythingFromDefendantsWhileAWarrantOfRestitutionIsLive() {
        // Given
        EnforcementOrderEntity served = warrant(CLOSED, true);
        EnforcementOrderEntity restitution = warrant(AWAITING_PAYMENT, false);
        restitution.setWarrantDetails(null);
        restitution.setWarrantOfRestitutionDetails(new WarrantOfRestitutionEntity());
        DocumentEntity notice = documentOn(served, DocumentType.NOTICE_OF_EVICTION);
        PCSCase pcsCase = caseView(List.of(restitution, served), document(notice.getId().toString(), ENFORCEMENT));
        givenTheCaseHas(restitution, served);
        givenCurrentUserHasRoles(UserRole.DEFENDANT.getRole());

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getEnforcementOrder()).isNull();
        assertThat(pcsCase.getAllDocuments()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "CREATOR", "CLAIMANT_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT",
        "JUDGE", "CTSC_ADMIN", "HEARING_CENTRE_ADMIN", "HEARING_CENTRE_TEAM_LEADER", "SYSTEM_USER", "BAILIFF"
    })
    void shouldLeaveEveryoneElseTheWholeEnforcement(UserRole role) {
        // Given
        EnforcementOrderEntity notServed = warrant(AWAITING_CLAIMANT_CONFIRMATION, false);
        ListValue<Document> claimForm = document("1", STATEMENTS_OF_CASE);
        ListValue<Document> evidence = document("2", ENFORCEMENT);
        PCSCase pcsCase = caseView(List.of(notServed), claimForm, evidence);
        EnforcementOrder full = pcsCase.getEnforcementOrder();
        full.setShowRiskAssessmentTab(YesOrNo.YES);
        givenTheCaseHas(notServed);
        givenCurrentUserHasRoles(role.getRole());

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getEnforcementOrder()).isSameAs(full);
        assertThat(full.getShowRiskAssessmentTab()).isEqualTo(YesOrNo.YES);
        assertThat(full.getEnforcements()).hasSize(1);
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm, evidence);
    }

    /**
     * A defendant's legal representative holds {@code caseworker-pcs-solicitor} too, so a user with no case
     * role on either side - for instance because the case roles could not be looked up - gets the defendant view.
     */
    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"PCS_SOLICITOR", "PCS_CASE_WORKER", "CITIZEN"})
    void shouldRestrictAUserWhoseSideOfTheCaseCannotBeEstablished(UserRole role) {
        // Given
        EnforcementOrderEntity served = warrant(CLOSED, true);
        EnforcementOrderEntity notServed = warrant(AWAITING_CLAIMANT_CONFIRMATION, false);
        ListValue<Document> evidence = document(UUID.randomUUID().toString(), ENFORCEMENT);
        PCSCase pcsCase = caseView(List.of(notServed, served), evidence);
        pcsCase.getEnforcementOrder().setRiskAssessmentClaimantMarkdown("the claimant's answers");
        givenTheCaseHas(notServed, served);
        givenCurrentUserHasRoles(role.getRole());

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getEnforcementOrder().getRiskAssessmentClaimantMarkdown()).isNull();
        assertThat(pcsCase.getEnforcementOrder().getEnforcements())
            .extracting(ListValue::getId)
            .containsExactly(served.getId().toString());
        assertThat(pcsCase.getAllDocuments()).isEmpty();
    }

    @Test
    void shouldRestrictASolicitorWhoseCaseRolesCameBackEmpty() {
        // Given
        EnforcementOrderEntity notServed = warrant(AWAITING_CLAIMANT_CONFIRMATION, false);
        PCSCase pcsCase = caseView(List.of(notServed));
        givenTheCaseHas(notServed);
        when(userRoleService.getCurrentUserCaseRoles(eq(CASE_REFERENCE), anyCollection()))
            .thenReturn(new UserRoles("user-id", null));

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldNotLookUpRolesWhenTheClaimHasNoEnforcement() {
        // Given
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(new EnforcementOrder()).build();
        pcsCase.getEnforcementOrder().setHasUnsubmittedEnforcementData(YesOrNo.NO);
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity)).thenReturn(List.of());

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getEnforcementOrder().getHasUnsubmittedEnforcementData()).isEqualTo(YesOrNo.NO);
        verifyNoInteractions(userRoleService);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"DEFENDANT", "DEFENDANT_SOLICITOR", "GA_DEFENDANT_SOLICITOR"})
    void shouldOfferDefendantsNoEnforcementEventWhenTheClaimHasNoEnforcement(UserRole defendantRole) {
        // Given
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(new EnforcementOrder()).build();
        pcsCase.getEnforcementOrder().setHasUnsubmittedEnforcementData(YesOrNo.NO);
        pcsCase.getEnforcementOrder().setEventAvailability(eventFlagsWithNoEnforcement());
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity)).thenReturn(List.of());
        givenCurrentUserHasRoles("caseworker-pcs-solicitor", defendantRole.getRole());

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getEnforcementOrder().getEventAvailability()).isNull();
        assertThat(pcsCase.getEnforcementOrder().getHasUnsubmittedEnforcementData()).isEqualTo(YesOrNo.NO);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CLAIMANT_SOLICITOR", "CLAIMANT", "BAILIFF", "HEARING_CENTRE_ADMIN"})
    void shouldLeaveEveryoneElseTheEventFlagsWhenTheClaimHasNoEnforcement(UserRole role) {
        // Given
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(new EnforcementOrder()).build();
        pcsCase.getEnforcementOrder().setEventAvailability(eventFlagsWithNoEnforcement());
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity)).thenReturn(List.of());
        givenCurrentUserHasRoles("caseworker-pcs-solicitor", role.getRole());

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getEnforcementOrder().getEventAvailability()).isEqualTo(eventFlagsWithNoEnforcement());
    }

    private static EnforcementEventAvailability eventFlagsWithNoEnforcement() {
        return EnforcementEventAvailabilityView.availability(null);
    }

    @Test
    void shouldNotLookUpRolesWhenThereIsNothingAboutEnforcement() {
        // Given
        ListValue<Document> claimForm = document("1", STATEMENTS_OF_CASE);
        PCSCase pcsCase = PCSCase.builder().allDocuments(List.of(claimForm)).build();

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).containsExactly(claimForm);
        verifyNoInteractions(userRoleService, currentEnforcementService);
    }

    @Test
    void shouldLeaveACaseWithNoDocumentsOrEnforcementAlone() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.restrictForDefendants(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getAllDocuments()).isNull();
        assertThat(pcsCase.getEnforcementOrder()).isNull();
        verifyNoInteractions(userRoleService, currentEnforcementService);
    }

    private void givenTheCaseHas(EnforcementOrderEntity... enforcements) {
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity)).thenReturn(List.of(enforcements));
    }

    private void givenCurrentUserHasRoles(String... roles) {
        when(userRoleService.getCurrentUserCaseRoles(eq(CASE_REFERENCE), anyCollection()))
            .thenReturn(new UserRoles("user-id", List.of(roles)));
    }

    /**
     * The case view as the other enforcement views leave it: one {@code enforcements} item per enforcement.
     */
    @SafeVarargs
    private static PCSCase caseView(List<EnforcementOrderEntity> enforcements, ListValue<Document>... documents) {
        EnforcementOrder enforcementOrder = new EnforcementOrder();
        enforcementOrder.setEnforcements(enforcements.stream()
                                             .map(enforcement -> ListValue.<EnforcementSummary>builder()
                                                 .id(enforcement.getId().toString())
                                                 .value(EnforcementSummary.builder()
                                                            .status(enforcement.getStatus())
                                                            .build())
                                                 .build())
                                             .toList());
        return PCSCase.builder()
            .enforcementOrder(enforcementOrder)
            .allDocuments(documents.length == 0 ? null : List.of(documents))
            .build();
    }

    private static EnforcementOrderEntity warrant(EnforcementStatus status, boolean defendantsCanSee) {
        return EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .status(status)
            .warrantDetails(new WarrantEntity())
            .defendantsCanSeeEnforcement(defendantsCanSee)
            .documents(new ArrayList<>())
            .build();
    }

    private static DocumentEntity documentOn(EnforcementOrderEntity enforcement, DocumentType type) {
        DocumentEntity document = DocumentEntity.builder().id(UUID.randomUUID()).type(type).build();
        enforcement.getDocuments().add(document);
        return document;
    }

    private static ListValue<Document> document(String id, String categoryId) {
        return ListValue.<Document>builder()
            .id(id)
            .value(Document.builder()
                       .filename(id + ".pdf")
                       .url("http://dm-store/documents/" + id)
                       .categoryId(categoryId)
                       .build())
            .build();
    }
}
