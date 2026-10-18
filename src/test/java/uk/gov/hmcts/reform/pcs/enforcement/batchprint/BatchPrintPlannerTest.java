package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffAllocationEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffAllocationRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.enforcement.batchprint.BatchPrintDocumentKind.NOTICE_OF_EVICTION;
import static uk.gov.hmcts.reform.pcs.enforcement.batchprint.BatchPrintDocumentKind.RISK_ASSESSMENT;
import static uk.gov.hmcts.reform.pcs.enforcement.batchprint.BatchPrintDocumentKind.WARRANT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;

@ExtendWith(MockitoExtension.class)
class BatchPrintPlannerTest {

    private static final String BAILIFF = "bob-id";
    private static final String OTHER_BAILIFF = "alex-id";
    private static final long CASE_ONE = 1111111111111111L;
    private static final long CASE_TWO = 2222222222222222L;
    private static final Instant MONDAY = Instant.parse("2026-10-12T09:00:00Z");

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private BailiffAllocationService bailiffAllocationService;
    @Mock
    private BailiffAllocationRepository bailiffAllocationRepository;

    private BatchPrintPlanner underTest;

    @BeforeEach
    void setUp() {
        underTest = new BatchPrintPlanner(pcsCaseRepository, currentEnforcementService, bailiffAllocationService,
                                          bailiffAllocationRepository);
    }

    @Test
    void shouldPrintTheWarrantRiskAssessmentAndNoticeOfEvictionInThatOrder() {
        PcsCaseEntity pcsCase = givenCase(CASE_ONE);
        EnforcementOrderEntity warrant = givenWarrant(pcsCase, CLAIMANT_CONFIRMED, "WP-00000001", BAILIFF);
        DocumentEntity notice = enforcementDocument(pcsCase, DocumentType.NOTICE_OF_EVICTION, "notice.pdf", 3);
        DocumentEntity risk = enforcementDocument(pcsCase, DocumentType.RISK_ASSESSMENT, "risk.pdf", 2);
        DocumentEntity warrantDocument = enforcementDocument(pcsCase, DocumentType.WARRANT, "warrant.pdf", 1);
        givenEnforcements(pcsCase, warrant);

        BatchPrintPlan plan = underTest.plan(BAILIFF, List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.excluded()).isEmpty();
        assertThat(plan.included()).singleElement().satisfies(printable -> {
            assertThat(printable.caseReference()).isEqualTo(String.valueOf(CASE_ONE));
            assertThat(printable.warrantNumber()).isEqualTo("WP-00000001");
            assertThat(printable.documents()).extracting(PrintableDocument::kind)
                .containsExactly(WARRANT, RISK_ASSESSMENT, NOTICE_OF_EVICTION);
            assertThat(printable.documents()).extracting(PrintableDocument::documentId)
                .containsExactly(warrantDocument.getDocumentId(), risk.getDocumentId(), notice.getDocumentId());
            assertThat(printable.missingDocuments()).isEmpty();
        });
    }

    @Test
    void shouldPrintOnlyTheLatestDocumentOfEachKind() {
        PcsCaseEntity pcsCase = givenCase(CASE_ONE);
        givenEnforcements(pcsCase, givenWarrant(pcsCase, CLAIMANT_CONFIRMED, "WP-00000001", BAILIFF));
        enforcementDocument(pcsCase, DocumentType.NOTICE_OF_EVICTION, "rescheduled notice.pdf", 5);
        enforcementDocument(pcsCase, DocumentType.NOTICE_OF_EVICTION, "original notice.pdf", 1);

        BatchPrintPlan plan = underTest.plan(BAILIFF, List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.included()).singleElement().satisfies(printable -> {
            assertThat(printable.documents()).extracting(PrintableDocument::fileName)
                .containsExactly("rescheduled notice.pdf");
            assertThat(printable.missingDocuments()).containsExactly(WARRANT, RISK_ASSESSMENT);
        });
    }

