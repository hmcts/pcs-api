package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_REVIEW;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentTabViewTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private BailiffNoteService bailiffNoteService;
    @Mock
    private UserRoleService userRoleService;

    private RiskAssessmentTabView underTest;
    private final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

    @BeforeEach
    void setUp() {
        underTest = new RiskAssessmentTabView(currentEnforcementService, bailiffNoteService, userRoleService);
        lenient().when(bailiffNoteService.getNotes(any())).thenReturn(List.of());
        signedInWith("bailiff");
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, mode = EnumSource.Mode.EXCLUDE,
        names = {"AWAITING_PAYMENT", "AWAITING_REVIEW", "REJECTED"})
    void shouldShowTheTabForAnApprovedWarrantLiveOrClosed(EnforcementStatus status) {
        givenEnforcements(warrant(status));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        assertThat(enforcementOrder.getShowRiskAssessmentTab()).isEqualTo(YesOrNo.YES);
        assertThat(enforcementOrder.getRiskAssessmentPrintMarkdown()).contains("#Risk%20assessment");
        assertThat(enforcementOrder.getRiskAssessmentInternalMarkdown()).contains("Bailiff note");
        assertThat(enforcementOrder.getRiskAssessmentClaimantMarkdown()).contains("Claimant risk level");
    }

    @Test
    void shouldShowTheTabForAWarrantOfRestitution() {
        givenEnforcements(EnforcementOrderEntity.builder()
                              .id(UUID.randomUUID())
                              .status(AWAITING_SCHEDULING)
                              .warrantOfRestitutionDetails(WarrantOfRestitutionEntity.builder().build())
                              .build());
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        assertThat(pcsCase.getEnforcementOrder().getShowRiskAssessmentTab()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getEnforcementOrder().getRiskAssessmentClaimantMarkdown())
            .doesNotContain("Do you want to evict everyone at the property?");
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"AWAITING_PAYMENT", "AWAITING_REVIEW", "REJECTED"})
    void shouldNotShowTheTabBeforeTheWarrantIsGenerated(EnforcementStatus status) {
        givenEnforcements(warrant(status));
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .showRiskAssessmentTab(YesOrNo.YES)
                                  .riskAssessmentInternalMarkdown("stale")
                                  .build())
            .build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        assertThat(pcsCase.getEnforcementOrder().getShowRiskAssessmentTab()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getEnforcementOrder().getRiskAssessmentInternalMarkdown()).isNull();
        verifyNoInteractions(userRoleService);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"AWAITING_REVIEW", "TRANSFERRED_TO_HIGH_COURT"})
    void shouldNotShowTheTabForAWrit(EnforcementStatus status) {
        // A writ has no warrant record: its details live in the enforcement order JSON
        givenEnforcements(EnforcementOrderEntity.builder()
                              .status(status)
                              .enforcementOrder(EnforcementOrder.builder()
                                                    .writDetails(WritDetails.builder().build())
                                                    .build())
                              .build());
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(EnforcementOrder.builder().build()).build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        assertThat(pcsCase.getEnforcementOrder().getShowRiskAssessmentTab()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldShowTheMostRecentWarrantWhenTheCurrentEnforcementIsAWrit() {
        EnforcementOrderEntity writ = EnforcementOrderEntity.builder()
            .status(TRANSFERRED_TO_HIGH_COURT)
            .enforcementOrder(EnforcementOrder.builder().writDetails(WritDetails.builder().build()).build())
            .build();
        EnforcementOrderEntity closedWarrant = warrant(CLOSED);
        closedWarrant.setRiskLevel(RiskLevel.HIGH);
        closedWarrant.setRiskLevelReason("Dog on site");
        givenEnforcements(writ, closedWarrant);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        assertThat(pcsCase.getEnforcementOrder().getShowRiskAssessmentTab()).isEqualTo(YesOrNo.YES);
        assertThat(pcsCase.getEnforcementOrder().getRiskAssessmentInternalMarkdown()).contains("Dog on site");
    }

    @Test
    void shouldShowTheWarrantsOwnNotes() {
        EnforcementOrderEntity enforcement = warrant(AWAITING_SCHEDULING);
        givenEnforcements(enforcement);
        when(bailiffNoteService.getNotes(enforcement)).thenReturn(List.of(BailiffNoteEntity.builder()
            .note("Rear gate is locked")
            .createdAt(Instant.parse("2026-02-01T16:20:00Z"))
            .build()));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        assertThat(pcsCase.getEnforcementOrder().getRiskAssessmentInternalMarkdown())
            .contains("Rear gate is locked");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[CLAIMANTSOLICITOR]", "claimant", "claimant-solicitor"})
    void shouldWordTheClaimantsAnswersForTheClaimantSide(String role) {
        signedInWith("caseworker-pcs-solicitor", role);
        givenEnforcements(warrant(AWAITING_SCHEDULING));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        assertThat(pcsCase.getEnforcementOrder().getRiskAssessmentClaimantMarkdown())
            .contains("Information you provided")
            .doesNotContain("provided by the claimant");
    }

    @Test
    void shouldWordTheClaimantsAnswersInTheThirdPersonForInternalUsers() {
        givenEnforcements(warrant(AWAITING_SCHEDULING));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        assertThat(pcsCase.getEnforcementOrder().getRiskAssessmentClaimantMarkdown())
            .contains("Information provided by the claimant");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[DEFENDANTSOLICITOR]", "[DEFENDANT]", "defendant-solicitor"})
    void shouldHideTheTabFromTheDefendantSide(String role) {
        // A defendant's legal representative holds caseworker-pcs-solicitor, as the claimant's does
        signedInWith("caseworker-pcs-solicitor", role);
        givenEnforcements(warrant(AWAITING_SCHEDULING));
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldNotAddAnEnforcementOrderToACaseWithNoEnforcement() {
        givenEnforcements();
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        assertThat(pcsCase.getEnforcementOrder()).isNull();
    }

    @Test
    void shouldDoNothingWithoutACase() {
        underTest.setRiskAssessmentTab(null, PCSCase.builder().build(), CASE_REFERENCE);
        underTest.setRiskAssessmentTab(pcsCaseEntity, null, CASE_REFERENCE);

        verifyNoInteractions(currentEnforcementService, bailiffNoteService);
    }

    @Test
    void shouldIgnoreAStaleTabFromADraftWhenTheApplicationIsAwaitingReview() {
        givenEnforcements(warrant(AWAITING_REVIEW));
        PCSCase pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .showRiskAssessmentTab(YesOrNo.YES)
                                  .riskAssessmentPrintMarkdown("stale")
                                  .riskAssessmentClaimantMarkdown("stale")
                                  .build())
            .build();

        underTest.setRiskAssessmentTab(pcsCaseEntity, pcsCase, CASE_REFERENCE);

        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        assertThat(enforcementOrder.getShowRiskAssessmentTab()).isEqualTo(YesOrNo.NO);
        assertThat(enforcementOrder.getRiskAssessmentPrintMarkdown()).isNull();
        assertThat(enforcementOrder.getRiskAssessmentClaimantMarkdown()).isNull();
    }

    private void givenEnforcements(EnforcementOrderEntity... enforcements) {
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCaseEntity)).thenReturn(List.of(enforcements));
    }

    private void signedInWith(String... roles) {
        lenient().when(userRoleService.getCurrentUserCaseRoles(CASE_REFERENCE))
            .thenReturn(new UserRoles("user-id", List.of(roles)));
    }

    private static EnforcementOrderEntity warrant(EnforcementStatus status) {
        return EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .status(status)
            .warrantDetails(WarrantEntity.builder().build())
            .build();
    }
}
