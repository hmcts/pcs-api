package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Party;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;



import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.FIRST_CLASS_POST;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.PERSONALLY_HANDED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency.MONTHLY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales.STANDARD_CONTRACT;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.OrderingJudge.newDraft;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.OrderingJudge.notesIn;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.OrderingJudge.recorded;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@PcsCcdEventTest
@DisplayName("Make an order")
class MakeOrderIT {

    private static final int CONFLICT = 409;

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;

    private OrderingJudge firstJudge;
    private OrderingJudge secondJudge;

    @BeforeEach
    void setUp() {
        long caseReference = cases.issue();
        firstJudge = OrderingJudge.onCase(events, "First", caseReference);
        secondJudge = OrderingJudge.onCase(events, "Second", caseReference);
    }

    @Test
    @DisplayName("shows the judge the property, parties and facts of the claim")
    void showsThePropertyPartiesAndFactsOfTheClaim() {
        PCSCase claim = cases.claim(ENGLAND);
        claim.getTenancyLicenceDetails().setTypeOfTenancyLicence(SECURE_TENANCY);
        claim.getTenancyLicenceDetails().setTenancyLicenceDate(LocalDate.of(2020, 2, 3));
        claim.setNoticeServed(YesOrNo.YES);
        claim.setNoticeServedDetails(NoticeServedDetails.builder()
            .serviceMethod(FIRST_CLASS_POST)
            .postedDate(LocalDate.of(2026, 8, 10))
            .build());
        claim.setRentDetails(RentDetails.builder().currentRent(new BigDecimal("750.00")).frequency(MONTHLY).build());
        claim.setRentArrears(RentArrearsSection.builder().total(new BigDecimal("1500.00")).build());
        claim.getSecureOrFlexiblePossessionGrounds()
            .setSecureOrFlexibleDiscretionaryGrounds(Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY));

        var context = judgeOn(cases.issue(claim)).opens().caseContext();

        assertThat(context.propertyAddress().getPostCode()).isEqualTo(claim.getPropertyAddress().getPostCode());
        assertThat(context.claimants()).extracting(Party::name).containsExactly(IssuedCases.CLAIMANT);
        assertThat(context.defendants()).extracting(Party::name).containsExactly(IssuedCases.DEFENDANT);
        var facts = context.caseFacts();
        assertThat(facts.tenancyType()).isEqualTo("SECURE_TENANCY");
        assertThat(facts.tenancyStartDate()).isEqualTo(LocalDate.of(2020, 2, 3));
        assertThat(facts.noticeDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(facts.currentRent()).isEqualByComparingTo("750.00");
        assertThat(facts.rentFrequency()).isEqualTo("MONTHLY");
        assertThat(facts.arrearsOnIssue()).isEqualByComparingTo("1500.00");
        assertThat(facts.groundsPleaded()).contains("Rent arrears");
    }

    @Test
    @DisplayName("shows the day a notice handed over in person was served")
    void showsTheDayANoticeWasHandedOver() {
        PCSCase claim = cases.claim(ENGLAND);
        claim.setNoticeServed(YesOrNo.YES);
        claim.setNoticeServedDetails(NoticeServedDetails.builder()
            .serviceMethod(PERSONALLY_HANDED)
            .handedOverDateTime(LocalDateTime.of(2026, 8, 11, 16, 30))
            .build());

        var facts = judgeOn(cases.issue(claim)).opens().caseContext().caseFacts();

        assertThat(facts.noticeDate()).isEqualTo(LocalDate.of(2026, 8, 11));
    }

    @Test
    @DisplayName("shows a Welsh claim's occupation contract as the tenancy")
    void showsTheWelshOccupationContractAsTheTenancy() {
        PCSCase claim = cases.claim(WALES);
        claim.getOccupationLicenceDetailsWales().setOccupationLicenceTypeWales(STANDARD_CONTRACT);
        claim.getOccupationLicenceDetailsWales().setLicenceStartDate(LocalDate.of(2024, 4, 5));

        var facts = judgeOn(cases.issue(claim)).opens().caseContext().caseFacts();

        assertThat(facts.tenancyType()).isEqualTo("STANDARD_CONTRACT");
        assertThat(facts.tenancyStartDate()).isEqualTo(LocalDate.of(2024, 4, 5));
    }

