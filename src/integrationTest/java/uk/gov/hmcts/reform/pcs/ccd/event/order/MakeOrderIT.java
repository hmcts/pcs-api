package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.config.TestOrganisations;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.testingsupport.service.BaseClaimPayloads;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.START_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action.SUBMIT_FOR_REVIEW;

@PcsCcdEventTest
@DisplayName("Make an order")
class MakeOrderIT {

    private static final String CREATE_CLAIM = "createPossessionClaim";
    private static final String RESUME_CLAIM = "resumePossessionClaim";
    // pcs-api answers an IllegalStateException with 409, which is what CCD would receive.
    private static final int CONFLICT = 409;

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private TestOrganisations organisations;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;

    private Actor firstJudge;
    private Actor secondJudge;
    private Actor claimant;
    private long caseReference;

    @BeforeEach
    void setUp() {
        firstJudge = events.registerActor("First", "Judge", "caseworker-pcs");
        secondJudge = events.registerActor("Second", "Judge", "caseworker-pcs");
        claimant = events.registerActor("Claimant", "Solicitor", "caseworker-pcs", "caseworker-pcs-solicitor");
        organisations.register(claimant, "Possession Claims Solicitor Org", "ORG-MAKE-ORDER");
        caseReference = issueClaim(baseClaim(LegislativeCountry.ENGLAND));
    }

    @Test
    @DisplayName("gives the judge the facts and parties from the claim")
    void providesTheFactsAndPartiesFromTheClaim() {
        PCSCase claim = baseClaim(LegislativeCountry.ENGLAND);
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
        caseReference = issueClaim(claim);

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
        PCSCase claim = baseClaim(LegislativeCountry.WALES);
        claim.getOccupationLicenceDetailsWales().setLicenceStartDate(LocalDate.of(2024, 4, 5));
        caseReference = issueClaim(claim);

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

        assertThat(notesIn(open(firstJudge))).isEqualTo("first judge's notes");
        assertThat(notesIn(open(secondJudge))).isEqualTo("second judge's notes");
        assertThat(draftOf(firstJudge).orElseThrow().getId()).isNotEqualTo(draftOf(secondJudge).orElseThrow().getId());
    }

    @Test
    @DisplayName("does not let a judge change another judge's draft")
    void rejectsAChangeToAnotherJudgesDraft() {
        startDraft(firstJudge, "first judge's notes");
        Order firstJudgesDraft = open(firstJudge).order();

        submit(secondJudge, SAVE_DRAFT, withNotes(firstJudgesDraft, "overwritten")).submitExpectingFailure(CONFLICT);

        assertThat(notesIn(open(firstJudge))).isEqualTo("first judge's notes");
    }

    @Test
    @DisplayName("does not let a judge start a second draft on the same case")
    void rejectsASecondDraftForTheSameJudge() {
        startDraft(firstJudge, "first draft");

        submit(firstJudge, START_DRAFT, newDraft("second draft")).submitExpectingFailure(CONFLICT);

        assertThat(notesIn(open(firstJudge))).isEqualTo("first draft");
    }

    @Test
    @DisplayName("saves a changed draft without submitting it")
    void savesADraft() {
        startDraft(firstJudge, "first version");
        Order draft = open(firstJudge).order();

        submit(firstJudge, SAVE_DRAFT, withNotes(draft, "second version")).submitExpectingSuccess();

        Order saved = open(firstJudge).order();
        assertThat(saved.id()).isEqualTo(draft.id());
        assertThat(saved.state()).isEqualTo(OrderState.DRAFT);
        assertThat(saved.version()).isGreaterThan(draft.version());
        assertThat(saved.draftPayload().path("notes").asText()).isEqualTo("second version");
    }

    @Test
    @DisplayName("rejects a change made from an out-of-date copy of the draft")
    void rejectsAStaleChange() {
        startDraft(firstJudge, "first version");
        Order draft = open(firstJudge).order();
        submit(firstJudge, SAVE_DRAFT, withNotes(draft, "second version")).submitExpectingSuccess();

        submit(firstJudge, SAVE_DRAFT, withNotes(draft, "stale version")).submitExpectingFailure(CONFLICT);

        assertThat(notesIn(open(firstJudge))).isEqualTo("second version");
    }

    @Test
    @DisplayName("records the judge who submits an order for review in the order and its audit")
    void recordsTheJudgeWhoSubmitsForReview() {
        startDraft(firstJudge, "draft");
        Order draft = open(firstJudge).order();

        var submission = submit(firstJudge, SUBMIT_FOR_REVIEW, withNotes(draft, "final")).submitExpectingSuccess();

        OrderEntity submitted = orderRepository.findById(draft.id()).orElseThrow();
        assertThat(submitted.getState()).isEqualTo(OrderState.SUBMITTED_FOR_REVIEW);
        assertThat(submitted.getIdamUserId()).isEqualTo(UUID.fromString(firstJudge.uid()));
        assertThat(submitted.getDraftPayload()).contains("final");
        assertThat(submission.changes("orders"))
            .filteredOn(change -> change.operation() == RowChange.Operation.UPDATE)
            .extracting(change -> change.newValues().path("idam_user_id").asText())
            .containsExactly(firstJudge.uid());
    }

