package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.DRAFT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.SUBMITTED_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType.ADJOURNMENT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType.FREE_FORM;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType.OUTRIGHT_POSSESSION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType.SUSPENDED_POSSESSION;

@PcsCcdEventTest
@Transactional
@DisplayName("The latest issued order on a case")
class OrderStepRepositoryIT extends AbstractPostgresContainerIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStepRepository orderStepRepository;

    private PcsCaseEntity pcsCase;
    private int judges;

    @BeforeEach
    void setUp() {
        // Orders are audited against the CCD case event that writes them; these are written outside one.
        jdbcTemplate.queryForObject("select set_config('ccd.audit_disabled', 'true', true)", String.class);
        pcsCase = pcsCaseRepository.saveAndFlush(PcsCaseEntity.builder()
            .caseReference(1234567890123456L)
            .build());
    }

    @Test
    @DisplayName("is absent when the case has no orders")
    void absentWithNoOrders() {
        assertThat(orderStepRepository.findLatestIssuedByCaseId(pcsCase.getId())).isEmpty();
    }

    @Test
    @DisplayName("is absent when the case's only order has not been issued")
    void absentWithOnlyUnissuedOrders() {
        order(DRAFT, SUSPENDED_POSSESSION);
        order(SUBMITTED_FOR_REVIEW, SUSPENDED_POSSESSION);

        assertThat(orderStepRepository.findLatestIssuedByCaseId(pcsCase.getId())).isEmpty();
    }

    @Test
    @DisplayName("is the issued order, whatever a later order being drafted says")
    void ignoresLaterDrafts() {
        order(ISSUED, SUSPENDED_POSSESSION);
        order(DRAFT, OUTRIGHT_POSSESSION);

        assertThat(latestIssuedOrderType()).isEqualTo(SUSPENDED_POSSESSION);
    }

    @Test
    @DisplayName("is the most recently issued, even when it makes no order on the claim")
    void latestIssuedWins() {
        order(ISSUED, SUSPENDED_POSSESSION);
        order(ISSUED, ADJOURNMENT);

        assertThat(latestIssuedOrderType()).isEqualTo(ADJOURNMENT);
    }

    @Test
    @DisplayName("is the type the judge chose, so a free-form order stays free-form")
    void freeFormStaysFreeForm() {
        order(ISSUED, OUTRIGHT_POSSESSION);
        order(ISSUED, FREE_FORM);

        assertThat(latestIssuedOrderType()).isEqualTo(FREE_FORM);
    }

    @Test
    @DisplayName("is not another case's order")
    void onlyThisCase() {
        PcsCaseEntity otherCase = pcsCaseRepository.saveAndFlush(PcsCaseEntity.builder()
            .caseReference(6543210987654321L)
            .build());
        order(ISSUED, OUTRIGHT_POSSESSION);
        order(otherCase, ISSUED, SUSPENDED_POSSESSION);

        assertThat(latestIssuedOrderType()).isEqualTo(OUTRIGHT_POSSESSION);
    }

    private OrderType latestIssuedOrderType() {
        return orderStepRepository.findLatestIssuedByCaseId(pcsCase.getId())
            .map(OrderStepEntity::getOrderType)
            .orElseThrow();
    }

    private void order(OrderState state, OrderType type) {
        order(pcsCase, state, type);
    }

    /** An order by its own judge, so drafts don't collide, whose one step leaves it in the given state. */
    private void order(PcsCaseEntity onCase, OrderState state, OrderType type) {
        String judge = "judge-" + ++judges;
        OrderEntity order = OrderEntity.builder()
            .pcsCase(onCase)
            .authorIdamUserId(judge)
            .build();
        order.moveTo(state);
        orderRepository.saveAndFlush(order);
        orderStepRepository.saveAndFlush(OrderStepEntity.builder()
            .order(order)
            .state(state)
            .actorIdamUserId(judge)
            .orderType(type)
            .formData(Map.of())
            .orderHtml(state == ISSUED ? "<p>Order</p>" : null)
            .build());
    }
}
