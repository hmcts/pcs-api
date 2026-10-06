package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.ExternalEvent;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskCompletionService;
import uk.gov.hmcts.reform.pcs.camunda.TaskSubject;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.IssuedOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Issue;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Application;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.Order;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.ADJOURN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.ConfirmOrderReviewRequest.Action.ISSUE;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SAVE_DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal.COUNTY_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.ConfirmOrderReview.CONFIRM_ORDER_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * Court staff refer a general application to a judge, who decides it by making an order on it: the
 * order starts from the application, and issuing it records what it decided.
 */
@PcsCcdEventTest
@DisplayName("Decide an application")
class DecideApplicationIT extends AbstractPostgresContainerIT {

    private static final String APPLICATION_DECISION = "APPLICATION_DECISION";
    private static final String WORDING =
        "<p>IT IS ORDERED THAT:</p><ol><li><p>The hearing is adjourned.</p></li></ol>";
    private static final String WHAT_ORDER_WANTED = "1. The hearing on 14 October is adjourned to the first open date.";

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private CamundaService camundaService;
    @MockitoBean
    private TaskCompletionService taskCompletionService;
    /** The case's view asks CCD for the user's case roles to filter its applications, which the test CCD refuses. */
    @MockitoBean
    private UserRoleService userRoleService;