    @Test
    void shouldIgnoreDocumentsOutsideTheEnforcementFolderAndOfOtherTypes() {
        PcsCaseEntity pcsCase = givenCase(CASE_ONE);
        givenEnforcements(pcsCase, givenWarrant(pcsCase, CLAIMANT_CONFIRMED, "WP-00000001", BAILIFF));
        DocumentEntity elsewhere = enforcementDocument(pcsCase, DocumentType.WARRANT, "uncategorised.pdf", 1);
        elsewhere.setCategoryId("uncategorisedDocuments");
        enforcementDocument(pcsCase, DocumentType.POLICE_REPORT, "police.pdf", 2);

        BatchPrintPlan plan = underTest.plan(BAILIFF, List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.included()).isEmpty();
        assertThat(plan.excluded()).containsExactly(
            ExcludedCase.of(String.valueOf(CASE_ONE), BatchPrintExclusion.NO_DOCUMENTS));
    }

    @Test
    void shouldIgnoreDocumentsFiledAgainstAnEarlierEnforcement() {
        PcsCaseEntity pcsCase = givenCase(CASE_ONE);
        EnforcementOrderEntity current = givenWarrant(pcsCase, CLAIMANT_CONFIRMED, "WP-00000002", BAILIFF);
        EnforcementOrderEntity earlier = warrant(pcsCase, CLOSED, "WP-00000001");
        DocumentEntity earlierNotice = enforcementDocument(pcsCase, DocumentType.NOTICE_OF_EVICTION, "old.pdf", 9);
        earlier.addDocuments(List.of(earlierNotice));
        DocumentEntity currentWarrant = enforcementDocument(pcsCase, DocumentType.WARRANT, "warrant.pdf", 1);
        current.addDocuments(List.of(currentWarrant));
        givenEnforcements(pcsCase, current, earlier);

        BatchPrintPlan plan = underTest.plan(BAILIFF, List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.included()).singleElement().satisfies(printable ->
            assertThat(printable.documents()).extracting(PrintableDocument::fileName).containsExactly("warrant.pdf"));
    }

    @Test
    void shouldLeaveOutACaseAllocatedToAnotherBailiff() {
        PcsCaseEntity pcsCase = givenCase(CASE_ONE);
        givenEnforcements(pcsCase, givenWarrant(pcsCase, CLAIMANT_CONFIRMED, "WP-00000001", OTHER_BAILIFF));
        enforcementDocument(pcsCase, DocumentType.WARRANT, "warrant.pdf", 1);

        BatchPrintPlan plan = underTest.plan(BAILIFF, List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.included()).isEmpty();
        assertThat(plan.excluded()).singleElement().satisfies(excluded -> {
            assertThat(excluded.code()).isEqualTo(BatchPrintExclusion.NOT_ALLOCATED);
            assertThat(excluded.reason()).isEqualTo("You are not the allocated bailiff on this case");
        });
    }

    @Test
    void shouldLeaveOutACaseWithNoAllocatedBailiff() {
        PcsCaseEntity pcsCase = givenCase(CASE_ONE);
        EnforcementOrderEntity warrant = warrant(pcsCase, AWAITING_CLAIMANT_CONFIRMATION, "WP-00000001");
        when(bailiffAllocationService.findActiveAllocation(warrant)).thenReturn(Optional.empty());
        givenEnforcements(pcsCase, warrant);

        BatchPrintPlan plan = underTest.plan(BAILIFF, List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.excluded()).extracting(ExcludedCase::code).containsExactly(BatchPrintExclusion.NOT_ALLOCATED);
    }

    @Test
    void shouldGiveACaseThatDoesNotExistTheSameReasonAsOneThatIsNotAllocated() {
        when(pcsCaseRepository.findByCaseReference(CASE_ONE)).thenReturn(Optional.empty());

        BatchPrintPlan plan = underTest.plan(BAILIFF, List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.excluded()).extracting(ExcludedCase::code).containsExactly(BatchPrintExclusion.NOT_ALLOCATED);
    }

    @Test
    void shouldLeaveOutACaseWithNoEnforcement() {
        PcsCaseEntity pcsCase = givenCase(CASE_ONE);
        givenEnforcements(pcsCase);

        BatchPrintPlan plan = underTest.plan(BAILIFF, List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.excluded()).extracting(ExcludedCase::code).containsExactly(BatchPrintExclusion.NOT_ALLOCATED);
    }

