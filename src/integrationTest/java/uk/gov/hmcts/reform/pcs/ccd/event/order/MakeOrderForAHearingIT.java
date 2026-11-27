package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.HearingChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.CaseFacts;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.OrderEventIT;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.FIRST_CLASS_POST;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.PERSONALLY_HANDED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency.MONTHLY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.HearingRecords.CHANGED_BY_ANOTHER_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales.STANDARD_CONTRACT;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

/**
 * A judge making an order for a hearing records the case's facts and their note for that hearing, which
 * the judge at the next hearing starts from.
 */
@DisplayName("Make an order for a hearing")
class MakeOrderForAHearingIT extends OrderEventIT {

    private static final LocalDateTime FIRST_HEARING = LocalDateTime.of(2026, 9, 1, 10, 0);
    private static final LocalDateTime SECOND_HEARING = LocalDateTime.of(2026, 10, 13, 10, 0);

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;

    private long caseReference;
    private int firstHearing;
    private int secondHearing;
    private Actor firstJudge;
    private Actor secondJudge;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        firstHearing = cases.hearing(caseReference, FIRST_HEARING, false);
        secondHearing = cases.hearing(caseReference, SECOND_HEARING, false);
        firstJudge = events.registerActor("First", "Judge", "caseworker-pcs", "judiciary");
        secondJudge = events.registerActor("Second", "Judge", "caseworker-pcs", "judiciary");
    }

    @Test
    @DisplayName("starts the first hearing's order from the claim's facts")
    void startsFromTheClaimsFacts() {
        PCSCase claim = cases.claim(ENGLAND);
        claim.getTenancyLicenceDetails().setTypeOfTenancyLicence(SECURE_TENANCY);
        claim.getTenancyLicenceDetails().setTenancyLicenceDate(LocalDate.of(2020, 2, 3));
        claim.setNoticeServed(YesOrNo.YES);
        claim.setNoticeServedDetails(NoticeServedDetails.builder()
            .serviceMethod(FIRST_CLASS_POST)
            .postedDate(LocalDate.of(2026, 8, 10))
            .build());
        claim.setRentDetails(RentDetails.builder().currentRent(new BigDecimal("750.00")).frequency(MONTHLY).build());
        claim.getSecureOrFlexiblePossessionGrounds()
            .setSecureOrFlexibleDiscretionaryGrounds(Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY));

        Hearing hearing = firstHearingOf(claim);

        assertThat(hearing.date()).isEqualTo(FIRST_HEARING);
        assertThat(hearing.inWales()).isFalse();
        CaseFacts facts = hearing.caseFacts();
        assertThat(facts.tenancyType()).isEqualTo("SECURE_TENANCY");
        assertThat(facts.tenancyStartDate()).isEqualTo(LocalDate.of(2020, 2, 3));
        assertThat(facts.noticeDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(facts.currentRent()).isEqualByComparingTo("750.00");
        assertThat(facts.rentFrequency()).isEqualTo("MONTHLY");
        assertThat(facts.groundsPleaded()).contains("Rent arrears");
        assertThat(hearing.recorded()).as("the claim's facts were recorded by no judge").isNull();
        assertThat(hearing.version()).isNull();
        assertThat(hearing.note()).isNull();
        assertThat(hearing.earlierNotes()).isEmpty();
    }

    @Test
    @DisplayName("takes the day a notice handed over in person was served from the claim")
    void takesTheDayANoticeWasHandedOver() {
        PCSCase claim = cases.claim(ENGLAND);
        claim.setNoticeServed(YesOrNo.YES);
        claim.setNoticeServedDetails(NoticeServedDetails.builder()
            .serviceMethod(PERSONALLY_HANDED)
            .handedOverDateTime(LocalDateTime.of(2026, 8, 11, 16, 30))
            .build());

        assertThat(firstHearingOf(claim).caseFacts().noticeDate()).isEqualTo(LocalDate.of(2026, 8, 11));
    }

    @Test
    @DisplayName("takes a Welsh claim's occupation contract as the tenancy")
    void takesTheWelshOccupationContractAsTheTenancy() {
        PCSCase claim = cases.claim(WALES);
        claim.getOccupationLicenceDetailsWales().setOccupationLicenceTypeWales(STANDARD_CONTRACT);
        claim.getOccupationLicenceDetailsWales().setLicenceStartDate(LocalDate.of(2024, 4, 5));

        Hearing hearing = firstHearingOf(claim);

        assertThat(hearing.inWales()).isTrue();
        CaseFacts facts = hearing.caseFacts();
        assertThat(facts.tenancyType()).isEqualTo("STANDARD_CONTRACT");
        assertThat(facts.tenancyStartDate()).isEqualTo(LocalDate.of(2024, 4, 5));
    }

    @Test
    @DisplayName("starts the next hearing from the facts and note the last hearing's judge recorded")
    void startsTheNextHearingFromTheLastOne() {
        final Instant before = Instant.now();
        submitForReview(firstJudge, firstHearing, null, facts("900.00"), "Turned up with rent book");

        Hearing next = forHearing(secondJudge, secondHearing).start().hearing();

        assertThat(next.caseFacts().currentRent()).isEqualByComparingTo("900.00");
        assertThat(next.caseFacts().arrearsToday()).isEqualByComparingTo("1200.00");
        assertThat(next.recorded().by()).isEqualTo("First Judge");
        assertThat(next.recorded().on()).isBetween(before, Instant.now());
        assertThat(next.version()).as("nothing is recorded for this hearing yet").isNull();
        assertThat(next.note()).as("each hearing's note starts blank").isNull();
        assertThat(next.earlierNotes()).singleElement().satisfies(note -> {
            assertThat(note.by()).isEqualTo("First Judge");
            assertThat(note.hearingDate()).isEqualTo(FIRST_HEARING);
            assertThat(note.note()).isEqualTo("Turned up with rent book");
        });
    }

    @Test
    @DisplayName("keeps each hearing's facts as its judge recorded them")
    void keepsEachHearingsFacts() {
        submitForReview(firstJudge, firstHearing, null, facts("900.00"), "first hearing's note");
        submitForReview(secondJudge, secondHearing, null, facts("950.00"), "second hearing's note");

        Hearing first = forHearing(firstJudge, firstHearing).start().hearing();

        assertThat(first.caseFacts().currentRent()).isEqualByComparingTo("900.00");
        assertThat(first.recorded().by()).isEqualTo("First Judge");
        assertThat(first.note()).isEqualTo("first hearing's note");
        assertThat(first.earlierNotes()).as("a later hearing's note is not an earlier one").isEmpty();
        assertThat(forHearing(secondJudge, secondHearing).start().hearing().caseFacts().currentRent())
            .isEqualByComparingTo("950.00");
    }

    @Test
    @DisplayName("refuses changed facts started from an older record than another judge has since made")
    void refusesAStaleRecord() {
        Hearing seenByBoth = forHearing(firstJudge, secondHearing).start().hearing();
        submitForReview(firstJudge, secondHearing, seenByBoth.version(), facts("900.00"), "first judge's note");

        var refused = forHearing(secondJudge, secondHearing).submitExpectingRejection(
            submission(SUBMIT_FOR_REVIEW, secondHearing, seenByBoth.version(), facts("1.00"), "overwritten"));

        assertThat(refused.errors()).containsExactly(CHANGED_BY_ANOTHER_JUDGE);
        // A judge who left the facts and note alone saves their order regardless.
        submitForReview(secondJudge, secondHearing, seenByBoth.version(), null, null);
        Hearing recorded = forHearing(secondJudge, secondHearing).start().hearing();
        assertThat(recorded.caseFacts().currentRent()).isEqualByComparingTo("900.00");
        assertThat(recorded.note()).isEqualTo("first judge's note");
        assertThat(recorded.recorded().by()).isEqualTo("First Judge");
    }

    @Test
    @DisplayName("records the hearing's facts and note when a judge saves a draft, which reopens with the latest")
    void recordsADraftsFactsForItsHearing() {
        forHearing(firstJudge, firstHearing)
            .submitExpectingSuccess(submission(SAVE_DRAFT, firstHearing, null, facts("900.00"), "draft note"));

        Hearing reopened = forHearing(firstJudge, firstHearing).start().hearing();
        assertThat(reopened.caseFacts().currentRent()).isEqualByComparingTo("900.00");
        assertThat(reopened.note()).isEqualTo("draft note");

        // Another judge records newer facts for the hearing; the draft reopens with them.
        submitForReview(secondJudge, firstHearing, reopened.version(), facts("950.00"), "draft note");
        Hearing latest = forHearing(firstJudge, firstHearing).start().hearing();
        assertThat(latest.caseFacts().currentRent()).isEqualByComparingTo("950.00");
        assertThat(latest.recorded().by()).isEqualTo("Second Judge");
    }

    @Test
    @DisplayName("keeps a judge's one draft on the case wherever it is made, whichever link opens it")
    void keepsOneDraftWhereverItIsMade() {
        forHearing(firstJudge, firstHearing).submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, OrderType.ADJOURNMENT, Map.of("notes", "at the first"), WORDING), null,
            new HearingChange(firstHearing, null, facts("900.00"), null)));
        OrderStart saved = forHearing(firstJudge, firstHearing).start();
        assertThat(saved.order().state()).isEqualTo(DRAFT);

        // A link to another hearing, or to none, opens the draft where it is made: a link's hearing is a new order's.
        OrderStart fromAnotherHearing = forHearing(firstJudge, secondHearing).start();
        OrderStart fromTheCase = events.external(caseReference, MAKE_ORDER).as(firstJudge).start();
        assertThat(List.of(fromAnotherHearing, fromTheCase)).allSatisfy(reopened -> {
            assertThat(reopened.order().id()).isEqualTo(saved.order().id());
            assertThat(reopened.order().formData()).containsEntry("notes", "at the first");
            assertThat(reopened.order().docweaveSnapshot()).isEqualTo(WORDING);
            assertThat(reopened.hearing().id()).isEqualTo(firstHearing);
            assertThat(reopened.hearing().caseFacts().currentRent()).isEqualByComparingTo("900.00");
        });
        assertThat(fromTheCase.caseContext().hearings()).extracting(OrderStart.HearingOption::id)
            .as("every hearing on the case, earliest first, for the judge to choose from")
            .containsExactly(firstHearing, secondHearing);

        // A page opened before the draft was saved starts no second one, at another hearing, at none, or sent.
        assertThat(forHearing(firstJudge, secondHearing).submitExpectingRejection(
            submission(SAVE_DRAFT, secondHearing, null, null, null)).errors())
            .containsExactly(MakeOrder.ALREADY_DRAFTING);
        assertThat(events.external(caseReference, MAKE_ORDER).as(firstJudge)
            .submitExpectingRejection(startDraft("on the papers")).errors())
            .containsExactly(MakeOrder.ALREADY_DRAFTING);
        assertThat(forHearing(firstJudge, secondHearing).submitExpectingRejection(
            submission(SUBMIT_FOR_REVIEW, secondHearing, null, null, null)).errors())
            .containsExactly(MakeOrder.ALREADY_DRAFTING);
        assertThat(orders(firstJudge)).containsExactly(tuple(saved.order().id(), "DRAFT", firstHearing));
        Order untouched = events.external(caseReference, MAKE_ORDER).as(firstJudge).start().order();
        assertThat(untouched.version()).isEqualTo(saved.order().version());
        assertThat(untouched.formData()).containsEntry("notes", "at the first");

        // Another judge's draft on the case is their own, at the same hearing or any.
        forHearing(secondJudge, firstHearing)
            .submitExpectingSuccess(submission(SAVE_DRAFT, firstHearing, null, null, null));
        assertThat(forHearing(secondJudge, firstHearing).start().order().id())
            .isNotNull().isNotEqualTo(saved.order().id());
        assertThat(orders(firstJudge)).hasSize(1);
    }

    @Test
    @DisplayName("has the database refuse a judge a second draft on a case, at another hearing or none")
    void theDatabaseRefusesASecondDraft() {
        forHearing(firstJudge, firstHearing)
            .submitExpectingSuccess(submission(SAVE_DRAFT, firstHearing, null, null, null));

        for (Integer hearing : Arrays.asList(firstHearing, secondHearing, null)) {
            assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO orders (id, case_id, author_idam_user_id, "
                + "state, hearing_id) SELECT gen_random_uuid(), case_id, author_idam_user_id, 'DRAFT', ? FROM orders "
                + "WHERE case_id = (SELECT id FROM pcs_case WHERE case_reference = ?)", hearing, caseReference))
                .hasMessageContaining("orders_one_draft_per_author_idx");
        }
    }

    @Test
    @DisplayName("moves the judge's draft to the hearing they choose, or to none, and back, keeping its wording and "
        + "leaving one draft, whatever link opened it")
    void movesADraftToTheChosenHearing() {
        forHearing(firstJudge, firstHearing).submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, OrderType.ADJOURNMENT, Map.of("notes", "kept"), WORDING), null,
            new HearingChange(firstHearing, null, null, null)));

        // The judge opened the first hearing's draft, which came with the second hearing to choose, then chose it.
        OrderStart chosen = forHearing(firstJudge, firstHearing).start();
        assertThat(chosen.hearing().id()).isEqualTo(firstHearing);
        assertThat(chosen.otherHearings()).singleElement().satisfies(other -> {
            assertThat(other.id()).isEqualTo(secondHearing);
            assertThat(other.caseFacts()).as("the facts the judge would start from there").isNotNull();
        });
        forHearing(firstJudge, firstHearing).submitExpectingSuccess(new MakeOrderRequest(
            SAVE_DRAFT, new OrderChange(chosen.order().id(), chosen.order().version(), OrderType.ADJOURNMENT,
                chosen.order().formData(), WORDING), null, new HearingChange(secondHearing,
                chosen.otherHearings().getFirst().version(), facts("925.00"), "at the second")));

        // The link to the first hearing, which opened it, opens it still, where it is now made.
        OrderStart moved = forHearing(firstJudge, firstHearing).start();
        assertThat(moved.order().id()).isEqualTo(chosen.order().id());
        assertThat(moved.order().formData()).containsEntry("notes", "kept");
        assertThat(moved.order().docweaveSnapshot()).isEqualTo(WORDING);
        assertThat(moved.hearing().id()).isEqualTo(secondHearing);
        assertThat(moved.hearing().caseFacts().currentRent()).isEqualByComparingTo("925.00");
        assertThat(moved.hearing().note()).isEqualTo("at the second");
        assertThat(moved.otherHearings()).extracting(Hearing::id).containsExactly(firstHearing);

        // Then without a hearing.
        forHearing(firstJudge, secondHearing).submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(moved.order().id(), moved.order().version(), OrderType.ADJOURNMENT,
                moved.order().formData(), WORDING), null));
        OrderStart papers = forHearing(firstJudge, secondHearing).start();
        assertThat(papers.order().id()).isEqualTo(chosen.order().id());
        assertThat(papers.hearing()).isNull();
        assertThat(papers.otherHearings()).extracting(Hearing::id).containsExactly(firstHearing, secondHearing);

        // And back to the first hearing.
        events.external(caseReference, MAKE_ORDER).as(firstJudge).submitExpectingSuccess(new MakeOrderRequest(
            SAVE_DRAFT, new OrderChange(papers.order().id(), papers.order().version(), OrderType.ADJOURNMENT,
                papers.order().formData(), WORDING), null, new HearingChange(firstHearing, null, null, null)));
        assertThat(orders(firstJudge)).containsExactly(tuple(chosen.order().id(), "DRAFT", firstHearing));
        assertThat(events.external(caseReference, MAKE_ORDER).as(firstJudge).start().order().formData())
            .containsEntry("notes", "kept");
        // The second hearing keeps the facts and note recorded at it.
        assertThat(forHearing(secondJudge, secondHearing).start().hearing().note()).isEqualTo("at the second");
    }

    @Test
    @DisplayName("keeps a saved draft usable after its hearing is cancelled, and lets the judge move it to boxwork")
    void keepsADraftUsableAfterItsHearingIsCancelled() {
        forHearing(firstJudge, firstHearing).submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, OrderType.ADJOURNMENT, Map.of("notes", "kept"), WORDING), null,
            new HearingChange(firstHearing, null, null, null)));
        jdbcTemplate.update("UPDATE hearing SET cancelled = true WHERE id = ?", firstHearing);

        var atCancelledHearing = events.external(caseReference, MAKE_ORDER).as(firstJudge);
        OrderStart reopened = atCancelledHearing.start();
        assertThat(reopened.hearing().id()).isEqualTo(firstHearing);
        assertThat(reopened.otherHearings()).extracting(Hearing::id).containsExactly(secondHearing);
        assertThat(reopened.order().formData()).containsEntry("notes", "kept");
        atCancelledHearing.submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(reopened.order().id(), reopened.order().version(), OrderType.ADJOURNMENT,
                reopened.order().formData(), WORDING), null, new HearingChange(firstHearing, null, null, null)));

        OrderStart saved = atCancelledHearing.start();
        atCancelledHearing.submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW,
            new OrderChange(saved.order().id(), saved.order().version(), OrderType.ADJOURNMENT,
                saved.order().formData(), WORDING), null));
        assertThat(orders(firstJudge))
            .containsExactly(tuple(reopened.order().id(), "SUBMITTED_FOR_REVIEW", null));
    }

    @Test
    @DisplayName("only lets the judge choose one of the case's own hearings that has not been cancelled")
    void onlyChoosesTheCasesOwnHearings() {
        events.external(caseReference, MAKE_ORDER).as(firstJudge).submitExpectingSuccess(startDraft("on the papers"));
        OrderStart.Order onPapers = events.external(caseReference, MAKE_ORDER).as(firstJudge).start().order();

        cases.hearing(caseReference, SECOND_HEARING.plusDays(7), true);
        assertThat(events.external(caseReference, MAKE_ORDER).as(firstJudge).start().otherHearings())
            .extracting(Hearing::id).as("the case's own hearings that have not been cancelled, earliest first")
            .containsExactly(firstHearing, secondHearing);
        int otherCasesHearing = cases.hearing(cases.issue(), FIRST_HEARING, false);
        assertThat(events.external(caseReference, MAKE_ORDER).as(firstJudge).submitExpectingRejection(
                new MakeOrderRequest(SAVE_DRAFT, new OrderChange(onPapers.id(), onPapers.version(),
                    onPapers.orderType(), Map.of(), WORDING), null,
                    new HearingChange(otherCasesHearing, null, null, null))).errors())
            .containsExactly("The hearing is not one of this case's hearings");
    }

    @Test
    @DisplayName("shows court staff reviewing the order which hearing it was made for, without the facts and note the "
        + "judge records for the hearing")
    void showsCourtStaffTheOrdersHearing() {
        UUID orderId = forHearing(firstJudge, firstHearing)
            .submitExpectingSuccess(submission(SUBMIT_FOR_REVIEW, firstHearing, null, facts("900.00"), "a note"))
            .changed("orders", OrderRow.class).id();

        Hearing reviewed = events.external(caseReference, CONFIRM_ORDER_REVIEW)
            .as(events.registerActor("A", "Caseworker", "caseworker-pcs"))
            .withClientContext(Map.of("orderId", orderId.toString())).start().hearing();

        assertThat(reviewed.id()).isEqualTo(firstHearing);
        assertThat(reviewed.date()).isEqualTo(FIRST_HEARING);
        assertThat(reviewed.caseFacts()).isNull();
        assertThat(reviewed.note()).isNull();
        assertThat(reviewed.earlierNotes()).isEmpty();
    }

    @Test
    @DisplayName("removes the hearing's note when the judge sends the order again with it cleared")
    void removesAClearedNote() {
        submitForReview(firstJudge, firstHearing, null, facts("900.00"), "first thoughts");
        Hearing recorded = forHearing(firstJudge, firstHearing).start().hearing();

        submitForReview(firstJudge, firstHearing, recorded.version(), facts("900.00"), "  ");

        assertThat(forHearing(firstJudge, firstHearing).start().hearing().note()).isNull();
        assertThat(forHearing(secondJudge, secondHearing).start().hearing().earlierNotes()).isEmpty();
    }

    @Test
    @DisplayName("only makes orders for the case's own hearings that have not been cancelled")
    void refusesAnotherCasesOrACancelledHearing() {
        int otherCasesHearing = cases.hearing(cases.issue(), FIRST_HEARING, false);
        int cancelledHearing = cases.hearing(caseReference, SECOND_HEARING.plusDays(7), true);

        assertThat(forHearing(firstJudge, otherCasesHearing).startExpectingRejection())
            .containsExactly("The hearing is not one of this case's hearings");
        assertThat(forHearing(firstJudge, cancelledHearing).startExpectingRejection())
            .containsExactly("The hearing is not one of this case's hearings");
    }

    /** The claim issued with one hearing listed, as the judge starts an order for that hearing. */
    private Hearing firstHearingOf(PCSCase claim) {
        long reference = cases.issue(claim);
        int hearingId = cases.hearing(reference, FIRST_HEARING, false);
        return events.external(reference, MAKE_ORDER).as(firstJudge)
            .withClientContext(Map.of("hearingId", String.valueOf(hearingId))).start().hearing();
    }

    private ExternalEvent<OrderStart, MakeOrderRequest> forHearing(Actor judge, int hearingId) {
        return events.external(caseReference, MAKE_ORDER).as(judge)
            .withClientContext(Map.of("hearingId", String.valueOf(hearingId)));
    }

    /** The judge's orders on the case: each one's id, state and hearing. */
    private List<Tuple> orders(Actor judge) {
        return jdbcTemplate.query("SELECT o.id, o.state, o.hearing_id FROM orders o JOIN pcs_case pc "
            + "ON pc.id = o.case_id WHERE pc.case_reference = ? AND o.author_idam_user_id = ?",
            (row, index) -> tuple(row.getObject("id", UUID.class), row.getString("state"),
                row.getObject("hearing_id", Integer.class)), caseReference, judge.uid());
    }

    /** The judge starts an order for the hearing and sends it straight for review. */
    private void submitForReview(Actor judge, int hearingId, Long version, CaseFacts facts, String note) {
        forHearing(judge, hearingId).submitExpectingSuccess(
            submission(SUBMIT_FOR_REVIEW, hearingId, version, facts, note));
    }

    private static MakeOrderRequest submission(MakeOrderRequest.Action action, int hearingId, Long version,
                                               CaseFacts facts, String note) {
        return new MakeOrderRequest(action,
            new OrderChange(null, 0, OrderType.OUTRIGHT_POSSESSION, Map.of(), WORDING), null,
            new HearingChange(hearingId, version, facts, note));
    }

    private static CaseFacts facts(String currentRent) {
        return new CaseFacts(LocalDate.of(2020, 2, 3), "SECURE_TENANCY", null, new BigDecimal(currentRent),
            "MONTHLY", "Rent arrears", null, null, LocalDate.of(2026, 8, 1), new BigDecimal("100.00"),
            new BigDecimal("1200.00"));
    }
}
