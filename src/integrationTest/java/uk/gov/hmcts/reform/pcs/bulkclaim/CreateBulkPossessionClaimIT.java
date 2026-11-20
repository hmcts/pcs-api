package uk.gov.hmcts.reform.pcs.bulkclaim;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimKind;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.AdditionalPayment;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.MortgageParticulars;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.Occupant;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.PartyExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.PaymentHistoryLine;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimStagingSerializer;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.SubmissionChannel;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.task.CaseRoleAssignmentTaskComponent;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createBulkPossessionClaim;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEES_AND_PAY_TASK_DESCRIPTOR;

@DisplayName("Create a bulk possession claim")
@PcsCcdEventTest
@TestPropertySource(properties = {"db-scheduler.executor-enabled=false", "testing-support.enabled=false"})
class CreateBulkPossessionClaimIT extends AbstractPostgresContainerIT {

    private static final String ORGANISATION_ID = "BULKORG1";
    private static final String FEE_TASK = FEES_AND_PAY_TASK_DESCRIPTOR.getTaskName();
    private static final String ROLE_TASK =
        CaseRoleAssignmentTaskComponent.ROLE_ASSIGNMENT_TASK_DESCRIPTOR.getTaskName();

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private BulkClaimSubmissionRepository submissions;
    @Autowired
    private BulkClaimStagingSerializer serializer;
    @Autowired
    private PcsCaseRepository pcsCases;
    @Autowired
    private TransactionTemplate transaction;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private FeeService feeService;

    private Actor systemUser;

    @BeforeEach
    void setUp() {
        systemUser = events.registerActor("Pcs", "System", "pcs-system-update");
        when(feeService.getFee(any(FeeType.class)))
            .thenReturn(FeeDetails.builder().code("FEE0001").feeAmount(new BigDecimal("404.00")).build());
        jdbcTemplate.update("DELETE FROM scheduled_tasks");
    }

    @AfterEach
    void dropScheduledTasks() {
        jdbcTemplate.update("DELETE FROM scheduled_tasks");
    }

    @Test
    @DisplayName("creates the case, the claim with its bulk data and the fee task, and marks the row created")
    void createsTheCaseFromTheStagedClaim() {
        UUID id = stage();

        var creation = events.create(createBulkPossessionClaim.name(), State.PENDING_CASE_ISSUED,
                                     PCSCase.builder().bulkClaimSubmissionId(id.toString()).build())
            .as(systemUser);
        CcdEventTestSupport<PCSCase, State>.Accepted accepted = creation.submitExpectingSuccess();
        long caseReference = creation.reference();

        assertThat(accepted.state()).isEqualTo(State.PENDING_CASE_ISSUED);

        BulkClaimSubmissionEntity row = submissions.findById(id).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(BulkClaimSubmissionStatus.CASE_CREATED);
        assertThat(row.getCaseReference()).isEqualTo(caseReference);

        transaction.executeWithoutResult(status -> assertStoredCase(caseReference));
        assertThat(scheduledTasks(FEE_TASK)).isEqualTo(1);
        assertThat(scheduledTasks(ROLE_TASK)).isEqualTo(1);
    }

    @Test
    @DisplayName("refuses a replay of an already created submission and creates nothing more")
    void refusesAReplay() {
        UUID id = stage();
        events.create(createBulkPossessionClaim.name(), State.PENDING_CASE_ISSUED,
                      PCSCase.builder().bulkClaimSubmissionId(id.toString()).build())
            .as(systemUser).submitExpectingSuccess();
        long casesBefore = pcsCases.count();
        int feeTasksBefore = scheduledTasks(FEE_TASK);

        var replay = events.create(createBulkPossessionClaim.name(), State.PENDING_CASE_ISSUED,
                                   PCSCase.builder().bulkClaimSubmissionId(id.toString()).build())
            .as(systemUser).submitExpectingErrors();

        assertThat(replay.errors()).singleElement().asString()
            .startsWith("BULK|6|bulkClaimSubmissionId|");
        assertThat(pcsCases.count()).isEqualTo(casesBefore);
        assertThat(scheduledTasks(FEE_TASK)).isEqualTo(feeTasksBefore);
        assertThat(submissions.findById(id).orElseThrow().getStatus())
            .isEqualTo(BulkClaimSubmissionStatus.CASE_CREATED);
    }

    @Test
    @DisplayName("refuses an unknown submission")
    void refusesAnUnknownSubmission() {
        long casesBefore = pcsCases.count();

        var rejected = events.create(createBulkPossessionClaim.name(), State.PENDING_CASE_ISSUED,
                                     PCSCase.builder().bulkClaimSubmissionId(UUID.randomUUID().toString()).build())
            .as(systemUser).submitExpectingErrors();

        assertThat(rejected.errors()).singleElement().asString().startsWith("BULK|6|bulkClaimSubmissionId|");
        assertThat(pcsCases.count()).isEqualTo(casesBefore);
    }

    @Test
    @DisplayName("rolls back the case already written when a later step fails")
    void rollsBackEverythingWhenAStagedFieldCannotBeRead() {
        UUID id = stage();
        BulkClaimSubmissionEntity row = submissions.findById(id).orElseThrow();
        row.setStagedExtras("[]");
        submissions.save(row);
        long casesBefore = pcsCases.count();

        var rejected = events.create(createBulkPossessionClaim.name(), State.PENDING_CASE_ISSUED,
                                     PCSCase.builder().bulkClaimSubmissionId(id.toString()).build())
            .as(systemUser).submitExpectingErrors();

        assertThat(rejected.errors()).singleElement().asString().startsWith("BULK|");
        assertThat(pcsCases.count()).isEqualTo(casesBefore);
        assertThat(scheduledTasks(FEE_TASK)).isZero();
        assertThat(submissions.findById(id).orElseThrow().getStatus())
            .isEqualTo(BulkClaimSubmissionStatus.PENDING);
    }

