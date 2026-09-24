package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.ccd.sdk.testing.ExternalOutcome;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderStart.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.FIRST_CLASS_POST;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.PERSONALLY_HANDED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency.MONTHLY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.START_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales.STANDARD_CONTRACT;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

/**
 * Judges make orders through the make order event as the frontend drives it: starting it sends them
 * their working order and the case, and they act on that order by submitting a request. The
 * frontend's form data is opaque to pcs-api, so a single note stands in for it.
 */
@PcsCcdEventTest
@DisplayName("Make an order")
class MakeOrderIT {

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;

    private Actor firstJudge;
    private ExternalEvent<MakeOrderStart, MakeOrderRequest> asFirstJudge;
    private ExternalEvent<MakeOrderStart, MakeOrderRequest> asSecondJudge;

    @BeforeEach
    void setUp() {
        long caseReference = cases.issue();
        firstJudge = judge("First");
        asFirstJudge = makeOrder(caseReference).as(firstJudge);
        asSecondJudge = makeOrder(caseReference).as(judge("Second"));
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

        var context = startedByAJudge(cases.issue(claim)).caseContext();

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

        var facts = startedByAJudge(cases.issue(claim)).caseContext().caseFacts();

        assertThat(facts.noticeDate()).isEqualTo(LocalDate.of(2026, 8, 11));
    }

    @Test
    @DisplayName("shows a Welsh claim's occupation contract as the tenancy")
    void showsTheWelshOccupationContractAsTheTenancy() {
        PCSCase claim = cases.claim(WALES);
        claim.getOccupationLicenceDetailsWales().setOccupationLicenceTypeWales(STANDARD_CONTRACT);
        claim.getOccupationLicenceDetailsWales().setLicenceStartDate(LocalDate.of(2024, 4, 5));

        var facts = startedByAJudge(cases.issue(claim)).caseContext().caseFacts();

        assertThat(facts.tenancyType()).isEqualTo("STANDARD_CONTRACT");
        assertThat(facts.tenancyStartDate()).isEqualTo(LocalDate.of(2024, 4, 5));
    }

    @Test
    @DisplayName("gives each judge their own draft on the same case")
    void eachJudgeKeepsTheirOwnDraft() {
        asFirstJudge.submitExpectingSuccess(startDraft("first judge's notes"));
        asSecondJudge.submitExpectingSuccess(startDraft("second judge's notes"));

        assertThat(notesIn(asFirstJudge.start())).isEqualTo("first judge's notes");
        assertThat(notesIn(asSecondJudge.start())).isEqualTo("second judge's notes");
    }

    @Test
    @DisplayName("does not let a judge change another judge's draft")
    void rejectsAChangeToAnotherJudgesDraft() {
        asFirstJudge.submitExpectingSuccess(startDraft("first judge's notes"));
        Order firstJudgesDraft = asFirstJudge.start().order();

        assertThat(asSecondJudge.submitExpectingRejection(save(firstJudgesDraft, "overwritten")).errors())
            .containsExactly("The order draft does not exist for this case");
        assertThat(notesIn(asFirstJudge.start())).isEqualTo("first judge's notes");
    }

    @Test
    @DisplayName("does not let a judge start a second draft on the same case")
    void rejectsASecondDraftForTheSameJudge() {
        asFirstJudge.submitExpectingSuccess(startDraft("first draft"));

        assertThat(asFirstJudge.submitExpectingRejection(startDraft("second draft")).errors())
            .containsExactly("You already have an order draft for this case");
        assertThat(notesIn(asFirstJudge.start())).isEqualTo("first draft");
    }

    @Test
    @DisplayName("keeps a saved draft as the judge's working order")
    void savesADraft() {
        asFirstJudge.submitExpectingSuccess(startDraft("first version"));

        asFirstJudge.submitExpectingSuccess(save(asFirstJudge.start().order(), "second version"));

        assertThat(notesIn(asFirstJudge.start())).isEqualTo("second version");
    }

    @Test
    @DisplayName("does not let an out-of-date copy of a draft overwrite a newer save")
    void rejectsAStaleChange() {
        asFirstJudge.submitExpectingSuccess(startDraft("first version"));
        Order staleCopy = asFirstJudge.start().order();
        asFirstJudge.submitExpectingSuccess(save(staleCopy, "second version"));

        assertThat(asFirstJudge.submitExpectingRejection(save(staleCopy, "stale version")).errors())
            .containsExactly("The order draft has been updated by another user. Reload it and try again");
        assertThat(notesIn(asFirstJudge.start())).isEqualTo("second version");
    }

