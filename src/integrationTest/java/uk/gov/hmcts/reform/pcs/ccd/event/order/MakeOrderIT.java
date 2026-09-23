package uk.gov.hmcts.reform.pcs.ccd.event.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.ccd.sdk.testing.ActorDetails;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.EnableCcdEventTesting;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.am.RoleAssignment;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@EnableCcdEventTesting
@DisplayName("Make an order")
class MakeOrderIT extends AbstractPostgresContainerIT {

    private static final String CREATE_CLAIM = "createPossessionClaim";
    private static final String RESUME_CLAIM = "resumePossessionClaim";
    // pcs-api answers an IllegalStateException with 409, which is what CCD would receive.
    private static final String CONFLICT = "returned HTTP 409";

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private IdamUserInfoApi idamUserInfoApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private RoleAssignmentApi roleAssignmentApi;
    @MockitoBean
    private RdProfessionalApi rdProfessionalApi;
    @MockitoBean(name = "prdAdminTokenProvider")
    private IdamTokenProvider prdAdminTokenProvider;

    private CcdEventTestSupport<PCSCase, State>.CaseType cases;
    private User firstJudge;
    private User secondJudge;
    private User claimant;
    private long caseReference;

    @BeforeEach
    void setUp() throws Exception {
        cases = events.forCaseType(CaseType.getCaseType());
        firstJudge = user("First", "Judge", List.of("caseworker-pcs"));
        secondJudge = user("Second", "Judge", List.of("caseworker-pcs"));
        claimant = user("Claimant", "Solicitor", List.of("caseworker-pcs", "caseworker-pcs-solicitor"));
        // Only the claimant belongs to an organisation; judges get no organisational roles.
        when(roleAssignmentApi.getRoles(any(), any(), eq(claimant.id().toString())))
            .thenReturn(new RoleAssignmentResponse(List.of(RoleAssignment.builder()
                .roleName(UserRole.GA_CLAIMANT_SOLICITOR.getRole())
                .grantType("STANDARD")
                .build())));
        when(rdProfessionalApi.getOrganisationDetails(eq(claimant.id().toString()), any(), any()))
            .thenReturn(OrganisationDetailsResponse.builder()
                .name("Possession Claims Solicitor Org")
                .organisationIdentifier("ORG-MAKE-ORDER")
                .organisationProfileIds(List.of("SOLICITOR_PROFILE"))
                .build());
        caseReference = issueClaim(baseClaim(LegislativeCountry.ENGLAND));
    }

    @Test
    @DisplayName("gives the judge the facts and parties from the claim")
    void providesTheFactsAndPartiesFromTheClaim() throws Exception {
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

        JsonNode opened = open(firstJudge);

        assertThat(opened.at("/order/state").asText()).isEqualTo("DRAFT");
        assertThat(opened.at("/order/version").asLong()).isZero();
        JsonNode context = opened.path("caseContext");
        assertThat(context.path("caseReference").asLong()).isEqualTo(caseReference);
        assertThat(context.path("claimants").findValuesAsText("name"))
            .containsExactly("Possession Claims Solicitor Org");
        assertThat(context.path("defendants").findValuesAsText("name")).containsExactly("Jane Doe");
        JsonNode facts = context.path("caseFacts");
        assertThat(facts.path("tenancyStartDate").asText()).isEqualTo("2020-02-03");
        assertThat(facts.path("tenancyType").asText()).isEqualTo("SECURE_TENANCY");
        assertThat(facts.path("noticeDate").asText()).isEqualTo("2026-08-10");
        assertThat(facts.path("currentRent").decimalValue()).isEqualByComparingTo("750.00");
        assertThat(facts.path("rentFrequency").asText()).isEqualTo("MONTHLY");
        assertThat(facts.path("arrearsOnIssue").decimalValue()).isEqualByComparingTo("1500.00");
        assertThat(facts.path("groundsPleaded").asText()).contains("Rent arrears");
    }

    @Test
    @DisplayName("uses the Welsh occupation contract when the claim has no tenancy")
    void usesTheWelshOccupationContract() throws Exception {
        PCSCase claim = baseClaim(LegislativeCountry.WALES);
        claim.getOccupationLicenceDetailsWales().setLicenceStartDate(LocalDate.of(2024, 4, 5));
        caseReference = issueClaim(claim);

        JsonNode facts = open(firstJudge).at("/caseContext/caseFacts");

        assertThat(facts.path("tenancyStartDate").asText()).isEqualTo("2024-04-05");
        assertThat(facts.path("tenancyType").asText()).isEqualTo("SECURE_CONTRACT");
        assertThat(facts.has("noticeDate")).isFalse();
    }

    @Test
    @DisplayName("gives each judge their own draft on the same case")
    void eachJudgeKeepsTheirOwnDraft() {
        submit(firstJudge, "START_DRAFT", null, 0, "first judge's notes");
        submit(secondJudge, "START_DRAFT", null, 0, "second judge's notes");

        assertThat(notesIn(open(firstJudge))).isEqualTo("first judge's notes");
        assertThat(notesIn(open(secondJudge))).isEqualTo("second judge's notes");
        assertThat(draftOf(firstJudge).orElseThrow().getId()).isNotEqualTo(draftOf(secondJudge).orElseThrow().getId());
    }

