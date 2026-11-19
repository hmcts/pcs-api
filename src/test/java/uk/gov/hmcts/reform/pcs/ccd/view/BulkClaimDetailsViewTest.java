package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.BulkClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SubmissionChannel;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimPaymentHistoryLineEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageAdditionalPaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageParticularsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.BulkClaimDetailsRenderer;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkClaimDetailsViewTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private BulkClaimSubmissionRepository submissionRepository;
    @Mock
    private BulkClaimDetailsRenderer renderer;

    private BulkClaimDetailsView underTest;
    private PCSCase pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new BulkClaimDetailsView(submissionRepository, renderer, new MoneyFormatter());
        pcsCase = PCSCase.builder().build();
    }

    @Test
    void shouldLeaveFieldEmptyForUiClaim() {
        ClaimEntity claim = ClaimEntity.builder().submissionChannel(SubmissionChannel.UI).build();

        underTest.setCaseFields(pcsCase, caseWith(claim));

        assertThat(pcsCase.getBulkClaimDetailsMarkdown()).isNull();
        verifyNoInteractions(renderer, submissionRepository);
    }

    @Test
    void shouldLeaveFieldEmptyWhenNothingToShow() {
        ClaimEntity claim = ClaimEntity.builder().submissionChannel(SubmissionChannel.BULK_API).build();
        when(submissionRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        underTest.setCaseFields(pcsCase, caseWith(claim));

        assertThat(pcsCase.getBulkClaimDetailsMarkdown()).isNull();
        verify(renderer, never()).render(any());
    }

    @Test
    void shouldBuildRentalDetails() {
        ClaimEntity claim = bulkClaim();
        claim.setClaimantDesignation("Landlord");
        claim.setLegacyPcolClaimantReference("PCOL-C");
        claim.setSolicitorFirmName("Firm LLP");
        claim.setClaimCosts(YesOrNo.YES);
        claim.addPaymentHistoryLine(ClaimPaymentHistoryLineEntity.builder()
            .rank(2).paymentDueDate(LocalDate.of(2026, 2, 1)).amountDue(new BigDecimal("500.00"))
            .amountPaid(BigDecimal.ZERO).cumulativeArrears(new BigDecimal("900.50")).build());
        claim.addPaymentHistoryLine(ClaimPaymentHistoryLineEntity.builder()
            .rank(1).paymentDueDate(LocalDate.of(2026, 1, 1)).amountDue(new BigDecimal("500.00"))
            .amountPaid(new BigDecimal("100.00")).cumulativeArrears(new BigDecimal("400.00")).build());
        PartyEntity claimant = PartyEntity.builder().firstName("Jane").lastName("Doe").title("Ms")
            .dxNumber("DX 1").isOccupant(YesOrNo.NO).build();
        claim.getClaimParties().add(ClaimPartyEntity.builder().role(PartyRole.CLAIMANT).party(claimant).build());
        PartyEntity plainDefendant = PartyEntity.builder().firstName("Bob").lastName("Roe").build();
        claim.getClaimParties().add(ClaimPartyEntity.builder().role(PartyRole.DEFENDANT).party(plainDefendant).build());
        when(submissionRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(
            BulkClaimSubmissionEntity.builder().customerUniqueIdentifier("CUST-1").schemaVersion("2.0").build()));
        when(renderer.render(any())).thenReturn("<html/>");

        underTest.setCaseFields(pcsCase, caseWith(claim));

        BulkClaimDetails details = captureDetails();
        assertThat(pcsCase.getBulkClaimDetailsMarkdown()).isEqualTo("<html/>");
        assertThat(details.submission()).extracting(BulkClaimDetails.Row::label, BulkClaimDetails.Row::value)
            .containsExactly(
                tuple("Customer unique identifier", "CUST-1"),
                tuple("Schema version", "2.0"),
                tuple("PCOL claimant reference", "PCOL-C"),
                tuple("Claimant designation", "Landlord"),
                tuple("Solicitor firm", "Firm LLP"),
                tuple("Claiming costs", "Yes"));
        assertThat(details.parties()).hasSize(1);
        assertThat(details.parties().getFirst().heading()).isEqualTo("Claimant: Jane Doe");
        assertThat(details.parties().getFirst().rows()).extracting(BulkClaimDetails.Row::value)
            .containsExactly("Ms", "DX 1", "No");
        assertThat(details.paymentHistory()).extracting(BulkClaimDetails.PaymentLine::dueDate)
            .containsExactly("1 January 2026", "1 February 2026");
        assertThat(details.paymentHistory().getLast().cumulativeArrears()).isEqualTo("£900.50");
        assertThat(details.paymentHistory().getFirst().amountDue()).isEqualTo("£500");
        assertThat(details.mortgage()).isEmpty();
    }

    @Test
    void shouldBuildMortgageDetails() {
        ClaimEntity claim = bulkClaim();
        MortgageParticularsEntity mortgage = MortgageParticularsEntity.builder()
            .mortgageDate(LocalDate.of(2020, 5, 4)).loanAmount(new BigDecimal("150000"))
            .startInterestRate(new BigDecimal("4.50")).moneyJudgmentWanted(YesOrNo.NO)
            .originalProviderName("First Lender").originalProviderBoughtDate(LocalDate.of(2021, 3, 2))
            .build();
        mortgage.addAdditionalPayment(MortgageAdditionalPaymentEntity.builder()
            .rank(1).amount(new BigDecimal("25")).reason("Insurance").arrears(new BigDecimal("10.25")).build());
        claim.setMortgageParticulars(mortgage);
        when(submissionRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());
        when(renderer.render(any())).thenReturn("<html/>");

        underTest.setCaseFields(pcsCase, caseWith(claim));

        BulkClaimDetails details = captureDetails();
        assertThat(details.submission()).isEmpty();
        assertThat(details.mortgage()).extracting(BulkClaimDetails.Row::label, BulkClaimDetails.Row::value)
            .containsExactly(
                tuple("Mortgage date", "4 May 2020"),
                tuple("Loan amount", "£150000"),
                tuple("Start interest rate", "4.5%"),
                tuple("Money judgment wanted", "No"));
        assertThat(details.additionalPayments()).singleElement()
            .satisfies(payment -> {
                assertThat(payment.amount()).isEqualTo("£25");
                assertThat(payment.reason()).isEqualTo("Insurance");
                assertThat(payment.arrears()).isEqualTo("£10.25");
            });
        assertThat(details.originalProvider()).extracting(BulkClaimDetails.Row::value)
            .containsExactly("First Lender", "2 March 2021");
    }

    private ClaimEntity bulkClaim() {
        return ClaimEntity.builder().submissionChannel(SubmissionChannel.BULK_API).build();
    }

    private PcsCaseEntity caseWith(ClaimEntity claim) {
        return PcsCaseEntity.builder().caseReference(CASE_REFERENCE).claims(List.of(claim)).build();
    }

    private BulkClaimDetails captureDetails() {
        ArgumentCaptor<BulkClaimDetails> captor = ArgumentCaptor.forClass(BulkClaimDetails.class);
        verify(renderer).render(captor.capture());
        return captor.getValue();
    }
}