    @Test
    @DisplayName("does not let an order submitted for review be changed")
    void rejectsAChangeToASubmittedOrder() {
        startDraft(firstJudge, "draft");
        Order draft = open(firstJudge).order();
        submit(firstJudge, SUBMIT_FOR_REVIEW, withNotes(draft, "final")).submitExpectingSuccess();
        long submittedVersion = orderRepository.findById(draft.id()).orElseThrow().getVersion();

        submit(firstJudge, SAVE_DRAFT, new Order(draft.id(), null, submittedVersion, notes("changed after submission")))
            .submitExpectingFailure(CONFLICT);
    }

    @Test
    @DisplayName("lets a judge start a new draft once their order is submitted for review")
    void startsANewDraftAfterSubmission() {
        startDraft(firstJudge, "draft");
        Order draft = open(firstJudge).order();
        submit(firstJudge, SUBMIT_FOR_REVIEW, withNotes(draft, "final")).submitExpectingSuccess();

        assertThat(open(firstJudge).order().id()).isNull();
        startDraft(firstJudge, "next order");

        assertThat(notesIn(open(firstJudge))).isEqualTo("next order");
    }

    @Test
    @DisplayName("rejects an order change that does not say which draft it is for")
    void rejectsAChangeWithoutADraftIdentifier() {
        assertThatThrownBy(() -> submit(firstJudge, SAVE_DRAFT, newDraft("notes")).submit())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order draft identifier is missing");
    }

    @Test
    @DisplayName("rejects an order submission with no action or no order")
    void rejectsAnIncompleteEnvelope() {
        assertThatThrownBy(() -> submitPayload(firstJudge, "{}").submit())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order action is missing");
        assertThatThrownBy(() -> submitPayload(firstJudge, "{\"action\":\"SAVE_DRAFT\"}").submit())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order is missing");
    }

    /** Creates a claim through pcs-api's own claim events, then issues it as the payment flow would. */
    private long issueClaim(PCSCase claim) {
        var create = events.create(CREATE_CLAIM, State.AWAITING_SUBMISSION_TO_HMCTS, PCSCase.builder()
                .propertyAddress(claim.getPropertyAddress())
                .legislativeCountry(claim.getLegislativeCountry())
                .build())
            .as(claimant);
        create.submitExpectingSuccess();
        events.event(create.reference(), RESUME_CLAIM, claim).as(claimant).submitExpectingSuccess();
        jdbc.update("update ccd.case_data set state = 'CASE_ISSUED' where reference = ?", create.reference());
        return create.reference();
    }

    /** The claim that pcs-api's testing support submits, at the property address it creates cases with. */
    private PCSCase baseClaim(LegislativeCountry country) {
        PCSCase claim = objectMapper.convertValue(BaseClaimPayloads.read(objectMapper, country), PCSCase.class);
        claim.setLegislativeCountry(country);
        claim.setPropertyAddress(BaseClaimPayloads.propertyAddress(country));
        return claim;
    }

    /** Opens the event as the judge and returns the envelope the start handler gives the frontend. */
    private MakeOrderEnvelope open(Actor judge) {
        PCSCase started = events.start(caseReference, MakeOrder.EVENT_ID).as(judge).startExpectingSuccess().caseData();
        return MakeOrderEnvelope.parse(objectMapper, started.getMakeOrderPayload());
    }

    private void startDraft(Actor judge, String notes) {
        submit(judge, START_DRAFT, newDraft(notes)).submitExpectingSuccess();
    }

    /** Opens the event as the judge and submits the order action, as the frontend does. */
    private CcdEventTestSupport<PCSCase, State>.CaseType.EventSubmission submit(Actor judge, Action action,
                                                                                   Order order) {
        return submitPayload(judge, new MakeOrderEnvelope(action, order, null).toJson(objectMapper));
    }

    private CcdEventTestSupport<PCSCase, State>.CaseType.EventSubmission submitPayload(Actor judge, String payload) {
        return events.start(caseReference, MakeOrder.EVENT_ID).as(judge).startExpectingSuccess()
            .edit(pcsCase -> pcsCase.setMakeOrderPayload(payload));
    }

    private Optional<OrderEntity> draftOf(Actor judge) {
        return orderRepository.findFirstByPcsCaseCaseReferenceAndIdamUserIdAndStateOrderByUpdatedAtDesc(
            caseReference, UUID.fromString(judge.uid()), OrderState.DRAFT);
    }

    private static Order newDraft(String notes) {
        return new Order(null, null, 0, notes(notes));
    }

    private static Order withNotes(Order draft, String notes) {
        return new Order(draft.id(), draft.state(), draft.version(), notes(notes));
    }

    private static JsonNode notes(String notes) {
        return new ObjectMapper().createObjectNode().put("notes", notes);
    }

    private static String notesIn(MakeOrderEnvelope opened) {
        return opened.order().draftPayload().path("notes").asText();
    }
}
