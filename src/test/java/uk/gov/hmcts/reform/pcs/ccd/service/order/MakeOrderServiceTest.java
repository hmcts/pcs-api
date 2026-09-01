package uk.gov.hmcts.reform.pcs.ccd.service.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
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