    @Test
    @DisplayName("does not let a judge change another judge's draft")
    void rejectsAChangeToAnotherJudgesDraft() {
        submit(firstJudge, "START_DRAFT", null, 0, "first judge's notes");
        JsonNode firstJudgesDraft = open(firstJudge).path("order");

        assertThatThrownBy(() -> submit(secondJudge, "SAVE_DRAFT",
            firstJudgesDraft.path("id").asText(), firstJudgesDraft.path("version").asLong(), "overwritten"))
            .hasMessageContaining(CONFLICT);
        assertThat(notesIn(open(firstJudge))).isEqualTo("first judge's notes");
    }

    @Test
    @DisplayName("does not let a judge start a second draft on the same case")
    void rejectsASecondDraftForTheSameJudge() {
        submit(firstJudge, "START_DRAFT", null, 0, "first draft");

        assertThatThrownBy(() -> submit(firstJudge, "START_DRAFT", null, 0, "second draft"))
            .hasMessageContaining(CONFLICT);
        assertThat(notesIn(open(firstJudge))).isEqualTo("first draft");
    }

    @Test
    @DisplayName("saves a changed draft without submitting it")
    void savesADraft() {
        submit(firstJudge, "START_DRAFT", null, 0, "first version");
        JsonNode draft = open(firstJudge).path("order");

        submit(firstJudge, "SAVE_DRAFT", draft.path("id").asText(), draft.path("version").asLong(), "second version");

        JsonNode saved = open(firstJudge).path("order");
        assertThat(saved.path("id").asText()).isEqualTo(draft.path("id").asText());
        assertThat(saved.path("state").asText()).isEqualTo("DRAFT");
        assertThat(saved.path("version").asLong()).isGreaterThan(draft.path("version").asLong());
        assertThat(saved.at("/draftPayload/notes").asText()).isEqualTo("second version");
    }

    @Test
    @DisplayName("rejects a change made from an out-of-date copy of the draft")
    void rejectsAStaleChange() {
        submit(firstJudge, "START_DRAFT", null, 0, "first version");
        JsonNode draft = open(firstJudge).path("order");
        submit(firstJudge, "SAVE_DRAFT", draft.path("id").asText(), draft.path("version").asLong(), "second version");

        assertThatThrownBy(() -> submit(firstJudge, "SAVE_DRAFT",
            draft.path("id").asText(), draft.path("version").asLong(), "stale version"))
            .hasMessageContaining(CONFLICT);
        assertThat(notesIn(open(firstJudge))).isEqualTo("second version");
    }

    @Test
    @DisplayName("records the judge who submits an order for review in the order and its audit")
    void recordsTheJudgeWhoSubmitsForReview() {
        submit(firstJudge, "START_DRAFT", null, 0, "draft");
        JsonNode draft = open(firstJudge).path("order");

        var submission = submit(firstJudge, "SUBMIT_FOR_REVIEW",
            draft.path("id").asText(), draft.path("version").asLong(), "final");

        OrderEntity submitted = orderRepository.findById(UUID.fromString(draft.path("id").asText())).orElseThrow();
        assertThat(submitted.getState()).isEqualTo(OrderState.SUBMITTED_FOR_REVIEW);
        assertThat(submitted.getIdamUserId()).isEqualTo(firstJudge.id());
        assertThat(submitted.getDraftPayload()).contains("final");
        assertThat(jdbc.queryForList("""
            select new_values ->> 'idam_user_id' from ccd.audit_log
            where case_event_id = ? and table_name = 'orders' and operation = 'UPDATE'
            """, String.class, submission.audit().id()))
            .containsExactly(firstJudge.id().toString());
    }

    @Test
    @DisplayName("does not let an order submitted for review be changed")
    void rejectsAChangeToASubmittedOrder() {
        submit(firstJudge, "START_DRAFT", null, 0, "draft");
        JsonNode draft = open(firstJudge).path("order");
        submit(firstJudge, "SUBMIT_FOR_REVIEW", draft.path("id").asText(), draft.path("version").asLong(), "final");
        long submittedVersion = orderRepository.findById(UUID.fromString(draft.path("id").asText()))
            .orElseThrow().getVersion();

        assertThatThrownBy(() -> submit(firstJudge, "SAVE_DRAFT",
            draft.path("id").asText(), submittedVersion, "changed after submission"))
            .hasMessageContaining(CONFLICT);
    }

    @Test
    @DisplayName("lets a judge start a new draft once their order is submitted for review")
    void startsANewDraftAfterSubmission() {
        submit(firstJudge, "START_DRAFT", null, 0, "draft");
        JsonNode draft = open(firstJudge).path("order");
        submit(firstJudge, "SUBMIT_FOR_REVIEW", draft.path("id").asText(), draft.path("version").asLong(), "final");

        assertThat(open(firstJudge).path("order").hasNonNull("id")).isFalse();
        submit(firstJudge, "START_DRAFT", null, 0, "next order");

        assertThat(notesIn(open(firstJudge))).isEqualTo("next order");
    }

