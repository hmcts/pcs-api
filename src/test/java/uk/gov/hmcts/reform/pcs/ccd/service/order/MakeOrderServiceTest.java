package uk.gov.hmcts.reform.pcs.ccd.service.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.Optional;
import java.util.UUID;

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

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
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