    @Test
    void shouldLeaveOutAnAllocatedEnforcementThatIsNotALiveWarrant() {
        PcsCaseEntity pcsCase = givenCase(CASE_ONE);
        EnforcementOrderEntity writ = EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .status(TRANSFERRED_TO_HIGH_COURT)
            .build();
        when(bailiffAllocationService.findActiveAllocation(writ)).thenReturn(Optional.of(allocation(writ, BAILIFF)));
        givenEnforcements(pcsCase, writ);

        BatchPrintPlan plan = underTest.plan(BAILIFF, List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.excluded()).extracting(ExcludedCase::code)
            .containsExactly(BatchPrintExclusion.NO_LIVE_WARRANT);
    }

    @Test
    void shouldKeepTheChosenOrderAcceptDashesAndPrintARepeatedCaseOnce() {
        PcsCaseEntity first = givenCase(CASE_ONE);
        givenEnforcements(first, givenWarrant(first, CLAIMANT_CONFIRMED, "WP-00000001", BAILIFF));
        enforcementDocument(first, DocumentType.WARRANT, "one.pdf", 1);
        PcsCaseEntity second = givenCase(CASE_TWO);
        givenEnforcements(second, givenWarrant(second, CLAIMANT_CONFIRMED, "WP-00000002", BAILIFF));
        enforcementDocument(second, DocumentType.WARRANT, "two.pdf", 1);

        BatchPrintPlan plan = underTest.plan(
            BAILIFF, List.of("2222-2222-2222-2222", "1111 1111 1111 1111", String.valueOf(CASE_TWO), "12-34"));

        assertThat(plan.included()).extracting(PrintableCase::caseReference)
            .containsExactly(String.valueOf(CASE_TWO), String.valueOf(CASE_ONE));
        assertThat(plan.excluded()).containsExactly(ExcludedCase.of("1234", BatchPrintExclusion.INVALID_REFERENCE));
    }

    @Test
    void shouldMatchTheBailiffIgnoringCase() {
        PcsCaseEntity pcsCase = givenCase(CASE_ONE);
        givenEnforcements(pcsCase, givenWarrant(pcsCase, CLAIMANT_CONFIRMED, "WP-00000001", "BOB-ID"));
        enforcementDocument(pcsCase, DocumentType.WARRANT, "warrant.pdf", 1);

        BatchPrintPlan plan = underTest.plan("Bob-Id", List.of(String.valueOf(CASE_ONE)));

        assertThat(plan.included()).hasSize(1);
    }

    @Test
    void shouldListTheBailiffsLiveWarrantsSoonestEvictionFirstWithinTheRange() {
        EnforcementOrderEntity later = listedWarrant(CASE_ONE, CLAIMANT_CONFIRMED,
                                                     LocalDateTime.of(2026, 10, 16, 10, 0));
        EnforcementOrderEntity sooner = listedWarrant(CASE_TWO, AWAITING_CLAIMANT_CONFIRMATION,
                                                      LocalDateTime.of(2026, 10, 13, 9, 30));
        EnforcementOrderEntity nextWeek = listedWarrant(3333333333333333L, CLAIMANT_CONFIRMED,
                                                        LocalDateTime.of(2026, 10, 20, 9, 0));
        EnforcementOrderEntity closed = listedWarrant(4444444444444444L, CLOSED, LocalDateTime.of(2026, 10, 14, 9, 0));
        when(bailiffAllocationRepository.findActiveByBailiffUserId("bob-id")).thenReturn(List.of(
            allocation(later, BAILIFF), allocation(sooner, BAILIFF), allocation(nextWeek, BAILIFF),
            allocation(closed, BAILIFF)));

        List<AllocatedWarrant> warrants = underTest.allocatedWarrants(
            "BOB-ID", LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18));

