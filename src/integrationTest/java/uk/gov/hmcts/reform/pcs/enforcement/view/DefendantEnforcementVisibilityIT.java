package uk.gov.hmcts.reform.pcs.enforcement.view;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureCaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.ccd.view.DocumentsView;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffNoteRepository;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * The case view a defendant gets of enforcement, from the enforcement records and documents in Postgres
 * through the enforcement case view contributor, as the notice of eviction is served and a warrant of
 * restitution is applied for (14-defendant-visibility.feature).
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Defendant visibility of enforcement in the case view")
class DefendantEnforcementVisibilityIT extends AbstractPostgresContainerIT {

    private static final LocalDateTime EVICTION = LocalDateTime.parse("2099-06-25T10:00");
    private static final LocalDateTime SERVED = LocalDateTime.parse("2026-03-15T08:00");
    private static final String ENFORCEMENT = FeatureCaseFileCategory.ENFORCEMENT.getId();

    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private EvictionScheduleService evictionScheduleService;
    @Autowired
    private DocumentsView documentsView;
    @Autowired
    private EnforcementCaseViewContributor enforcementCaseViewContributor;
    @Autowired
    private BailiffNoteRepository bailiffNoteRepository;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private UserRoleService userRoleService;
    @MockitoBean
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void enableEnforcement() {
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);
    }

    @Test
    @DisplayName("before the notice is served a defendant sees no enforcement field or document")
    void defendantsSeeNothingBeforeTheNoticeIsServed() {
        long caseReference = 1414000000000001L;
        savedWarrantWithDocuments(caseReference);
        evictionScheduleService.schedule(caseReference, EVICTION, false);
        flushAndClear();

        PCSCase defendantView = caseViewAs(caseReference, UserRole.GA_DEFENDANT_SOLICITOR);

        assertThat(defendantView.getEnforcementOrder()).isNull();
        assertThat(filenames(defendantView)).containsExactly("claim.pdf");
    }

    @Test
    @DisplayName("once the notice is served a defendant sees the warrant and its notice of eviction only")
    void defendantsSeeTheWarrantAndItsNoticeOnceServed() {
        long caseReference = 1414000000000002L;
        EnforcementOrderEntity warrant = savedWarrantWithDocuments(caseReference);
        evictionScheduleService.schedule(caseReference, EVICTION, false);
        evictionScheduleService.markNoticeServed(caseReference, SERVED, "by hand", null);
        bailiffNoteRepository.save(BailiffNoteEntity.builder()
            .enforcementOrder(enforcementOrderRepository.findById(warrant.getId()).orElseThrow())
            .note("A bailiff note")
            .createdAt(Instant.now())
            .build());
        flushAndClear();
        assertThat(caseViewAs(caseReference, UserRole.BAILIFF).getEnforcementOrder().getBailiffNotes()).hasSize(1);

        PCSCase defendantView = caseViewAs(caseReference, UserRole.DEFENDANT);

        EnforcementOrder enforcementOrder = defendantView.getEnforcementOrder();
        assertThat(enforcementOrder.getEnforcements()).extracting(ListValue::getId)
            .containsExactly(warrant.getId().toString());
        assertThat(enforcementOrder.getEnforcements().getFirst().getValue().getStatus())
            .isEqualTo(EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION);
        assertThat(enforcementOrder.getBailiffNotes()).isNull();
        assertThat(enforcementOrder.getShowRiskAssessmentTab()).isNull();
        assertThat(enforcementOrder.getEnforcementStatus()).isNull();
        assertThat(enforcementOrder.getEnforcementStatusTabMarkdown())
            .contains(">Eviction notice</h3>", ">Scheduled eviction</h3>", ">Eviction outcome</h3>")
            .doesNotContain("Claimant eviction confirmation");
        assertThat(enforcementOrder.getRiskAssessmentClaimantMarkdown()).isNull();
        assertThat(enforcementOrder.getRiskAssessmentInternalMarkdown()).isNull();
        assertThat(filenames(defendantView)).containsExactlyInAnyOrder("claim.pdf", "notice-of-eviction.pdf");
    }

    @Test
    @DisplayName("the claimant side loses only the notice of eviction, internal users keep the whole enforcement")
    void claimantSideLosesOnlyTheNoticeOfEviction() {
        long caseReference = 1414000000000003L;
        savedWarrantWithDocuments(caseReference);
        evictionScheduleService.schedule(caseReference, EVICTION, false);
        flushAndClear();

        PCSCase claimantView = caseViewAs(caseReference, UserRole.PCS_SOLICITOR, UserRole.CLAIMANT_SOLICITOR);
        PCSCase bailiffView = caseViewAs(caseReference, UserRole.BAILIFF);

        assertThat(claimantView.getEnforcementOrder().getEnforcements()).hasSize(1);
        assertThat(claimantView.getEnforcementOrder().getEnforcementStatusTabMarkdown())
            .contains("Claimant eviction confirmation");
        // Of the two sides, only defendants see the notice of eviction (05-schedule-eviction.feature).
        assertThat(filenames(claimantView))
            .containsExactlyInAnyOrder("claim.pdf", "evidence.pdf");
        assertThat(filenames(bailiffView))
            .containsExactlyInAnyOrder("claim.pdf", "notice-of-eviction.pdf", "evidence.pdf");
        assertThat(bailiffView.getEnforcementOrder().getEnforcements()).hasSize(1);
        assertThat(bailiffView.getEnforcementOrder().getDefendantsCanSeeEnforcement()).isEqualTo(YesOrNo.NO);
    }

    @Test
    @DisplayName("a legal representative with no case role on either side sees what a defendant sees")
    void solicitorWithNoSideSeesTheDefendantView() {
        long caseReference = 1414000000000006L;
        savedWarrantWithDocuments(caseReference);
        evictionScheduleService.schedule(caseReference, EVICTION, false);
        flushAndClear();

        // caseworker-pcs-solicitor alone, as when /case-users returns nothing for the user
        PCSCase solicitorView = caseViewAs(caseReference, UserRole.PCS_SOLICITOR);

        assertThat(solicitorView.getEnforcementOrder()).isNull();
        assertThat(filenames(solicitorView)).containsExactly("claim.pdf");
    }

    @Test
    @DisplayName("a without notice eviction stays hidden from defendants")
    void withoutNoticeEvictionStaysHidden() {
        long caseReference = 1414000000000004L;
        savedWarrantWithDocuments(caseReference);
        evictionScheduleService.schedule(caseReference, EVICTION, true);
        flushAndClear();

        PCSCase defendantView = caseViewAs(caseReference, UserRole.DEFENDANT_SOLICITOR);

        assertThat(defendantView.getEnforcementOrder()).isNull();
        assertThat(filenames(defendantView)).containsExactly("claim.pdf");
    }

    @Test
    @DisplayName("a live warrant of restitution hides the earlier, served warrant from defendants again")
    void warrantOfRestitutionHidesEnforcementAgain() {
        long caseReference = 1414000000000005L;
        final EnforcementOrderEntity warrant = savedWarrantWithDocuments(caseReference);
        evictionScheduleService.schedule(caseReference, EVICTION, false);
        evictionScheduleService.markNoticeServed(caseReference, SERVED, "by hand", null);
        flushAndClear();
        assertThat(caseViewAs(caseReference, UserRole.DEFENDANT).getEnforcementOrder()).isNotNull();

        EnforcementOrderEntity closed = enforcementOrderRepository.findById(warrant.getId()).orElseThrow();
        closed.setStatus(EnforcementStatus.CLOSED);
        // Flushed before the restitution is inserted: Hibernate runs inserts before updates, so without this
        // the claim briefly has two live enforcements, which the one-live-enforcement index refuses.
        entityManager.flush();
        final EnforcementOrderEntity restitution = savedEnforcement(
            closed.getClaim(), SelectEnforcementType.WARRANT_OF_RESTITUTION, EnforcementStatus.AWAITING_PAYMENT);
        flushAndClear();

        PCSCase whileLive = caseViewAs(caseReference, UserRole.DEFENDANT);
        assertThat(whileLive.getEnforcementOrder()).isNull();
        assertThat(filenames(whileLive)).containsExactly("claim.pdf");

        enforcementOrderRepository.findById(restitution.getId()).orElseThrow().setStatus(EnforcementStatus.CLOSED);
        flushAndClear();

        assertThat(caseViewAs(caseReference, UserRole.DEFENDANT).getEnforcementOrder().getEnforcements())
            .extracting(ListValue::getId)
            .containsExactly(warrant.getId().toString());
    }

    private PCSCase caseViewAs(long caseReference, UserRole... roles) {
        when(userRoleService.getCurrentUserCaseRoles(anyLong(), anyCollection())).thenReturn(new UserRoles(
            "user-id", Arrays.stream(roles).map(UserRole::getRole).toList()));
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();
        PCSCase pcsCase = PCSCase.builder().build();
        documentsView.setCaseFields(pcsCase, pcsCaseEntity, null);
        enforcementCaseViewContributor.contribute(pcsCaseEntity, pcsCase, caseReference, State.JUDICIAL_REFERRAL);
        return pcsCase;
    }

    private static List<String> filenames(PCSCase pcsCase) {
        return pcsCase.getAllDocuments().stream()
            .map(ListValue::getValue)
            .map(Document::getFilename)
            .toList();
    }

    /**
     * A warrant awaiting scheduling, with the claim form in Statements of case and, in the Enforcement folder,
     * a notice of eviction and some claimant evidence.
     */
    private EnforcementOrderEntity savedWarrantWithDocuments(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        pcsCase.addClaim(new ClaimEntity());
        pcsCase = pcsCaseRepository.save(pcsCase);
        EnforcementOrderEntity warrant = savedEnforcement(
            pcsCase.getClaims().getFirst(), SelectEnforcementType.WARRANT, EnforcementStatus.AWAITING_SCHEDULING);

        DocumentEntity claimForm = document("claim.pdf", CaseFileCategory.STATEMENTS_OF_CASE.getId(),
                                            DocumentType.CLAIM);
        DocumentEntity notice = document("notice-of-eviction.pdf", ENFORCEMENT, DocumentType.NOTICE_OF_EVICTION);
        DocumentEntity evidence = document("evidence.pdf", ENFORCEMENT, DocumentType.PHOTOGRAPHIC_EVIDENCE);
        pcsCase.addDocuments(List.of(claimForm, notice, evidence));
        warrant.addDocuments(List.of(notice, evidence));
        entityManager.flush();
        return warrant;
    }

    private EnforcementOrderEntity savedEnforcement(ClaimEntity claim, SelectEnforcementType type,
                                                    EnforcementStatus status) {
        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(EnforcementOrder.builder().selectEnforcementType(type).build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(Instant.now());
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        entityManager.flush();
        return saved;
    }

    private static DocumentEntity document(String fileName, String categoryId, DocumentType type) {
        return DocumentEntity.builder()
            .fileName(fileName)
            .url("http://dm-store/documents/" + fileName)
            .binaryUrl("http://dm-store/documents/" + fileName + "/binary")
            .categoryId(categoryId)
            .type(type)
            .build();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