    private long caseReference;
    private UUID genAppId;
    private Actor judge;
    private Actor caseworker;
    private ExternalEvent<OrderStart, MakeOrderRequest> asJudge;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        cases.update(caseReference, pcsCase -> {
            PartyEntity defendant = pcsCase.getParties().stream()
                .filter(party -> party.getOrgName() == null).findFirst().orElseThrow();
            pcsCase.addGenApp(GenAppEntity.builder().party(defendant).type(ADJOURN).state(GEN_APP_ISSUED)
                .within14Days(VerticalYesNo.YES).otherPartiesAgreed(VerticalYesNo.NO)
                .withoutNotice(VerticalYesNo.NO).whatOrderWanted(WHAT_ORDER_WANTED)
                .applicationSubmittedDate(LocalDateTime.of(2026, 10, 1, 9, 30)).build());
        });
        genAppId = jdbcTemplate.queryForObject("""
            SELECT g.id FROM general_application g JOIN pcs_case c ON c.id = g.case_id
            WHERE c.case_reference = ?""", UUID.class, caseReference);
        when(userRoleService.getCurrentUserCaseRoles(anyLong())).thenReturn(new UserRoles(null, List.of()));
        when(userRoleService.getCurrentUserOrganisationalRoles())
            .thenReturn(new UserRoles(null, List.of("caseworker-pcs")));
        judge = events.registerActor("A", "Judge", "caseworker-pcs");
        caseworker = events.registerActor("A", "Caseworker", "caseworker-pcs");
        asJudge = events.external(caseReference, MAKE_ORDER).as(judge)
            .withClientContext(Map.of("genAppId", genAppId.toString()));
    }

    @Test
    @DisplayName("refers an application to a judge, giving the judges a task to decide it")
    void refersTheApplicationToAJudge() {
        var started = events.start(caseReference, "referGenAppToJudge").as(caseworker).startExpectingSuccess();
        assertThat(started.caseData().getReferGenAppList().getListItems())
            .extracting(DynamicListElement::getLabel)
            .containsExactly("GA1: Adjourn the hearing, by " + IssuedCases.DEFENDANT);

        started.edit(pcsCase -> {
            pcsCase.getReferGenAppList().setValue(pcsCase.getReferGenAppList().getListItems().getFirst());
            pcsCase.setReferGenAppNote("The hearing is next week");
        }).as(caseworker).submitExpectingSuccess();

        assertThat(jdbcTemplate.queryForMap(
            "SELECT referral_note, referred_on IS NOT NULL AS referred FROM general_application WHERE id = ?",
            genAppId)).containsEntry("referral_note", "The hearing is next week").containsEntry("referred", true);
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.DECIDE_GEN_APP),
            contains("ext:makeOrder?expected_sub=${[EXPECTED_SUB]}&amp;genAppId=" + genAppId),
            eq(TaskSubject.genApp(genAppId)), isNull());
    }

    @Test
    @DisplayName("shows the judge the application they are deciding and what the applicant asked for")
    void showsTheJudgeTheApplication() {
        OrderStart start = asJudge.start();

        assertThat(start.order().genAppId()).isEqualTo(genAppId);
        Application application = start.caseContext().application();
        assertThat(application.reference()).isEqualTo("GA1");
        assertThat(application.type()).isEqualTo("Adjourn the hearing");
        assertThat(application.applicant()).startsWith(IssuedCases.DEFENDANT);
        assertThat(application.within14Days()).isEqualTo("YES");
        assertThat(application.whatOrderWanted()).isEqualTo(WHAT_ORDER_WANTED);
    }

    @Test
    @DisplayName("keeps the judge's order on the application apart from their draft for the case")
    void keepsTheApplicationDraftApartFromTheCaseDraft() {
        events.external(caseReference, MAKE_ORDER).as(judge).submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, "OUTRIGHT_POSSESSION", Map.of("notes", "case draft"), null, null)));
        asJudge.submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, APPLICATION_DECISION, Map.of("application-decision", "grant"), null, genAppId)));

        assertThat(asJudge.start().order().formData()).containsEntry("application-decision", "grant");
        assertThat(events.external(caseReference, MAKE_ORDER).as(judge).start().order().formData())
            .containsEntry("notes", "case draft");
    }

    @Test
    @DisplayName("closes the judge's task when they send their order on the application for review")
    void closesTheTaskOnSubmission() {
        Order submitted = judgeSubmits(Map.of("application-decision", "grant"));

        assertThat(submitted.genAppId()).isEqualTo(genAppId);
        verify(camundaService).cancelTask(eq(caseReference), eq(TaskType.DECIDE_GEN_APP),
            eq(TaskSubject.genApp(genAppId)), any());
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.CONFIRM_ORDER_REVIEW),
            contains("has submitted an order for review"), eq(submitted.id()));
    }

    @Test
    @DisplayName("records the decision when court staff issue the order, closing a granted application")
    void recordsTheDecisionOnIssue() {
        Order submitted = judgeSubmits(Map.of("application-decision", "grant"));
        var asCaseworker = events.external(caseReference, CONFIRM_ORDER_REVIEW).as(caseworker)
            .withClientContext(Map.of("orderId", submitted.id().toString()));
        assertThat(asCaseworker.start().caseContext().application().reference()).isEqualTo("GA1");

        asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, submitted.id(), submitted.version(),
            null, new Issue(new IssuedOrder(APPLICATION_DECISION, submitted.formData(), null, WORDING), List.of(),
                true, true, true, List.of(), COUNTY_COURT)));

        assertThat(jdbcTemplate.queryForMap(
            "SELECT outcome, decided_by_order_id IS NOT NULL AS decided FROM general_application WHERE id = ?",
            genAppId)).containsEntry("outcome", "GRANTED").containsEntry("decided", true);
        assertThat(events.external(caseReference, MAKE_ORDER).as(judge).start().caseContext().openApplication())
            .isFalse();
        assertThat(asJudge.startExpectingRejection())
            .containsExactly("The application has already been decided");
    }

    @Test
    @DisplayName("leaves an application listed for a hearing open")
    void leavesAListedApplicationOpen() {
        Order submitted = judgeSubmits(Map.of("application-decision", "list"));
        var asCaseworker = events.external(caseReference, CONFIRM_ORDER_REVIEW).as(caseworker)
            .withClientContext(Map.of("orderId", submitted.id().toString()));
        asCaseworker.submitExpectingSuccess(new ConfirmOrderReviewRequest(ISSUE, submitted.id(), submitted.version(),
            null, new Issue(new IssuedOrder(APPLICATION_DECISION, submitted.formData(), null, WORDING), List.of(),
                true, false, true, List.of(), COUNTY_COURT)));

        assertThat(jdbcTemplate.queryForObject("SELECT outcome FROM general_application WHERE id = ?", String.class,
            genAppId)).isEqualTo("LISTED");
        assertThat(asJudge.start().caseContext().application().reference()).isEqualTo("GA1");
    }

    private Order judgeSubmits(Map<String, Object> formData) {
        asJudge.submitExpectingSuccess(new MakeOrderRequest(SAVE_DRAFT,
            new OrderChange(null, 0, APPLICATION_DECISION, formData, null, genAppId)));
        Order draft = asJudge.start().order();
        return asJudge.submitExpectingSuccess(new MakeOrderRequest(SUBMIT_FOR_REVIEW, new OrderChange(
                draft.id(), draft.version(), draft.orderType(), draft.formData(), null, null)))
            .changed("draft_orders", Order.class);
    }
}
