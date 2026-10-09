package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;

import java.util.List;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * An order on a case. Its content is on its steps ({@link OrderStepEntity}), the latest of which is the
 * order as it stands; that step's id is the order's version, so there is no {@code @Version} here.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private PcsCaseEntity pcsCase;

    @Column(nullable = false, updatable = false)
    private UUID authorIdamUserId;

    /** The judge's name as IDAM had it when they started the order, for the order's document. */
    @Column(updatable = false)
    private String authorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState state;

    // Set when the order is issued.

    @Enumerated(EnumType.STRING)
    private OrderSeal seal;

    private Boolean finalOrder;

    private Boolean serveAllParties;

    /** The ids of the parties to serve, when the order is not served on all of them. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> partiesServed;

    private Boolean nextStepsComplete;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "document_id")
    private DocumentEntity document;
}