        assertThat(warrants).extracting(AllocatedWarrant::caseReference)
            .containsExactly(String.valueOf(CASE_TWO), String.valueOf(CASE_ONE));
        assertThat(warrants.getFirst().evictionDate()).isEqualTo(LocalDateTime.of(2026, 10, 13, 9, 30));
        assertThat(warrants.getFirst().status()).isEqualTo(AWAITING_CLAIMANT_CONFIRMATION);
    }

    @Test
    void shouldListUnscheduledWarrantsLastWhenThereIsNoRange() {
        EnforcementOrderEntity unscheduled = listedWarrant(CASE_ONE, EnforcementStatus.AWAITING_SCHEDULING, null);
        EnforcementOrderEntity scheduled = listedWarrant(CASE_TWO, CLAIMANT_CONFIRMED,
                                                         LocalDateTime.of(2026, 10, 13, 9, 30));
        when(bailiffAllocationRepository.findActiveByBailiffUserId("bob-id"))
            .thenReturn(List.of(allocation(unscheduled, BAILIFF), allocation(scheduled, BAILIFF)));

        assertThat(underTest.allocatedWarrants(BAILIFF, null, null)).extracting(AllocatedWarrant::caseReference)
            .containsExactly(String.valueOf(CASE_TWO), String.valueOf(CASE_ONE));
        assertThat(underTest.allocatedWarrants(BAILIFF, LocalDate.of(2026, 10, 1), null))
            .extracting(AllocatedWarrant::caseReference).containsExactly(String.valueOf(CASE_TWO));
    }

    private PcsCaseEntity givenCase(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseReference(caseReference)
            .documents(new ArrayList<>())
            .claims(new ArrayList<>())
            .build();
        ClaimEntity claim = ClaimEntity.builder().id(UUID.randomUUID()).pcsCase(pcsCase).build();
        pcsCase.getClaims().add(claim);
        when(pcsCaseRepository.findByCaseReference(caseReference)).thenReturn(Optional.of(pcsCase));
        return pcsCase;
    }

    private void givenEnforcements(PcsCaseEntity pcsCase, EnforcementOrderEntity... currentFirst) {
        when(currentEnforcementService.findEnforcementsCurrentFirst(pcsCase)).thenReturn(List.of(currentFirst));
    }

    private EnforcementOrderEntity givenWarrant(PcsCaseEntity pcsCase, EnforcementStatus status, String number,
                                                String bailiffUserId) {
        EnforcementOrderEntity warrant = warrant(pcsCase, status, number);
        when(bailiffAllocationService.findActiveAllocation(warrant))
            .thenReturn(Optional.of(allocation(warrant, bailiffUserId)));
        return warrant;
    }

    private static EnforcementOrderEntity warrant(PcsCaseEntity pcsCase, EnforcementStatus status, String number) {
        EnforcementOrderEntity warrant = EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .claim(pcsCase.getClaims().getFirst())
            .status(status)
            .enforcementNumber(number)
            .build();
        warrant.setWarrantDetails(WarrantEntity.builder().build());
        return warrant;
    }

    private static EnforcementOrderEntity listedWarrant(long caseReference, EnforcementStatus status,
                                                        LocalDateTime evictionDate) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        EnforcementOrderEntity warrant = EnforcementOrderEntity.builder()
            .id(UUID.randomUUID())
            .claim(ClaimEntity.builder().pcsCase(pcsCase).build())
            .status(status)
            .enforcementNumber("WP-" + caseReference)
            .bailiffDate(evictionDate)
            .build();
        warrant.setWarrantDetails(WarrantEntity.builder().build());
        return warrant;
    }

    private static BailiffAllocationEntity allocation(EnforcementOrderEntity enforcement, String bailiffUserId) {
        return BailiffAllocationEntity.builder()
            .enforcementOrder(enforcement)
            .bailiffUserId(bailiffUserId)
            .startedAt(MONDAY)
            .build();
    }

    private static DocumentEntity enforcementDocument(PcsCaseEntity pcsCase, DocumentType type, String fileName,
                                                      int minutesAfterMonday) {
        DocumentEntity document = DocumentEntity.builder()
            .id(UUID.randomUUID())
            .documentId(UUID.randomUUID())
            .type(type)
            .fileName(fileName)
            .categoryId("enforcement")
            .submittedDate(MONDAY.plusSeconds(60L * minutesAfterMonday))
            .build();
        pcsCase.getDocuments().add(document);
        return document;
    }
}
