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
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal;

import java.time.LocalDate;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * An order as the court issued it: its wording as HTML, exported from the order document the
 * caseworker issued, and the document generated from that wording, which is empty until Docmosis
 * has rendered it.
 */
@Entity
@Table(name = "issued_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuedOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private PcsCaseEntity pcsCase;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "draft_order_id", nullable = false, updatable = false)
    private DraftOrderEntity draftOrder;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "order_review_id", nullable = false, updatable = false)
    private OrderReviewEntity orderReview;

    @Column(nullable = false)
    private String orderType;

    @Column(nullable = false)
    private String orderHtml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSeal seal;

    @Column(nullable = false)
    private boolean finalOrder;

    @Column(nullable = false, updatable = false)
    private UUID issuedByIdamUserId;

    @Column(nullable = false)
    private LocalDate issuedOn;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "document_id")
    private DocumentEntity document;
}
