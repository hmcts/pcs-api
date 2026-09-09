package uk.gov.hmcts.reform.pcs.ccd.service.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeOrderServiceTest {

    private static final long CASE_REFERENCE = 1777027600017760L;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private AddressMapper addressMapper;

    private final ObjectMapper objectMapper = new JacksonConfiguration().getMapper();
    private MakeOrderService underTest;
    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        pcsCase = PcsCaseEntity.builder().id(UUID.randomUUID()).caseReference(CASE_REFERENCE).build();
        underTest = new MakeOrderService(orderRepository, pcsCaseRepository, objectMapper, addressMapper);
    }

    @Test
    void shouldResumeTheExistingCaseDraft() throws Exception {
        OrderEntity draft = order(OrderState.DRAFT, 2, "{\"orderType\":\"FREE_FORM\",\"notes\":\"saved\"}");
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
        when(orderRepository.findFirstByPcsCaseCaseReferenceAndStateOrderByUpdatedAtDesc(
            CASE_REFERENCE, OrderState.DRAFT)).thenReturn(Optional.of(draft));

        JsonNode result = objectMapper.readTree(underTest.start(CASE_REFERENCE, PCSCase.builder().build()));

        assertThat(result.path("order").path("id").asText()).isEqualTo(draft.getId().toString());
        assertThat(result.path("order").path("draftPayload").path("notes").asText()).isEqualTo("saved");
    }

    @Test
    void shouldProvideTheFactsAndPartiesNeededToMakeAnOrder() throws Exception {
        LocalDate tenancyStartDate = LocalDate.of(2020, 2, 3);
        LocalDate noticeDate = LocalDate.of(2026, 8, 10);
        PCSCase caseData = PCSCase.builder()
            .allClaimants(List.of(
                listValue("claimant-1", Party.builder().orgName("Example Housing").build()),
                listValue("claimant-2", Party.builder().firstName("Alex").lastName("Smith").build())
            ))
            .allDefendants(List.of(listValue("defendant-1", null)))
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                .tenancyLicenceDate(tenancyStartDate)
                .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                .build())
            .noticeServedDetails(NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .postedDate(noticeDate)
                .build())
            .rentDetails(RentDetails.builder()
                .currentRent(new BigDecimal("750.00"))
                .frequency(RentPaymentFrequency.MONTHLY)
                .build())
            .rentArrears(RentArrearsSection.builder().total(new BigDecimal("1500.00")).build())
            .claimGroundSummaries(List.of(
                listValue(null, ClaimGroundSummary.builder().label("Rent arrears").build()),
                listValue(null, ClaimGroundSummary.builder().label("Rent arrears").build()),
                listValue(null, ClaimGroundSummary.builder().label(" ").build()),
                listValue(null, ClaimGroundSummary.builder().build()),
                listValue(null, null)
            ))
            .build();
        givenCaseHasNoDraft();

        JsonNode result = objectMapper.readTree(underTest.start(CASE_REFERENCE, caseData));

        assertThat(result.path("order").path("state").asText()).isEqualTo("DRAFT");
        assertThat(result.path("order").path("version").asLong()).isZero();
        assertThat(result.path("caseContext").path("caseReference").asLong()).isEqualTo(CASE_REFERENCE);
        assertThat(result.path("caseContext").path("claimants").get(0).path("name").asText())
            .isEqualTo("Example Housing");
        assertThat(result.path("caseContext").path("claimants").get(1).path("name").asText())
            .isEqualTo("Alex Smith");
        assertThat(result.path("caseContext").path("defendants").get(0).path("name").asText()).isEmpty();
        JsonNode facts = result.path("caseContext").path("caseFacts");
        assertThat(facts.path("tenancyStartDate").asText()).isEqualTo("2020-02-03");
        assertThat(facts.path("tenancyType").asText()).isEqualTo("ASSURED_TENANCY");
        assertThat(facts.path("noticeDate").asText()).isEqualTo("2026-08-10");
        assertThat(facts.path("currentRent").decimalValue()).isEqualByComparingTo("750.00");
        assertThat(facts.path("rentFrequency").asText()).isEqualTo("MONTHLY");
        assertThat(facts.path("groundsPleaded").asText()).isEqualTo("Rent arrears");
        assertThat(facts.path("arrearsOnIssue").decimalValue()).isEqualByComparingTo("1500.00");
    }

    @Test
    void shouldUseWelshOccupationDetailsWhenThereAreNoTenancyDetails() throws Exception {
        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                .licenceStartDate(LocalDate.of(2024, 4, 5))
                .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
                .build())
            .build();
        givenCaseHasNoDraft();

        JsonNode facts = objectMapper.readTree(underTest.start(CASE_REFERENCE, caseData))
            .path("caseContext").path("caseFacts");

        assertThat(facts.path("tenancyStartDate").asText()).isEqualTo("2024-04-05");
        assertThat(facts.path("tenancyType").asText()).isEqualTo("SECURE_CONTRACT");
        assertThat(facts.path("noticeDate").isMissingNode()).isTrue();
        assertThat(facts.path("currentRent").isMissingNode()).isTrue();
        assertThat(facts.path("rentFrequency").isMissingNode()).isTrue();
        assertThat(facts.path("groundsPleaded").isMissingNode()).isTrue();
        assertThat(facts.path("arrearsOnIssue").isMissingNode()).isTrue();
        assertThat(objectMapper.readTree(underTest.start(CASE_REFERENCE, caseData))
            .path("caseContext").path("claimants").isEmpty()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("noticeDetailsAndDates")
    void shouldExposeTheDateOnWhichNoticeWasServed(NoticeServedDetails notice, LocalDate expectedDate)
        throws Exception {
        givenCaseHasNoDraft();
        PCSCase caseData = PCSCase.builder().noticeServedDetails(notice).build();

        JsonNode facts = objectMapper.readTree(underTest.start(CASE_REFERENCE, caseData))
            .path("caseContext").path("caseFacts");

        assertThat(facts.path("noticeDate").asText()).isEqualTo(expectedDate.toString());
    }

    @Test
    void shouldStartANewDraftWhenTheCaseHasNone() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
        String payload = """
            {"action":"START_DRAFT","order":{"version":0,"draftPayload":{"orderType":"FREE_FORM"}}}
            """;

        underTest.submit(CASE_REFERENCE, payload);

        verify(orderRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(order ->
            order.getPcsCase() == pcsCase
                && order.getState() == OrderState.DRAFT
                && order.getDraftPayload().contains("FREE_FORM")));
    }

    @Test
    void shouldAcceptAStringEncodedEnvelopeAndAnEmptyDraftPayload() throws Exception {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
        String envelope = """
            {"action":"START_DRAFT","order":{"version":0}}
            """;

        Action result = underTest.submit(CASE_REFERENCE, objectMapper.writeValueAsString(envelope));

        assertThat(result).isEqualTo(Action.START_DRAFT);
        verify(orderRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(order ->
            order.getState() == OrderState.DRAFT && order.getDraftPayload().equals("{}")));
    }

    @Test
    void shouldRejectStartingASecondCaseDraft() {
        OrderEntity draft = order(OrderState.DRAFT, 0, "{}");
        when(orderRepository.findFirstByPcsCaseCaseReferenceAndStateOrderByUpdatedAtDesc(
            CASE_REFERENCE, OrderState.DRAFT)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, """
            {"action":"START_DRAFT","order":{"version":0,"draftPayload":{}}}
            """))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("An order draft already exists for this case");
    }

    @Test
    void shouldSaveAndSubmitACaseOwnedDraft() {
        OrderEntity order = order(OrderState.DRAFT, 3, "{}");
        when(orderRepository.findByIdAndPcsCaseCaseReference(order.getId(), CASE_REFERENCE))
            .thenReturn(Optional.of(order));

        Action result = underTest.submit(CASE_REFERENCE, """
            {
              "action":"SUBMIT_FOR_REVIEW",
              "order":{
                "id":"%s",
                "version":3,
                "draftPayload":{"orderType":"OUTRIGHT_POSSESSION","answers":{"possessionDate":"2026-10-01"}}
              }
            }
            """.formatted(order.getId()));

        assertThat(result).isEqualTo(Action.SUBMIT_FOR_REVIEW);
        assertThat(order.getState()).isEqualTo(OrderState.SUBMITTED_FOR_REVIEW);
        assertThat(order.getDraftPayload()).contains("possessionDate");
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    void shouldSaveAChangedDraftWithoutSubmittingItForReview() {
        OrderEntity order = order(OrderState.DRAFT, 3, "{}");
        when(orderRepository.findByIdAndPcsCaseCaseReference(order.getId(), CASE_REFERENCE))
            .thenReturn(Optional.of(order));

        Action result = underTest.submit(CASE_REFERENCE, """
            {
              "action":"SAVE_DRAFT",
              "order":{"id":"%s","version":3,"draftPayload":{"notes":"come back later"}}
            }
            """.formatted(order.getId()));

        assertThat(result).isEqualTo(Action.SAVE_DRAFT);
        assertThat(order.getState()).isEqualTo(OrderState.DRAFT);
        assertThat(order.getDraftPayload()).contains("come back later");
        verify(orderRepository).saveAndFlush(order);
    }

    @ParameterizedTest
    @MethodSource("invalidEnvelopes")
    void shouldRejectAnInvalidOrderEnvelope(String payload, String expectedMessage) {
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, payload))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(expectedMessage);
    }

    @Test
    void shouldRejectAChangeWithoutADraftIdentifier() {
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, """
            {"action":"SAVE_DRAFT","order":{"version":0,"draftPayload":{}}}
            """))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order draft identifier is missing");
    }

    @Test
    void shouldRejectAChangeToADraftThatDoesNotBelongToTheCase() {
        UUID orderId = UUID.randomUUID();

        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, changePayload(orderId, 0)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The order draft does not exist for this case");
    }

    @Test
    void shouldRejectAStaleDraftChange() {
        OrderEntity order = order(OrderState.DRAFT, 4, "{}");
        when(orderRepository.findByIdAndPcsCaseCaseReference(order.getId(), CASE_REFERENCE))
            .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, changePayload(order.getId(), 3)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The order draft has been updated by another user. Reload it and try again");
    }

    @Test
    void shouldRejectAChangeToAnOrderAlreadySubmittedForReview() {
        OrderEntity order = order(OrderState.SUBMITTED_FOR_REVIEW, 4, "{}");
        when(orderRepository.findByIdAndPcsCaseCaseReference(order.getId(), CASE_REFERENCE))
            .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, changePayload(order.getId(), 4)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Only a draft order can be changed");
    }

    @Test
    void shouldRejectAnUnreadableStoredDraft() {
        OrderEntity draft = order(OrderState.DRAFT, 0, "not-json");
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
        when(orderRepository.findFirstByPcsCaseCaseReferenceAndStateOrderByUpdatedAtDesc(
            CASE_REFERENCE, OrderState.DRAFT)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> underTest.start(CASE_REFERENCE, PCSCase.builder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The stored order draft payload is not valid JSON");
    }

    private void givenCaseHasNoDraft() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
        when(orderRepository.findFirstByPcsCaseCaseReferenceAndStateOrderByUpdatedAtDesc(
            CASE_REFERENCE, OrderState.DRAFT)).thenReturn(Optional.empty());
    }

    private static Stream<Arguments> noticeDetailsAndDates() {
        LocalDate date = LocalDate.of(2026, 8, 10);
        LocalDateTime dateTime = date.atTime(12, 30);
        return Stream.of(
            Arguments.of(NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST).postedDate(date).build(), date),
            Arguments.of(NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.DELIVERED_PERMITTED_PLACE).deliveredDate(date).build(), date),
            Arguments.of(NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.PERSONALLY_HANDED).handedOverDateTime(dateTime).build(), date),
            Arguments.of(NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.EMAIL).emailSentDateTime(dateTime).build(), date),
            Arguments.of(NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.OTHER_ELECTRONIC).otherElectronicDateTime(dateTime).build(), date),
            Arguments.of(NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.OTHER).otherDateTime(dateTime).build(), date)
        );
    }

    private static Stream<Arguments> invalidEnvelopes() {
        return Stream.of(
            Arguments.of(null, "The order draft payload is missing"),
            Arguments.of(" ", "The order draft payload is missing"),
            Arguments.of("not-json", "The order draft payload is not valid JSON"),
            Arguments.of("{}", "The order action is missing"),
            Arguments.of("{\"action\":\"SAVE_DRAFT\"}", "The order is missing")
        );
    }

    private static String changePayload(UUID orderId, long version) {
        return """
            {"action":"SAVE_DRAFT","order":{"id":"%s","version":%d,"draftPayload":{}}}
            """.formatted(orderId, version);
    }

    private static <T> ListValue<T> listValue(String id, T value) {
        return ListValue.<T>builder().id(id).value(value).build();
    }

    private OrderEntity order(OrderState state, long version, String payload) {
        return OrderEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
            .state(state)
            .version(version)
            .draftPayload(payload)
            .build();
    }
}
