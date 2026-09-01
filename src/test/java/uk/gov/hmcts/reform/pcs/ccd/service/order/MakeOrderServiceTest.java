package uk.gov.hmcts.reform.pcs.ccd.service.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeOrderServiceTest {

    private static final long CASE_REFERENCE = 1777027600017760L;
    private static final Clock UK_CLOCK = Clock.fixed(
        Instant.parse("2026-09-01T09:00:00Z"), ZoneId.of("Europe/London"));

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private AddressMapper addressMapper;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private MakeOrderService underTest;
    private OrderEntity order;
    private HearingEntity hearing;
    private PcsCaseEntity pcsCaseEntity;

    @BeforeEach
    void setUp() {
        pcsCaseEntity = PcsCaseEntity.builder().id(UUID.randomUUID()).build();
        hearing = HearingEntity.builder().id(41).pcsCase(pcsCaseEntity).build();
        order = OrderEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCaseEntity)
            .hearing(hearing)
            .state(OrderState.DRAFT)
            .version(2)
            .build();

        when(hearingRepository.findActiveHearingsBetween(
            CASE_REFERENCE,
            LocalDateTime.of(2026, 9, 1, 0, 0),
            LocalDateTime.of(2026, 9, 2, 0, 0)
        )).thenReturn(List.of(hearing));

        underTest = new MakeOrderService(
            orderRepository, hearingRepository, objectMapper, addressMapper, UK_CLOCK);
    }

    @Test
    void shouldExposeKnownCaseFacts() throws Exception {
        when(orderRepository.findFirstByPcsCase_IdAndHearing_IdAndStateOrderByCreatedAtDesc(
            pcsCaseEntity.getId(), hearing.getId(), OrderState.DRAFT)).thenReturn(Optional.of(order));
        PCSCase pcsCase = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                .tenancyLicenceDate(LocalDate.of(2024, 1, 9))
                .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                .build())
            .noticeServedDetails(NoticeServedDetails.builder()
                .serviceMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .postedDate(LocalDate.of(2025, 6, 12))
                .build())
            .rentDetails(RentDetails.builder()
                .currentRent(new BigDecimal("750.00"))
                .frequency(RentPaymentFrequency.MONTHLY)
                .build())
            .rentArrears(RentArrearsSection.builder().total(new BigDecimal("2400.00")).build())
            .claimGroundSummaries(List.of(ListValue.<ClaimGroundSummary>builder()
                .value(ClaimGroundSummary.builder().label("Ground 8").build())
                .build()))
            .build();

        JsonNode caseFacts = objectMapper.readTree(underTest.start(CASE_REFERENCE, pcsCase))
            .path("caseContext").path("caseFacts");

        assertThat(caseFacts.path("tenancyStartDate").asText()).isEqualTo("2024-01-09");
        assertThat(caseFacts.path("tenancyType").asText()).isEqualTo("ASSURED_TENANCY");
        assertThat(caseFacts.path("noticeDate").asText()).isEqualTo("2025-06-12");
        assertThat(caseFacts.path("currentRent").decimalValue()).isEqualByComparingTo("750.00");
        assertThat(caseFacts.path("rentFrequency").asText()).isEqualTo("MONTHLY");
        assertThat(caseFacts.path("groundsPleaded").asText()).isEqualTo("Ground 8");
        assertThat(caseFacts.path("arrearsOnIssue").decimalValue()).isEqualByComparingTo("2400.00");
    }

    @Test
    void shouldSaveVersionedDraftPayload() throws Exception {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Action result = underTest.submit(CASE_REFERENCE, envelope("SAVE_DRAFT", """
            {
              "version": 1,
              "orderType": "SUSPENDED_POSSESSION",
              "formData": {"hearing-notes": "Saved note"},
              "documents": {}
            }
            """));

        assertThat(result).isEqualTo(Action.SAVE_DRAFT);
        assertThat(order.getState()).isEqualTo(OrderState.DRAFT);
        JsonNode storedPayload = objectMapper.readTree(order.getDraftPayload());
        assertThat(storedPayload.path("version").asInt()).isEqualTo(1);
        assertThat(storedPayload.path("orderType").asText()).isEqualTo("SUSPENDED_POSSESSION");
        assertThat(storedPayload.path("formData").path("hearing-notes").asText()).isEqualTo("Saved note");
        assertThat(storedPayload.path("documents").isObject()).isTrue();
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    void shouldSubmitValidOutrightPossessionDocumentForReview() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Action result = underTest.submit(CASE_REFERENCE, envelope("SUBMIT_FOR_REVIEW", """
            {
              "version": 1,
              "orderType": "OUTRIGHT_POSSESSION",
              "formData": {},
              "documents": {
                "OUTRIGHT_POSSESSION": {
                  "schema": "docweave-document",
                  "version": 1,
                  "current": {"type": "doc"},
                  "generated": {"type": "doc"}
                }
              }
            }
            """));

        assertThat(result).isEqualTo(Action.SUBMIT_FOR_REVIEW);
        assertThat(order.getState()).isEqualTo(OrderState.SUBMITTED_FOR_REVIEW);
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    void shouldRejectSubmissionWithoutValidOutrightPossessionDocument() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, envelope("SUBMIT_FOR_REVIEW", """
            {
              "version": 1,
              "orderType": "OUTRIGHT_POSSESSION",
              "formData": {},
              "documents": {}
            }
            """)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The outright possession order document is invalid");
    }

    @Test
    void shouldStartANewEmptyDraftWhenTodaysHearingHasNoDraft() throws Exception {
        when(orderRepository.findFirstByPcsCase_IdAndHearing_IdAndStateOrderByCreatedAtDesc(
            pcsCaseEntity.getId(), hearing.getId(), OrderState.DRAFT)).thenReturn(Optional.empty());
        AddressUK address = AddressUK.builder().addressLine1("8 Court Road").postCode("CF10 1AA").build();
        when(addressMapper.toAddressUK(pcsCaseEntity.getPropertyAddress())).thenReturn(address);

        JsonNode envelope = objectMapper.readTree(underTest.start(CASE_REFERENCE, PCSCase.builder().build()));

        assertThat(envelope.path("order").path("id").isNull()).isTrue();
        assertThat(envelope.path("order").path("state").asText()).isEqualTo("DRAFT");
        assertThat(envelope.path("order").path("draftPayload").path("version").asInt()).isEqualTo(1);
        assertThat(envelope.path("order").path("draftPayload").path("orderType").asText())
            .isEqualTo("OUTRIGHT_POSSESSION");
        assertThat(envelope.path("order").path("draftPayload").path("formData").isObject()).isTrue();
        assertThat(envelope.path("caseContext").path("caseReference").asLong()).isEqualTo(CASE_REFERENCE);
        assertThat(envelope.path("caseContext").path("propertyAddress").path("AddressLine1").asText())
            .isEqualTo("8 Court Road");
        assertThat(envelope.path("caseContext").path("claimants").isEmpty()).isTrue();
        assertThat(envelope.path("caseContext").path("defendants").isEmpty()).isTrue();
    }

    @Test
    void shouldExposeWelshOccupationDetailsAndHumanReadablePartyNames() throws Exception {
        existingDraft("{}");
        PCSCase pcsCase = PCSCase.builder()
            .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                .licenceStartDate(LocalDate.of(2023, 3, 4))
                .occupationLicenceTypeWales(OccupationLicenceTypeWales.STANDARD_CONTRACT)
                .build())
            .allClaimants(List.of(
                ListValue.<Party>builder().id("claimant-1")
                    .value(Party.builder().orgName("Housing Group").firstName("Ignored").build()).build(),
                ListValue.<Party>builder().id("claimant-2")
                    .value(Party.builder().firstName("Alex").lastName("Smith").build()).build()
            ))
            .allDefendants(List.of(
                ListValue.<Party>builder().id("defendant-1").value(null).build()
            ))
            .claimGroundSummaries(List.of(
                ListValue.<ClaimGroundSummary>builder().value(ClaimGroundSummary.builder().label("Ground 8").build())
                    .build(),
                ListValue.<ClaimGroundSummary>builder().value(ClaimGroundSummary.builder().label(" ").build()).build(),
                ListValue.<ClaimGroundSummary>builder().value(null).build(),
                ListValue.<ClaimGroundSummary>builder().value(ClaimGroundSummary.builder().label("Ground 8").build())
                    .build(),
                ListValue.<ClaimGroundSummary>builder().value(ClaimGroundSummary.builder().label("Ground 11").build())
                    .build()
            ))
            .build();

        JsonNode context = objectMapper.readTree(underTest.start(CASE_REFERENCE, pcsCase)).path("caseContext");

        assertThat(context.path("caseFacts").path("tenancyStartDate").asText()).isEqualTo("2023-03-04");
        assertThat(context.path("caseFacts").path("tenancyType").asText()).isEqualTo("STANDARD_CONTRACT");
        assertThat(context.path("caseFacts").path("groundsPleaded").asText()).isEqualTo("Ground 8, Ground 11");
        assertThat(context.path("claimants").get(0).path("name").asText()).isEqualTo("Housing Group");
        assertThat(context.path("claimants").get(1).path("name").asText()).isEqualTo("Alex Smith");
        assertThat(context.path("defendants").get(0).path("name").asText()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "FIRST_CLASS_POST,2026-01-01",
        "DELIVERED_PERMITTED_PLACE,2026-02-02",
        "PERSONALLY_HANDED,2026-03-03",
        "EMAIL,2026-04-04",
        "OTHER_ELECTRONIC,2026-05-05",
        "OTHER,2026-06-06"
    })
    void shouldExposeTheRelevantNoticeDate(NoticeServiceMethod method, String expectedDate) throws Exception {
        existingDraft("{}");
        NoticeServedDetails notice = NoticeServedDetails.builder()
            .serviceMethod(method)
            .postedDate(LocalDate.of(2026, 1, 1))
            .deliveredDate(LocalDate.of(2026, 2, 2))
            .handedOverDateTime(LocalDateTime.of(2026, 3, 3, 10, 0))
            .emailSentDateTime(LocalDateTime.of(2026, 4, 4, 10, 0))
            .otherElectronicDateTime(LocalDateTime.of(2026, 5, 5, 10, 0))
            .otherDateTime(LocalDateTime.of(2026, 6, 6, 10, 0))
            .build();

        JsonNode facts = objectMapper.readTree(underTest.start(
            CASE_REFERENCE, PCSCase.builder().noticeServedDetails(notice).build()))
            .path("caseContext").path("caseFacts");

        assertThat(facts.path("noticeDate").asText()).isEqualTo(expectedDate);
    }

    @Test
    void shouldPersistANewDraftOnlyWhenTheStartActionIsSubmitted() {
        when(orderRepository.findFirstByPcsCase_IdAndHearing_IdAndStateOrderByCreatedAtDesc(
            pcsCaseEntity.getId(), hearing.getId(), OrderState.DRAFT)).thenReturn(Optional.empty());

        Action result = underTest.submit(CASE_REFERENCE, """
            {"action":"START_DRAFT","order":{"version":0}}
            """);

        assertThat(result).isEqualTo(Action.START_DRAFT);
        verify(orderRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(saved ->
            saved.getPcsCase().equals(pcsCaseEntity)
                && saved.getHearing().equals(hearing)
                && saved.getState() == OrderState.DRAFT
                && saved.getDraftPayload().contains("\"OUTRIGHT_POSSESSION\"")
        ));
    }

    @Test
    void shouldRejectStartingASecondDraftForTheSameHearing() {
        when(orderRepository.findFirstByPcsCase_IdAndHearing_IdAndStateOrderByCreatedAtDesc(
            pcsCaseEntity.getId(), hearing.getId(), OrderState.DRAFT)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, """
            {"action":"START_DRAFT","order":{"version":0}}
            """))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("An order draft already exists for today's hearing");
    }

    @Test
    void shouldAcceptAJsonEncodedEnvelopeFromCcd() throws Exception {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        String encodedEnvelope = objectMapper.valueToTree(envelope("SAVE_DRAFT", """
            {"version":1,"orderType":"FREE_FORM","formData":{},"documents":{}}
            """)).toString();

        Action result = underTest.submit(CASE_REFERENCE, encodedEnvelope);

        assertThat(result).isEqualTo(Action.SAVE_DRAFT);
        assertThat(objectMapper.readTree(order.getDraftPayload()).path("orderType").asText()).isEqualTo("FREE_FORM");
    }

    @Test
    void shouldRejectMalformedOrIncompleteEnvelopeData() {
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, "not-json"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order draft payload is not valid JSON");
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order draft payload is missing");
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, "{\"order\":{}}"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order action is missing");
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, "{\"action\":\"SAVE_DRAFT\"}"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order is missing");
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, """
            {"action":"SAVE_DRAFT","order":{"version":0}}
            """))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The order draft identifier is missing");
    }

    @Test
    void shouldRejectAStaleDraftOrOneForADifferentHearing() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        order.setVersion(3);

        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, envelopeWithVersion(2)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The order draft has been updated by another user. Reload it and try again");

        order.setVersion(2);
        order.setHearing(HearingEntity.builder().id(99).pcsCase(pcsCaseEntity).build());
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, envelopeWithVersion(2)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The order draft does not belong to today's hearing");

        order.setHearing(hearing);
        order.setState(OrderState.SUBMITTED_FOR_REVIEW);
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, envelopeWithVersion(2)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Only a draft order can be changed");
    }

    @Test
    void shouldRequireExactlyOneHearingToday() {
        when(hearingRepository.findActiveHearingsBetween(
            CASE_REFERENCE,
            LocalDateTime.of(2026, 9, 1, 0, 0),
            LocalDateTime.of(2026, 9, 2, 0, 0)
        )).thenReturn(List.of());

        assertThatThrownBy(() -> underTest.start(CASE_REFERENCE, PCSCase.builder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No hearing is listed for this case today");

        when(hearingRepository.findActiveHearingsBetween(
            CASE_REFERENCE,
            LocalDateTime.of(2026, 9, 1, 0, 0),
            LocalDateTime.of(2026, 9, 2, 0, 0)
        )).thenReturn(List.of(hearing, hearing));
        assertThatThrownBy(() -> underTest.start(CASE_REFERENCE, PCSCase.builder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("More than one hearing is listed for this case today");
    }

    @Test
    void shouldRejectInvalidDraftShapesBeforeSaving() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertInvalidDraft("{\"version\":2,\"orderType\":\"FREE_FORM\",\"formData\":{},\"documents\":{}}",
            "The order draft payload version is not supported");
        assertInvalidDraft("{\"version\":1,\"formData\":{},\"documents\":{}}",
            "The order type is missing");
        assertInvalidDraft("{\"version\":1,\"orderType\":\"FREE_FORM\",\"formData\":[],\"documents\":{}}",
            "The order draft form data must be a JSON object");
        assertInvalidDraft("{\"version\":1,\"orderType\":\"FREE_FORM\",\"formData\":{},\"documents\":null}",
            "The order documents must be JSON objects");
        assertInvalidDraft(
            "{\"version\":1,\"orderType\":\"FREE_FORM\",\"formData\":{},\"documents\":{\"FREE_FORM\":null}}",
            "The order documents must be JSON objects");
    }

    @Test
    void shouldRejectAnUnreadableStoredDraft() {
        existingDraft("{");

        assertThatThrownBy(() -> underTest.start(CASE_REFERENCE, PCSCase.builder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The stored order draft payload is not valid JSON");
    }

    private void existingDraft(String draftPayload) {
        order.setDraftPayload(draftPayload);
        when(orderRepository.findFirstByPcsCase_IdAndHearing_IdAndStateOrderByCreatedAtDesc(
            pcsCaseEntity.getId(), hearing.getId(), OrderState.DRAFT)).thenReturn(Optional.of(order));
    }

    private String envelopeWithVersion(long version) {
        return """
            {
              "action":"SAVE_DRAFT",
              "order":{
                "id":"%s",
                "version":%d,
                "draftPayload":{"version":1,"orderType":"FREE_FORM","formData":{},"documents":{}}
              }
            }
            """.formatted(order.getId(), version);
    }

    private void assertInvalidDraft(String draftPayload, String expectedMessage) {
        assertThatThrownBy(() -> underTest.submit(CASE_REFERENCE, envelope("SAVE_DRAFT", draftPayload)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(expectedMessage);
    }

    private String envelope(String action, String draftPayload) {
        return """
            {
              "action": "%s",
              "order": {
                "id": "%s",
                "version": %d,
                "draftPayload": %s
              }
            }
            """.formatted(action, order.getId(), order.getVersion(), draftPayload);
    }
}