    private void assertStoredCase(long caseReference) {
        PcsCaseEntity pcsCase = pcsCases.findByCaseReference(caseReference).orElseThrow();
        ClaimEntity claim = pcsCase.getMainClaim();

        assertThat(claim.getSubmissionChannel()).isEqualTo(SubmissionChannel.BULK_API);
        assertThat(claim.getClaimantDesignation()).isEqualTo("Landlord");
        assertThat(claim.getLegacyPcolClaimantReference()).isEqualTo("PCOL-C-1");
        assertThat(claim.getLegacyPcolSolicitorReference()).isEqualTo("PCOL-S-1");
        assertThat(claim.getSolicitorFirmName()).isEqualTo("Firm LLP");
        assertThat(claim.getClaimCosts()).isEqualTo(YesOrNo.YES);
        assertThat(claim.getClaimantOwnAddress().getAddressLine1()).isEqualTo("9 Own Street");
        assertThat(claim.getClaimantOwnTelephoneNumber()).isEqualTo("01482000000");
        assertThat(claim.getClaimantOwnEmailAddress()).isEqualTo("own@example.org");

        assertThat(claim.getPaymentHistoryLines())
            .extracting("rank", "amountDue")
            .containsExactly(org.assertj.core.groups.Tuple.tuple(1, new BigDecimal("100.00")),
                             org.assertj.core.groups.Tuple.tuple(2, new BigDecimal("110.00")));
        assertThat(claim.getOccupants()).singleElement().satisfies(occupant -> {
            assertThat(occupant.getLastName()).isEqualTo("Occupier");
            assertThat(occupant.getRank()).isEqualTo(1);
        });
        assertThat(claim.getMortgageParticulars().getLoanAmount()).isEqualByComparingTo("150000.00");
        assertThat(claim.getMortgageParticulars().getMoneyJudgmentWanted()).isEqualTo(YesOrNo.NO);
        assertThat(claim.getMortgageParticulars().getAdditionalPayments()).singleElement()
            .satisfies(payment -> assertThat(payment.getReason()).isEqualTo("Insurance"));

        assertThat(partyWithRole(claim, PartyRole.CLAIMANT).getDxNumber()).isEqualTo("DX 1");
        PartyEntity defendant = partyWithRole(claim, PartyRole.DEFENDANT);
        assertThat(defendant.getTitle()).isEqualTo("Mr");
        assertThat(defendant.getIsOccupant()).isEqualTo(YesOrNo.YES);
    }

    private static PartyEntity partyWithRole(ClaimEntity claim, PartyRole role) {
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .min(Comparator.comparing(ClaimPartyEntity::getRank))
            .orElseThrow()
            .getParty();
    }

    private int scheduledTasks(String taskName) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM scheduled_tasks WHERE task_name = ?",
                                           Integer.class, taskName);
    }

    private UUID stage() {
        PCSCase pcsCase = cases.claim(LegislativeCountry.ENGLAND);
        OrganisationDetailsResponse organisation = OrganisationDetailsResponse.builder()
            .organisationIdentifier(ORGANISATION_ID)
            .name("Bulk Org")
            .status("ACTIVE")
            .organisationProfileIds(List.of("SOLICITOR_PROFILE"))
            .paymentAccount(List.of("PBA1234567"))
            .build();
        BulkClaimExtras extras = new BulkClaimExtras(
            "2.0", "Landlord", "PCOL-C-1", "PCOL-S-1", "Firm LLP", true, null,
            new PartyExtras(null, null, null, "DX 1", "0207", null, null, null),
            List.of(new PartyExtras("Mr", "J", "Quentin", null, null, true, null, null)),
            List.of(new PaymentHistoryLine(LocalDate.of(2026, 1, 1), new BigDecimal("100.00"),
                                           new BigDecimal("0.00"), new BigDecimal("100.00")),
                    new PaymentHistoryLine(LocalDate.of(2026, 2, 1), new BigDecimal("110.00"),
                                           new BigDecimal("0.00"), new BigDecimal("210.00"))),
            new MortgageParticulars(
                LocalDate.of(2020, 1, 1), null, new BigDecimal("150000.00"), "Monthly", LocalDate.of(2045, 1, 1),
                new BigDecimal("160000.00"), new BigDecimal("500.00"), null, new BigDecimal("4.5000"),
                new BigDecimal("4.5000"), new BigDecimal("5.0000"), null, null, null, null, null, false, null,
                null, null,
                                    List.of(new AdditionalPayment(new BigDecimal("12.00"), "Insurance",
                                                                  new BigDecimal("12.00")))),
            List.of(new Occupant("Mrs", "Olive", "Occupier", null, "O")),
            new BulkClaimExtras.ClaimantOwnContact(
                AddressUK.builder().addressLine1("9 Own Street").postTown("Hull").postCode("HU1 1AA").build(),
                "01482000000", "own@example.org"));

        BulkClaimSubmissionEntity row = BulkClaimSubmissionEntity.builder()
            .organisationId(ORGANISATION_ID)
            .customerUniqueIdentifier("IT-" + UUID.randomUUID())
            .claimKind(BulkClaimKind.RENTAL)
            .requestHash("hash")
            .status(BulkClaimSubmissionStatus.PENDING)
            .stagedCaseData(serializer.writeCase(pcsCase))
            .stagedExtras(serializer.writeExtras(extras))
            .organisationSnapshot(serializer.writeOrganisation(organisation))
            .pbaAccountNumber("PBA1234567")
            .paymentCustomerReference("REF-1")
            .build();
        return submissions.save(row).getId();
    }
}