    @Test
    @DisplayName("rejects an order change that does not say which draft it is for")
    void rejectsAChangeWithoutADraftIdentifier() {
        assertThatThrownBy(() -> submit(firstJudge, "SAVE_DRAFT", null, 0, "notes"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order draft identifier is missing");
    }

    @Test
    @DisplayName("rejects an order submission with no action or no order")
    void rejectsAnIncompleteEnvelope() {
        assertThatThrownBy(() -> submitEnvelope(firstJudge, objectMapper.createObjectNode()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order action is missing");
        ObjectNode withoutOrder = objectMapper.createObjectNode().put("action", "SAVE_DRAFT");
        assertThatThrownBy(() -> submitEnvelope(firstJudge, withoutOrder))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order is missing");
    }

    /** Creates a claim through pcs-api's own claim events, then issues it as the payment flow would. */
    private long issueClaim(PCSCase claim) {
        var create = cases.create(CREATE_CLAIM, State.AWAITING_SUBMISSION_TO_HMCTS, PCSCase.builder()
                .propertyAddress(claim.getPropertyAddress())
                .legislativeCountry(claim.getLegislativeCountry())
                .build())
            .as(claimant.actor());
        create.submitExpectingSuccess();
        cases.event(create.reference(), RESUME_CLAIM, claim).as(claimant.actor()).submitExpectingSuccess();
        jdbc.update("update ccd.case_data set state = 'CASE_ISSUED' where reference = ?", create.reference());
        return create.reference();
    }

    /** The claim that pcs-api's testing support submits, with the property address it creates cases with. */
    private PCSCase baseClaim(LegislativeCountry country) throws Exception {
        try (var payload = getClass().getClassLoader()
            .getResourceAsStream("testing-support/Create-Case-" + country + "-Base.json")) {
            PCSCase claim = objectMapper.readValue(payload, PCSCase.class);
            AddressUK address = country == LegislativeCountry.WALES
                ? AddressUK.builder()
                    .addressLine1("2 Pentre Street")
                    .postTown("Caerdydd")
                    .postCode("CF11 6QX")
                    .country("Deyrnas Unedig")
                    .build()
                : AddressUK.builder()
                    .addressLine1("1 Second Avenue")
                    .postTown("London")
                    .postCode("W3 7RX")
                    .country("United Kingdom")
                    .build();
            claim.setLegislativeCountry(country);
            claim.setPropertyAddress(address);
            return claim;
        }
    }

    /** Opens the event as the judge and returns the envelope the start handler gives the frontend. */
    private JsonNode open(User judge) {
        PCSCase started = cases.start(caseReference, MakeOrder.EVENT_ID).as(judge.actor()).startExpectingSuccess()
            .caseData();
        try {
            return objectMapper.readTree(started.getMakeOrderPayload());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String notesIn(JsonNode opened) {
        return opened.at("/order/draftPayload/notes").asText();
    }

    private Optional<OrderEntity> draftOf(User judge) {
        return orderRepository.findFirstByPcsCaseCaseReferenceAndIdamUserIdAndStateOrderByUpdatedAtDesc(
            caseReference, judge.id(), OrderState.DRAFT);
    }

    private CcdEventTestSupport<PCSCase, State>.Accepted submit(User judge, String action, String orderId,
                                                                long version, String notes) {
        ObjectNode order = objectMapper.createObjectNode().put("version", version);
        if (orderId != null) {
            order.put("id", orderId);
        }
        order.putObject("draftPayload").put("notes", notes);
        ObjectNode envelope = objectMapper.createObjectNode().put("action", action);
        envelope.set("order", order);
        return submitEnvelope(judge, envelope);
    }

    private CcdEventTestSupport<PCSCase, State>.Accepted submitEnvelope(User judge, ObjectNode envelope) {
        PCSCase submitted = PCSCase.builder().makeOrderPayload(envelope.toString()).build();
        return cases.event(caseReference, MakeOrder.EVENT_ID, submitted).as(judge.actor()).submitExpectingSuccess();
    }

    private User user(String givenName, String familyName, List<String> roles) {
        UUID id = UUID.randomUUID();
        var actor = events.registerActor(new ActorDetails(id.toString(),
            givenName.toLowerCase() + "." + familyName.toLowerCase() + "@example.com", givenName, familyName, roles));
        when(idamUserInfoApi.getUserInfo(actor.authorisation())).thenReturn(UserInfo.builder()
            .uid(id.toString())
            .sub(actor.details().email())
            .givenName(givenName)
            .familyName(familyName)
            .roles(roles)
            .build());
        return new User(id, actor);
    }

    private record User(UUID id, CcdEventTestSupport.Actor actor) {
    }
}
