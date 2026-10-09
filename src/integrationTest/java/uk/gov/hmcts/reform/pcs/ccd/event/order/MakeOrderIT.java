package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.CaseContext;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.OrderEventIT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.FIRST_CLASS_POST;
import static uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod.PERSONALLY_HANDED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency.MONTHLY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.SECURE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.ADJOURN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.COUNTER_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.PENDING_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales.STANDARD_CONTRACT;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

/**
 * Judges make orders through the make order event as the frontend drives it: starting it sends them
 * their working order and the case, and they act on that order by submitting a request. The form is
 * opaque to pcs-api, so a single note stands in for it.
 */
@DisplayName("Make an order")
class MakeOrderIT extends OrderEventIT {

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;

    private long caseReference;
    private Actor firstJudge;
    private Actor secondJudge;
    private ExternalEvent<OrderStart, MakeOrderRequest> asFirstJudge;
    private ExternalEvent<OrderStart, MakeOrderRequest> asSecondJudge;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        firstJudge = events.registerActor("First", "Judge", "caseworker-pcs");
        asFirstJudge = events.external(caseReference, MAKE_ORDER).as(firstJudge);
        secondJudge = events.registerActor("Second", "Judge", "caseworker-pcs");
        asSecondJudge = events.external(caseReference, MAKE_ORDER).as(secondJudge);
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
        claim.getSecureOrFlexiblePossessionGrounds()
            .setSecureOrFlexibleDiscretionaryGrounds(Set.of(RENT_ARREARS_OR_BREACH_OF_TENANCY));

        var context = events.external(cases.issue(claim), MAKE_ORDER).as(firstJudge).start().caseContext();

        assertThat(context.propertyAddress().getPostCode()).isEqualTo(claim.getPropertyAddress().getPostCode());
        assertThat(context.claimants()).extracting(Party::name).containsExactly(IssuedCases.CLAIMANT);
        assertThat(context.defendants()).extracting(Party::name).containsExactly(IssuedCases.DEFENDANT);
        var facts = context.caseFacts();
        assertThat(facts.tenancyType()).isEqualTo("SECURE_TENANCY");
        assertThat(facts.tenancyStartDate()).isEqualTo(LocalDate.of(2020, 2, 3));
        assertThat(facts.noticeDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(facts.currentRent()).isEqualByComparingTo("750.00");
        assertThat(facts.rentFrequency()).isEqualTo("MONTHLY");
        assertThat(facts.groundsPleaded()).contains("Rent arrears");
    }

    @Test
    @DisplayName("says whether the case has an issued counterclaim or application")
    void saysWhetherTheCaseHasAnIssuedCounterclaimOrApplication() {
        assertThat(asFirstJudge.start().caseContext())
            .extracting(CaseContext::openCounterclaim, CaseContext::openApplication)
            .containsExactly(false, false);

        cases.update(caseReference, pcsCase -> {
            PartyEntity defendant = pcsCase.getParties().stream()
                .filter(party -> party.getOrgName() == null).findFirst().orElseThrow();
            pcsCase.addCounterClaim(CounterClaimEntity.builder().party(defendant).status(PENDING_REVIEW).build());
            pcsCase.addGenApp(GenAppEntity.builder().party(defendant).type(ADJOURN).state(GEN_APP_ISSUED)
                .applicationSubmittedDate(LocalDateTime.now()).build());
        });
        assertThat(asFirstJudge.start().caseContext())
            .extracting(CaseContext::openCounterclaim, CaseContext::openApplication)
            .containsExactly(false, true);

        cases.update(caseReference, pcsCase -> pcsCase.getCounterClaims().getFirst().setStatus(COUNTER_CLAIM_ISSUED));
        assertThat(asFirstJudge.start().caseContext().openCounterclaim()).isTrue();
    }

    @Test
    @DisplayName("names a defendant the claimant could not name as a person unknown")
    void namesAnUnnamedDefendantAsAPersonUnknown() {
        long caseReference = cases.issue(cases.claim(ENGLAND),
            PartyEntity.builder().nameKnown(VerticalYesNo.NO).build());

        var context = events.external(caseReference, MAKE_ORDER).as(firstJudge).start().caseContext();

        assertThat(context.defendants()).extracting(Party::name).containsExactly("Person unknown");
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

        var facts = events.external(cases.issue(claim), MAKE_ORDER).as(firstJudge).start().caseContext().caseFacts();

        assertThat(facts.noticeDate()).isEqualTo(LocalDate.of(2026, 8, 11));
    }

    @Test
    @DisplayName("shows a Welsh claim's occupation contract as the tenancy")
    void showsTheWelshOccupationContractAsTheTenancy() {
        PCSCase claim = cases.claim(WALES);
        claim.getOccupationLicenceDetailsWales().setOccupationLicenceTypeWales(STANDARD_CONTRACT);
        claim.getOccupationLicenceDetailsWales().setLicenceStartDate(LocalDate.of(2024, 4, 5));

        var facts = events.external(cases.issue(claim), MAKE_ORDER).as(firstJudge).start().caseContext().caseFacts();

        assertThat(facts.tenancyType()).isEqualTo("STANDARD_CONTRACT");
        assertThat(facts.tenancyStartDate()).isEqualTo(LocalDate.of(2024, 4, 5));
    }