    @Test
    @DisplayName("gives each judge their own draft on the same case")
    void eachJudgeKeepsTheirOwnDraft() {
        firstJudge.startsDraft("first judge's notes");
        secondJudge.startsDraft("second judge's notes");

        assertThat(firstJudge.draftNotes()).isEqualTo("first judge's notes");
        assertThat(secondJudge.draftNotes()).isEqualTo("second judge's notes");
    }

    @Test
    @DisplayName("does not let a judge change another judge's draft")
    void rejectsAChangeToAnotherJudgesDraft() {
        firstJudge.startsDraft("first judge's notes");

        assertThat(secondJudge.triesToSave(firstJudge.draft(), "overwritten")).isEqualTo(CONFLICT);
        assertThat(firstJudge.draftNotes()).isEqualTo("first judge's notes");
    }

    @Test
    @DisplayName("does not let a judge start a second draft on the same case")
    void rejectsASecondDraftForTheSameJudge() {
        firstJudge.startsDraft("first draft");

        assertThat(firstJudge.triesToStartDraft("second draft")).isEqualTo(CONFLICT);
        assertThat(firstJudge.draftNotes()).isEqualTo("first draft");
    }

    @Test
    @DisplayName("keeps a saved draft as the judge's working order")
    void savesADraft() {
        firstJudge.startsDraft("first version");

        firstJudge.saves(firstJudge.draft(), "second version");

        assertThat(firstJudge.draftNotes()).isEqualTo("second version");
    }

    @Test
    @DisplayName("does not let an out-of-date copy of a draft overwrite a newer save")
    void rejectsAStaleChange() {
        firstJudge.startsDraft("first version");
        Order staleCopy = firstJudge.draft();
        firstJudge.saves(staleCopy, "second version");

        assertThat(firstJudge.triesToSave(staleCopy, "stale version")).isEqualTo(CONFLICT);
        assertThat(firstJudge.draftNotes()).isEqualTo("second version");
    }

    @Test
    @DisplayName("submits the judge's draft for review, recording it in the case history as theirs")
    void submitsADraftForReview() {
        firstJudge.startsDraft("draft");
        Order draft = firstJudge.draft();

        var submission = firstJudge.submitsForReview(draft, "final");

        Order submitted = recorded(submission);
        assertThat(submitted.id()).isEqualTo(draft.id());
        assertThat(submitted.state()).isEqualTo(SUBMITTED_FOR_REVIEW);
        assertThat(notesIn(submitted)).isEqualTo("final");
        assertThat(firstJudge.hasADraft()).isFalse();
        assertThat(submission.audit().summary()).isEqualTo("Order submitted for review");
        assertThat(submission.audit().userId()).isEqualTo(firstJudge.uid());
    }

    @Test
    @DisplayName("does not let an order submitted for review be changed")
    void rejectsAChangeToASubmittedOrder() {
        firstJudge.startsDraft("draft");
        Order submitted = recorded(firstJudge.submitsForReview(firstJudge.draft(), "final"));

        assertThat(firstJudge.triesToSave(submitted, "changed after submission")).isEqualTo(CONFLICT);
    }

    @Test
    @DisplayName("lets a judge start a new draft once their order is submitted for review")
    void startsANewDraftAfterSubmission() {
        firstJudge.startsDraft("draft");
        firstJudge.submitsForReview(firstJudge.draft(), "final");

        firstJudge.startsDraft("next order");

        assertThat(firstJudge.draftNotes()).isEqualTo("next order");
    }

    @Test
    @DisplayName("refuses to change a draft without saying which draft")
    void rejectsAChangeWithoutADraftIdentifier() {
        assertThat(firstJudge.refusalOf(SAVE_DRAFT, newDraft("notes")))
            .contains("The order draft identifier is missing");
    }

    @Test
    @DisplayName("refuses a request from the frontend with no action or no order")
    void rejectsAMalformedRequest() {
        assertThat(firstJudge.refusalOf(null, null)).contains("The order action is missing");
        assertThat(firstJudge.refusalOf(SAVE_DRAFT, null)).contains("The order is missing");
    }

    private OrderingJudge judgeOn(long caseReference) {
        return OrderingJudge.onCase(events, "First", caseReference);
    }
}
