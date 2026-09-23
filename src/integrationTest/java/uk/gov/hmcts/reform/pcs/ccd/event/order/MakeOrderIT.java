package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.RowChange;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.config.IssuedClaims;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.config.TestOrganisations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.START_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@PcsCcdEventTest
@DisplayName("Make an order")
class MakeOrderIT {

    private static final int BAD_REQUEST = 400;
    private static final int CONFLICT = 409;

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private TestOrganisations organisations;
    @Autowired
    private IssuedClaims claims;

    private Actor firstJudge;
    private Actor secondJudge;
    private Actor claimant;
    private long caseReference;

    @BeforeEach
    void setUp() {
        firstJudge = events.registerActor("First", "Judge", "caseworker-pcs");
        secondJudge = events.registerActor("Second", "Judge", "caseworker-pcs");
        claimant = events.registerActor("Claimant", "Solicitor", "caseworker-pcs", "caseworker-pcs-solicitor");
        organisations.register(claimant, "Possession Claims Solicitor Org");
        caseReference = claims.issue(claimant, ENGLAND);
    }

    @Test
    @DisplayName("gives the judge the facts and parties from the claim")
    void providesTheFactsAndPartiesFromTheClaim() {
        PCSCase claim = claims.baseClaim(ENGLAND);
        claim.getTenancyLicenceDetails().setTenancyLicenceDate(LocalDate.of(2020, 2, 3));
        claim.setNoticeServed(YesOrNo.YES);
        claim.setNoticeServedDetails(NoticeServedDetails.builder()
            .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
            .postedDate(LocalDate.of(2026, 8, 10))
            .build());
        claim.setRentDetails(RentDetails.builder()
            .currentRent(new BigDecimal("750.00"))
            .frequency(RentPaymentFrequency.MONTHLY)
            .build());
        claim.setRentArrears(RentArrearsSection.builder().total(new BigDecimal("1500.00")).build());
        claim.getSecureOrFlexiblePossessionGrounds().setSecureOrFlexibleDiscretionaryGrounds(
            Set.of(SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY));
        caseReference = claims.issue(claimant, claim);

        MakeOrderEnvelope opened = open(firstJudge);

        assertThat(opened.order().state()).isEqualTo(OrderState.DRAFT);
        assertThat(opened.order().version()).isZero();
        var context = opened.caseContext();
        assertThat(context.caseReference()).isEqualTo(caseReference);
        assertThat(context.claimants()).extracting(Party::name).containsExactly("Possession Claims Solicitor Org");
        assertThat(context.defendants()).extracting(Party::name).containsExactly("Jane Doe");
        var facts = context.caseFacts();
        assertThat(facts.tenancyStartDate()).isEqualTo(LocalDate.of(2020, 2, 3));
        assertThat(facts.tenancyType()).isEqualTo("SECURE_TENANCY");
        assertThat(facts.noticeDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(facts.currentRent()).isEqualByComparingTo("750.00");
        assertThat(facts.rentFrequency()).isEqualTo("MONTHLY");
        assertThat(facts.arrearsOnIssue()).isEqualByComparingTo("1500.00");
        assertThat(facts.groundsPleaded()).contains("Rent arrears");
    }

    @Test
    @DisplayName("uses the Welsh occupation contract when the claim has no tenancy")
    void usesTheWelshOccupationContract() {
        PCSCase claim = claims.baseClaim(WALES);
        claim.getOccupationLicenceDetailsWales().setLicenceStartDate(LocalDate.of(2024, 4, 5));
        caseReference = claims.issue(claimant, claim);

        var facts = open(firstJudge).caseContext().caseFacts();

        assertThat(facts.tenancyStartDate()).isEqualTo(LocalDate.of(2024, 4, 5));
        assertThat(facts.tenancyType()).isEqualTo("SECURE_CONTRACT");
        assertThat(facts.noticeDate()).isNull();
    }

    @Test
    @DisplayName("gives each judge their own draft on the same case")
    void eachJudgeKeepsTheirOwnDraft() {
        startDraft(firstJudge, "first judge's notes");
        startDraft(secondJudge, "second judge's notes");

        Order firstJudgesDraft = open(firstJudge).order();
        Order secondJudgesDraft = open(secondJudge).order();
        assertThat(notesIn(firstJudgesDraft)).isEqualTo("first judge's notes");
        assertThat(notesIn(secondJudgesDraft)).isEqualTo("second judge's notes");
        assertThat(firstJudgesDraft.id()).isNotEqualTo(secondJudgesDraft.id());
    }

    @Test
    @DisplayName("does not let a judge change another judge's draft")
    void rejectsAChangeToAnotherJudgesDraft() {
        startDraft(firstJudge, "first judge's notes");
        Order firstJudgesDraft = open(firstJudge).order();

        reply(secondJudge, SAVE_DRAFT, withNotes(firstJudgesDraft, "overwritten")).submitExpectingFailure(CONFLICT);

        assertThat(notesIn(open(firstJudge).order())).isEqualTo("first judge's notes");
    }

    @Test
    @DisplayName("does not let a judge start a second draft on the same case")
    void rejectsASecondDraftForTheSameJudge() {
        startDraft(firstJudge, "first draft");

        reply(firstJudge, START_DRAFT, newDraft("second draft")).submitExpectingFailure(CONFLICT);

        assertThat(notesIn(open(firstJudge).order())).isEqualTo("first draft");
    }

    @Test
    @DisplayName("saves a changed draft without submitting it")
    void savesADraft() {
        startDraft(firstJudge, "first version");
        Order draft = open(firstJudge).order();

        reply(firstJudge, SAVE_DRAFT, withNotes(draft, "second version")).submitExpectingSuccess();

        Order saved = open(firstJudge).order();
        assertThat(saved.id()).isEqualTo(draft.id());
        assertThat(saved.state()).isEqualTo(OrderState.DRAFT);
        assertThat(saved.version()).isGreaterThan(draft.version());
        assertThat(notesIn(saved)).isEqualTo("second version");
    }

    @Test
    @DisplayName("rejects a change made from an out-of-date copy of the draft")
    void rejectsAStaleChange() {
        startDraft(firstJudge, "first version");
        Order draft = open(firstJudge).order();
        reply(firstJudge, SAVE_DRAFT, withNotes(draft, "second version")).submitExpectingSuccess();

        reply(firstJudge, SAVE_DRAFT, withNotes(draft, "stale version")).submitExpectingFailure(CONFLICT);

        assertThat(notesIn(open(firstJudge).order())).isEqualTo("second version");
    }

    @Test
    @DisplayName("records the judge who submits an order for review in the order and its audit")
    void recordsTheJudgeWhoSubmitsForReview() {
        startDraft(firstJudge, "draft");
        Order draft = open(firstJudge).order();

        var submission = reply(firstJudge, SUBMIT_FOR_REVIEW, withNotes(draft, "final")).submitExpectingSuccess();

        assertThat(submission.changes("orders")).singleElement().satisfies(change -> {
            assertThat(change.operation()).isEqualTo(RowChange.Operation.UPDATE);
            assertThat(change.newValues().path("id").asText()).isEqualTo(draft.id().toString());
            assertThat(change.newValues().path("state").asText()).isEqualTo(OrderState.SUBMITTED_FOR_REVIEW.name());
            assertThat(change.newValues().path("idam_user_id").asText()).isEqualTo(firstJudge.uid());
            assertThat(change.newValues().path("draft_payload").path("notes").asText()).isEqualTo("final");
        });
    }

    @Test
    @DisplayName("does not let an order submitted for review be changed")
    void rejectsAChangeToASubmittedOrder() {
        startDraft(firstJudge, "draft");
        Order draft = open(firstJudge).order();
        var submitted = reply(firstJudge, SUBMIT_FOR_REVIEW, withNotes(draft, "final")).submitExpectingSuccess();
        long submittedVersion = submitted.changes("orders").getFirst().newValues().path("version").asLong();

        reply(firstJudge, SAVE_DRAFT, new Order(draft.id(), null, submittedVersion, notes("changed after submission")))
            .submitExpectingFailure(CONFLICT);
    }

    @Test
    @DisplayName("lets a judge start a new draft once their order is submitted for review")
    void startsANewDraftAfterSubmission() {
        startDraft(firstJudge, "draft");
        Order draft = open(firstJudge).order();
        reply(firstJudge, SUBMIT_FOR_REVIEW, withNotes(draft, "final")).submitExpectingSuccess();

        assertThat(open(firstJudge).order().id()).isNull();
        startDraft(firstJudge, "next order");

        assertThat(notesIn(open(firstJudge).order())).isEqualTo("next order");
    }

    @Test
    @DisplayName("rejects an order change that does not say which draft it is for")
    void rejectsAChangeWithoutADraftIdentifier() {
        var rejected = reply(firstJudge, SAVE_DRAFT, newDraft("notes")).submitExpectingFailure(BAD_REQUEST);

        assertThat(rejected.body()).contains("The order draft identifier is missing");
    }

    @Test
    @DisplayName("rejects an order submission with no action or no order")
    void rejectsAnIncompleteEnvelope() {
        var noAction = reply(firstJudge, null, null).submitExpectingFailure(BAD_REQUEST);
        var noOrder = reply(firstJudge, SAVE_DRAFT, null).submitExpectingFailure(BAD_REQUEST);

        assertThat(noAction.body()).contains("The order action is missing");
        assertThat(noOrder.body()).contains("The order is missing");
    }

    private MakeOrderEnvelope open(Actor judge) {
        return events.start(caseReference, MakeOrder.EVENT_ID).as(judge).startExpectingSuccess().payload();
    }

    /** Opens the event as the judge and replies with an order action, as the frontend does. */
    private CcdEventTestSupport<PCSCase, State>.EventSubmission reply(Actor judge, Action action, Order order) {
        return events.start(caseReference, MakeOrder.EVENT_ID).as(judge).startExpectingSuccess()
            .submittingPayload(new MakeOrderEnvelope(action, order, null));
    }

    private void startDraft(Actor judge, String notes) {
        reply(judge, START_DRAFT, newDraft(notes)).submitExpectingSuccess();
    }

    private static Order newDraft(String notes) {
        return new Order(null, null, 0, notes(notes));
    }

    private static Order withNotes(Order draft, String notes) {
        return new Order(draft.id(), draft.state(), draft.version(), notes(notes));
    }

    /** The frontend's form data is opaque to pcs-api; a single note stands in for it. */
    private static JsonNode notes(String notes) {
        return JsonNodeFactory.instance.objectNode().put("notes", notes);
    }

    private static String notesIn(Order order) {
        return order.draftPayload().path("notes").asText();
    }
}