    @Test
    @DisplayName("gives each judge their own draft on the same case")
    void eachJudgeKeepsTheirOwnDraft() {
        asFirstJudge.submitExpectingSuccess(startDraft("first judge's draft"));
        asSecondJudge.submitExpectingSuccess(startDraft("second judge's draft"));

        assertThat(workingDraft(asFirstJudge)).isEqualTo("first judge's draft");
        assertThat(workingDraft(asSecondJudge)).isEqualTo("second judge's draft");
    }

    @Test
    @DisplayName("does not let a judge change another judge's draft")
    void rejectsAChangeToAnotherJudgesDraft() {
        asFirstJudge.submitExpectingSuccess(startDraft("first judge's draft"));
        Order firstJudgesDraft = asFirstJudge.start().order();

        assertThat(asSecondJudge.submitExpectingRejection(change(SAVE_DRAFT, firstJudgesDraft, "overwritten")).errors())
            .containsExactly("The order draft does not exist for this case");
        assertThat(workingDraft(asFirstJudge)).isEqualTo("first judge's draft");
    }

    @Test
    @DisplayName("keeps the order type, form and document a judge saves as their working order")
    void savesADraft() {
        asFirstJudge.submitExpectingSuccess(startDraft("first version"));
        Order draft = asFirstJudge.start().order();
        var document = TextNode.valueOf("the order document");

        asFirstJudge.submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT, new OrderChange(draft.id(),
            draft.version(), OrderType.SUSPENDED_POSSESSION, Map.of("notes", "second version"), document), null));

        Order saved = asFirstJudge.start().order();
        assertThat(saved.orderType()).isEqualTo(OrderType.SUSPENDED_POSSESSION);
        assertThat(saved.formData()).containsEntry("notes", "second version");
        assertThat(saved.docweaveSnapshot()).isEqualTo(document);
        verifyNoInteractions(camundaService);
    }

    @Test
    @DisplayName("lists the judge's draft on the orders tab, and starts it again when it is chosen")
    void listsTheDraftOnTheOrdersTab() {
        asFirstJudge.submitExpectingSuccess(startDraft("first version"));
        Order draft = asFirstJudge.start().order();

        assertThat(events.view(caseReference, secondJudge).getOrdersMarkdown())
            .contains("Outright possession", "Draft");
        assertThat(asFirstJudge.withClientContext(Map.of("orderId", draft.id().toString())).start().order().formData())
            .containsEntry("notes", "first version");
    }

    @Test
    @DisplayName("does not let an out-of-date copy of a draft overwrite a newer save")
    void rejectsAStaleChange() {
        asFirstJudge.submitExpectingSuccess(startDraft("first version"));
        Order staleCopy = asFirstJudge.start().order();
        asFirstJudge.submitExpectingSuccess(change(SAVE_DRAFT, staleCopy, "second version"));

        assertThat(asFirstJudge.submitExpectingRejection(change(SAVE_DRAFT, staleCopy, "stale version")).errors())
            .containsExactly("The order has been updated by another user. Reload it and try again");
        assertThat(workingDraft(asFirstJudge)).isEqualTo("second version");
    }

    @Test
    @DisplayName("submits the judge's draft for review, recording it in the case history as theirs")
    void submitsADraftForReview() {
        asFirstJudge.submitExpectingSuccess(startDraft("draft"));
        Order draft = asFirstJudge.start().order();

        var submission = asFirstJudge.submitExpectingSuccess(change(SUBMIT_FOR_REVIEW, draft, "final"));

        Order submitted = submission.changed("orders", Order.class);
        assertThat(submitted.id()).isEqualTo(draft.id());
        assertThat(submitted.state()).isEqualTo(SUBMITTED_FOR_REVIEW);
        assertThat(asFirstJudge.start().order().id()).as("no working draft after submission").isNull();
        assertThat(submission.audit().summary()).isEqualTo("Order submitted for review");
        assertThat(submission.audit().userId()).isEqualTo(firstJudge.uid());
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.CONFIRM_ORDER_REVIEW),
            contains("has submitted an order for review"));
        // The submitted order is out of the judge's hands, and they can start their next one.
        assertThat(asFirstJudge.submitExpectingRejection(change(SAVE_DRAFT, submitted, "changed")).errors())
            .containsExactly("The order draft does not exist for this case");
        asFirstJudge.submitExpectingSuccess(startDraft("next order"));
        assertThat(workingDraft(asFirstJudge)).isEqualTo("next order");
    }

    /** The note on the judge's working draft. */
    private static Object workingDraft(ExternalEvent<OrderStart, MakeOrderRequest> judge) {
        return judge.start().order().formData().get("notes");
    }
}
