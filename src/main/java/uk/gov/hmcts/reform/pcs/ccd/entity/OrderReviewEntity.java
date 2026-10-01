package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * A caseworker's review of an order a judge submitted: the query returning it to the judge, or the
 * order as they issued it and how it is to be issued. The judge's order stays as they submitted it.
 * Its history is in the case event audit, like the order's.
 */
@Entity
@Table(name = "order_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "draft_order_id", nullable = false)
    private DraftOrderEntity draftOrder;

    @Column(nullable = false, updatable = false)
    private UUID reviewerIdamUserId;

    /** The version of the order the caseworker reviewed; an order returned more than once has a review of each. */
    @Column(nullable = false, updatable = false)
    private long reviewedVersion;

    /** RETURNED_TO_JUDGE or ISSUED: the state the review left the order in. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftOrderState outcome;

    private String queryToJudge;

    /** The order as the caseworker issued it, which may differ from the judge's in its form or its wording. */
    private String orderType;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> formData;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode docweaveSnapshot;

    private Boolean nextStepsComplete;

    private Boolean finalOrder;

    private Boolean serveAllParties;

    /** The ids of the parties to serve, when the order is not served on all of them. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> partiesServed;

    @Enumerated(EnumType.STRING)
    private OrderSeal seal;
}