    @Test
    @DisplayName("submits the judge's draft for review, recording it in the case history as theirs")
    void submitsADraftForReview() {
        asFirstJudge.submitExpectingSuccess(startDraft("draft"));
        Order draft = asFirstJudge.start().order();

        var submission = asFirstJudge.submitExpectingSuccess(submitForReview(draft, "final"));

        Order submitted = recorded(submission);
        assertThat(submitted.id()).isEqualTo(draft.id());
        assertThat(submitted.state()).isEqualTo(SUBMITTED_FOR_REVIEW);
        assertThat(notesIn(submitted)).isEqualTo("final");
        assertThat(asFirstJudge.start().order().id()).as("no working draft after submission").isNull();
        assertThat(submission.audit().summary()).isEqualTo("Order submitted for review");
        assertThat(submission.audit().userId()).isEqualTo(firstJudge.uid());
    }

    @Test
    @DisplayName("does not let an order submitted for review be changed")
    void rejectsAChangeToASubmittedOrder() {
        asFirstJudge.submitExpectingSuccess(startDraft("draft"));
        Order submitted = recorded(
            asFirstJudge.submitExpectingSuccess(submitForReview(asFirstJudge.start().order(), "final")));

        assertThat(asFirstJudge.submitExpectingRejection(save(submitted, "changed after submission")).errors())
            .containsExactly("Only a draft order can be changed");
    }

    @Test
    @DisplayName("lets a judge start a new draft once their order is submitted for review")
    void startsANewDraftAfterSubmission() {
        asFirstJudge.submitExpectingSuccess(startDraft("draft"));
        asFirstJudge.submitExpectingSuccess(submitForReview(asFirstJudge.start().order(), "final"));

        asFirstJudge.submitExpectingSuccess(startDraft("next order"));

        assertThat(notesIn(asFirstJudge.start())).isEqualTo("next order");
    }

    @Test
    @DisplayName("refuses to change a draft without saying which draft")
    void rejectsAChangeWithoutADraftIdentifier() {
        var refused = asFirstJudge.submitExpectingRejection(
            new MakeOrderRequest(SAVE_DRAFT, new OrderChange(null, 0, notes("notes"))));

        assertThat(refused.errors()).containsExactly("The order draft identifier is missing");
    }

    @Test
    @DisplayName("refuses a request from the frontend with no action or no order")
    void rejectsAMalformedRequest() {
        assertThat(asFirstJudge.submitExpectingRejection(new MakeOrderRequest(null, null)).errors())
            .containsExactly("The order action is missing");
        assertThat(asFirstJudge.submitExpectingRejection(new MakeOrderRequest(SAVE_DRAFT, null)).errors())
            .containsExactly("The order is missing");
    }

    private Actor judge(String name) {
        return events.registerActor(name, "Judge", "caseworker-pcs");
    }

    private ExternalEvent<MakeOrderStart, MakeOrderRequest> makeOrder(long caseReference) {
        return events.external(caseReference, MakeOrder.MAKE_ORDER);
    }

    private MakeOrderStart startedByAJudge(long caseReference) {
        return makeOrder(caseReference).as(judge("First")).start();
    }

    private static MakeOrderRequest startDraft(String notes) {
        return new MakeOrderRequest(START_DRAFT, new OrderChange(null, 0, notes(notes)));
    }

    private static MakeOrderRequest save(Order order, String notes) {
        return change(SAVE_DRAFT, order, notes);
    }

    private static MakeOrderRequest submitForReview(Order order, String notes) {
        return change(SUBMIT_FOR_REVIEW, order, notes);
    }

    private static MakeOrderRequest change(Action action, Order order, String notes) {
        return new MakeOrderRequest(action, new OrderChange(order.id(), order.version(), notes(notes)));
    }

    private static JsonNode notes(String notes) {
        return JsonNodeFactory.instance.objectNode().put("notes", notes);
    }

    private static String notesIn(MakeOrderStart page) {
        return notesIn(page.order());
    }

    private static String notesIn(Order order) {
        return order.draftPayload().path("notes").asText();
    }

    /** The one order the submission changed, as the orders table recorded it. */
    private static Order recorded(ExternalOutcome submission) {
        var changes = submission.changes("orders");
        assertThat(changes).as("orders changed by the submission").hasSize(1);
        JsonNode row = changes.getFirst().newValues();
        return new Order(UUID.fromString(row.path("id").asText()), OrderState.valueOf(row.path("state").asText()),
            row.path("version").asLong(), row.path("draft_payload"));
    }
}
