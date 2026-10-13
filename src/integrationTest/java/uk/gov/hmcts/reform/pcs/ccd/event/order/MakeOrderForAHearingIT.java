package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.HearingChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.CaseFacts;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.OrderEventIT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency.MONTHLY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.HearingRecords.CHANGED_BY_ANOTHER_JUDGE;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;

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
        PCSCase claim = cases.claim(ENGLAND);
        claim.setRentDetails(RentDetails.builder().currentRent(new BigDecimal("750.00")).frequency(MONTHLY).build());
        caseReference = cases.issue(claim);
        firstHearing = cases.hearing(caseReference, FIRST_HEARING, false);
        secondHearing = cases.hearing(caseReference, SECOND_HEARING, false);
        firstJudge = events.registerActor("First", "Judge", "caseworker-pcs");
        secondJudge = events.registerActor("Second", "Judge", "caseworker-pcs");
    }

    @Test
    @DisplayName("starts the first hearing's order from the claim's facts")
    void startsFromTheClaimsFacts() {
        Hearing hearing = forHearing(firstJudge, firstHearing).start().hearing();

        assertThat(hearing.id()).isEqualTo(firstHearing);
        assertThat(hearing.date()).isEqualTo(FIRST_HEARING);
        assertThat(hearing.caseFacts().currentRent()).isEqualByComparingTo("750.00");
        assertThat(hearing.caseFacts().rentFrequency()).isEqualTo("MONTHLY");
        assertThat(hearing.recorded()).as("the claim's facts were recorded by no judge").isNull();
        assertThat(hearing.version()).isNull();
        assertThat(hearing.note()).isNull();
        assertThat(hearing.earlierNotes()).isEmpty();
    }

    @Test
    @DisplayName("starts the next hearing from the facts and note the last hearing's judge recorded")
    void startsTheNextHearingFromTheLastOne() {
        submitForReview(firstJudge, firstHearing, null, facts("900.00"), "Turned up with rent book");

        Hearing next = forHearing(secondJudge, secondHearing).start().hearing();

        assertThat(next.caseFacts().currentRent()).isEqualByComparingTo("900.00");
        assertThat(next.caseFacts().arrearsToday()).isEqualByComparingTo("1200.00");
        assertThat(next.recorded().by()).isEqualTo("First Judge");
        assertThat(next.recorded().hearingDate()).isEqualTo(FIRST_HEARING);
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
    @DisplayName("refuses facts started from an older record than another judge has since made")
    void refusesAStaleRecord() {
        Hearing seenByBoth = forHearing(firstJudge, secondHearing).start().hearing();
        submitForReview(firstJudge, secondHearing, seenByBoth.version(), facts("900.00"), "first judge's note");

        var refused = forHearing(secondJudge, secondHearing).submitExpectingRejection(
            submission(SUBMIT_FOR_REVIEW, secondHearing, seenByBoth.version(), facts("1.00"), "overwritten"));

        assertThat(refused.errors()).containsExactly(CHANGED_BY_ANOTHER_JUDGE);
        Hearing recorded = forHearing(secondJudge, secondHearing).start().hearing();
        assertThat(recorded.caseFacts().currentRent()).isEqualByComparingTo("900.00");
        assertThat(recorded.note()).isEqualTo("first judge's note");
    }

    @Test
    @DisplayName("records the hearing's facts and note when the judge saves a draft, and keeps the draft with it")
    void recordsADraftsFactsForItsHearing() {
        forHearing(firstJudge, firstHearing)
            .submitExpectingSuccess(submission(SAVE_DRAFT, firstHearing, null, facts("900.00"), "draft note"));

        Hearing reopened = events.external(caseReference, MAKE_ORDER).as(firstJudge).start().hearing();
        assertThat(reopened.id()).isEqualTo(firstHearing);
        assertThat(reopened.caseFacts().currentRent()).isEqualByComparingTo("900.00");
        assertThat(reopened.note()).isEqualTo("draft note");
        assertThat(forHearing(firstJudge, secondHearing).start().hearing().id())
            .as("a link to another hearing opens the draft, for its own hearing").isEqualTo(firstHearing);
        assertThat(forHearing(firstJudge, firstHearing).submitExpectingRejection(
                change(SAVE_DRAFT, secondHearing)).errors())
            .containsExactly("The order is for a different hearing");
    }

    @Test
    @DisplayName("shows a judge reopening their draft the facts another judge has since recorded")
    void reopensADraftWithTheLatestFacts() {
        forHearing(firstJudge, secondHearing)
            .submitExpectingSuccess(submission(SAVE_DRAFT, secondHearing, null, facts("900.00"), null));
        Hearing seen = forHearing(secondJudge, secondHearing).start().hearing();
        submitForReview(secondJudge, secondHearing, seen.version(), facts("950.00"), null);

        Hearing reopened = events.external(caseReference, MAKE_ORDER).as(firstJudge).start().hearing();

        assertThat(reopened.caseFacts().currentRent()).isEqualByComparingTo("950.00");
        assertThat(reopened.recorded().by()).isEqualTo("Second Judge");
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

    private ExternalEvent<OrderStart, MakeOrderRequest> forHearing(Actor judge, int hearingId) {
        return events.external(caseReference, MAKE_ORDER).as(judge)
            .withClientContext(Map.of("hearingId", String.valueOf(hearingId)));
    }

    /** The judge starts an order for the hearing and sends it straight for review. */
    private void submitForReview(Actor judge, int hearingId, Long version, CaseFacts facts, String note) {
        forHearing(judge, hearingId).submitExpectingSuccess(
            submission(SUBMIT_FOR_REVIEW, hearingId, version, facts, note));
    }

    private static MakeOrderRequest submission(MakeOrderRequest.Action action, int hearingId, Long version,
                                               CaseFacts facts, String note) {
        return new MakeOrderRequest(action,
            new OrderChange(null, 0, OrderType.OUTRIGHT_POSSESSION, Map.of(), null), null,
            new HearingChange(hearingId, version, facts, note));
    }

    /** The judge's saved draft, changed as if it were for another hearing. */
    private MakeOrderRequest change(MakeOrderRequest.Action action, int hearingId) {
        OrderStart.Order draft = forHearing(firstJudge, firstHearing).start().order();
        return new MakeOrderRequest(action,
            new OrderChange(draft.id(), draft.version(), draft.orderType(), Map.of(), null), null,
            new HearingChange(hearingId, null, facts("900.00"), null));
    }

    private static CaseFacts facts(String currentRent) {
        return new CaseFacts(LocalDate.of(2020, 2, 3), "SECURE_TENANCY", null, new BigDecimal(currentRent),
            "MONTHLY", "Rent arrears", null, null, LocalDate.of(2026, 8, 1), new BigDecimal("100.00"),
            new BigDecimal("1200.00"));
    }
}
