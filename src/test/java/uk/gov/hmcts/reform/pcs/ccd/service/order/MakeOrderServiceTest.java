package uk.gov.hmcts.reform.pcs.ccd.service.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.time.Clock;
import java.time.Instant;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MakeOrderService underTest;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(UUID.randomUUID()).build();
        HearingEntity hearing = HearingEntity.builder().id(41).pcsCase(pcsCase).build();
        order = OrderEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
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
    void shouldSaveVersionedDraftPayload() throws Exception {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Action result = underTest.submit(CASE_REFERENCE, envelope("SAVE_DRAFT", """
            {
              "version": 1,
              "orderType": "SUSPENDED_POSSESSION",
              "fields": {"hearing-notes": "Saved note"},
              "documents": {}
            }
            """));

        assertThat(result).isEqualTo(Action.SAVE_DRAFT);
        assertThat(order.getState()).isEqualTo(OrderState.DRAFT);
        JsonNode storedPayload = objectMapper.readTree(order.getDraftPayload());
        assertThat(storedPayload.path("version").asInt()).isEqualTo(1);
        assertThat(storedPayload.path("orderType").asText()).isEqualTo("SUSPENDED_POSSESSION");
        assertThat(storedPayload.path("fields").path("hearing-notes").asText()).isEqualTo("Saved note");
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
              "fields": {},
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
              "fields": {},
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
