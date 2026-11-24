package uk.gov.hmcts.reform.pcs.bulkclaim;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimKind;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimPaymentStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.SubmissionChannel;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimOccupantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimPaymentHistoryLineEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageAdditionalPaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageParticularsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class BulkClaimSchemaIT extends AbstractPostgresContainerIT {

    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private BulkClaimSubmissionRepository submissionRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsBulkClaimChildrenAndPartyExtras() {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(1781000000000001L);

        ClaimEntity claim = ClaimEntity.builder()
            .submissionChannel(SubmissionChannel.BULK_API)
            .claimantDesignation("Lender")
            .legacyPcolClaimantReference("CLAIMANT-REF")
            .legacyPcolSolicitorReference("SOL-REF")
            .solicitorFirmName("Firm LLP")
            .claimCosts(YesOrNo.YES)
            .build();
        caseEntity.addClaim(claim);

        PartyEntity party = PartyEntity.builder()
            .firstName("Jane")
            .lastName("Smith")
            .title("Mrs")
            .initials("J")
            .otherNames("Anne")
            .dxNumber("DX 123")
            .faxNumber("0123456789")
            .isOccupant(YesOrNo.YES)
            .build();
        caseEntity.addParty(party);
        claim.addParty(party, PartyRole.DEFENDANT);

        claim.addPaymentHistoryLine(historyLine(2, "200.00"));
        claim.addPaymentHistoryLine(historyLine(1, "100.00"));

        MortgageParticularsEntity mortgage = MortgageParticularsEntity.builder()
            .mortgageDate(LocalDate.of(2020, 1, 1))
            .loanAmount(new BigDecimal("150000.00"))
            .currentRepaymentTerms("Monthly")
            .fullRepaymentDate(LocalDate.of(2045, 1, 1))
            .fullRepaymentAmount(new BigDecimal("250000.00"))
            .costsAndAdministration(new BigDecimal("500.00"))
            .startInterestRate(new BigDecimal("3.5000"))
            .preArrearsInterestRate(new BigDecimal("4.2500"))
            .claimStartInterestRate(new BigDecimal("5.0000"))
            .moneyJudgmentWanted(YesOrNo.NO)
            .build();
        mortgage.addAdditionalPayment(additionalPayment(2, "20.00", "Insurance"));
        mortgage.addAdditionalPayment(additionalPayment(1, "10.00", "Fees"));
        claim.setMortgageParticulars(mortgage);

        claim.addOccupant(occupant(2, "Bob"));
        claim.addOccupant(occupant(1, "Alice"));

        PcsCaseEntity saved = pcsCaseRepository.saveAndFlush(caseEntity);
        UUID claimId = saved.getClaims().getFirst().getId();
        entityManager.clear();

        ClaimEntity reloaded = entityManager.find(ClaimEntity.class, claimId);

        assertThat(reloaded.getSubmissionChannel()).isEqualTo(SubmissionChannel.BULK_API);
        assertThat(reloaded.getClaimantDesignation()).isEqualTo("Lender");
        assertThat(reloaded.getLegacyPcolClaimantReference()).isEqualTo("CLAIMANT-REF");
        assertThat(reloaded.getLegacyPcolSolicitorReference()).isEqualTo("SOL-REF");
        assertThat(reloaded.getSolicitorFirmName()).isEqualTo("Firm LLP");
        assertThat(reloaded.getClaimCosts()).isEqualTo(YesOrNo.YES);

        assertThat(reloaded.getPaymentHistoryLines())
            .extracting(ClaimPaymentHistoryLineEntity::getRank, ClaimPaymentHistoryLineEntity::getAmountDue)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, new BigDecimal("100.00")),
                org.assertj.core.groups.Tuple.tuple(2, new BigDecimal("200.00")));

        MortgageParticularsEntity reloadedMortgage = reloaded.getMortgageParticulars();
        assertThat(reloadedMortgage.getClaim()).isSameAs(reloaded);
        assertThat(reloadedMortgage.getStartInterestRate()).isEqualByComparingTo("3.5");
        assertThat(reloadedMortgage.getMoneyJudgmentWanted()).isEqualTo(YesOrNo.NO);
        assertThat(reloadedMortgage.getAdditionalPayments())
            .extracting(MortgageAdditionalPaymentEntity::getRank, MortgageAdditionalPaymentEntity::getReason)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, "Fees"),
                org.assertj.core.groups.Tuple.tuple(2, "Insurance"));

        assertThat(reloaded.getOccupants())
            .extracting(ClaimOccupantEntity::getRank, ClaimOccupantEntity::getFirstName)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, "Alice"),
                org.assertj.core.groups.Tuple.tuple(2, "Bob"));

        PartyEntity reloadedParty = entityManager.find(PartyEntity.class, party.getId());
        assertThat(reloadedParty.getTitle()).isEqualTo("Mrs");
        assertThat(reloadedParty.getInitials()).isEqualTo("J");
        assertThat(reloadedParty.getOtherNames()).isEqualTo("Anne");
        assertThat(reloadedParty.getDxNumber()).isEqualTo("DX 123");
        assertThat(reloadedParty.getFaxNumber()).isEqualTo("0123456789");
        assertThat(reloadedParty.getIsOccupant()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void claimsDefaultToUiSubmissionChannel() {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(1781000000000002L);
        ClaimEntity claim = ClaimEntity.builder().build();
        caseEntity.addClaim(claim);

        pcsCaseRepository.saveAndFlush(caseEntity);
        UUID claimId = claim.getId();
        entityManager.clear();

        assertThat(entityManager.find(ClaimEntity.class, claimId).getSubmissionChannel())
            .isEqualTo(SubmissionChannel.UI);
    }

    @Test
    void findsSubmissionsForAnOrganisationByAnyOfSeveralIdentifiers() {
        submissionRepository.saveAndFlush(submission("ORG-IN-1", "CUST-A"));
        submissionRepository.saveAndFlush(submission("ORG-IN-1", "CUST-B"));
        submissionRepository.saveAndFlush(submission("ORG-IN-1", "CUST-C"));
        submissionRepository.saveAndFlush(submission("ORG-IN-2", "CUST-A"));
        entityManager.clear();

        List<BulkClaimSubmissionEntity> found = submissionRepository
            .findByOrganisationIdAndCustomerUniqueIdentifierIn("ORG-IN-1", Set.of("CUST-A", "CUST-C", "CUST-Z"));

        assertThat(found).extracting(BulkClaimSubmissionEntity::getOrganisationId)
            .containsOnly("ORG-IN-1");
        assertThat(found).extracting(BulkClaimSubmissionEntity::getCustomerUniqueIdentifier)
            .containsExactlyInAnyOrder("CUST-A", "CUST-C");
    }

    @Test
    void persistsSubmissionAndFindsItByKeys() {
        BulkClaimSubmissionEntity saved = submissionRepository.saveAndFlush(submission("ORG1", "CUST-1"));
        entityManager.clear();

        BulkClaimSubmissionEntity found = submissionRepository.findByOrganisationIdAndCustomerUniqueIdentifier(
            "ORG1", "CUST-1").orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getVersion()).isZero();
        assertThat(found.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.NOT_STARTED);
        assertThat(found.getStatus()).isEqualTo(BulkClaimSubmissionStatus.PENDING);
        assertThat(found.getStagedCaseData()).contains("\"a\"");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getLastModified()).isNotNull();
        assertThat(submissionRepository.findByIdForUpdate(saved.getId())).isPresent();
        assertThat(submissionRepository.findByIdForUpdate(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findsSubmissionByServiceRequestAndCaseReference() {
        BulkClaimSubmissionEntity entity = submission("ORG2", "CUST-2");
        entity.setServiceRequestReference("2026-SR-1");
        entity.setCaseReference(1781000000000003L);
        submissionRepository.saveAndFlush(entity);
        entityManager.clear();

        assertThat(submissionRepository.findByServiceRequestReference("2026-SR-1")).isPresent();
        assertThat(submissionRepository.findByCaseReference(1781000000000003L)).isPresent();
        assertThat(submissionRepository.findByCaseReference(1L)).isEmpty();
    }

    @Test
    void rejectsDuplicateCustomerIdentifierForOrganisation() {
        submissionRepository.saveAndFlush(submission("ORG3", "CUST-3"));

        assertThatThrownBy(() -> submissionRepository.saveAndFlush(submission("ORG3", "CUST-3")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameCustomerIdentifierForDifferentOrganisations() {
        submissionRepository.saveAndFlush(submission("ORG4", "CUST-4"));
        submissionRepository.saveAndFlush(submission("ORG5", "CUST-4"));

        assertThat(submissionRepository.count()).isGreaterThanOrEqualTo(2);
    }

    private static BulkClaimSubmissionEntity submission(String organisationId, String customerId) {
        return BulkClaimSubmissionEntity.builder()
            .organisationId(organisationId)
            .customerUniqueIdentifier(customerId)
            .claimKind(BulkClaimKind.RENTAL)
            .requestHash("hash")
            .status(BulkClaimSubmissionStatus.PENDING)
            .stagedCaseData("{\"a\": 1}")
            .build();
    }

    private static ClaimPaymentHistoryLineEntity historyLine(int rank, String amount) {
        return ClaimPaymentHistoryLineEntity.builder()
            .rank(rank)
            .paymentDueDate(LocalDate.of(2026, rank, 1))
            .amountDue(new BigDecimal(amount))
            .amountPaid(BigDecimal.ZERO)
            .cumulativeArrears(new BigDecimal(amount))
            .build();
    }

    private static MortgageAdditionalPaymentEntity additionalPayment(int rank, String amount, String reason) {
        return MortgageAdditionalPaymentEntity.builder()
            .rank(rank)
            .amount(new BigDecimal(amount))
            .reason(reason)
            .build();
    }

    private static ClaimOccupantEntity occupant(int rank, String firstName) {
        return ClaimOccupantEntity.builder()
            .rank(rank)
            .firstName(firstName)
            .lastName("Occupant")
            .build();
    }

}
