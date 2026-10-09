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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/** An order as it stood after one change to it. Steps are only ever added; the latest is the order as it stands. */
@Entity
@Immutable
@Table(name = "order_steps")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState state;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private UUID actorIdamUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> formData;

    /** The frontend's Docweave editor state. */
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode docweaveSnapshot;

    /** On the issuing step, the order's wording as issued. */
    private String orderHtml;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "note_id")
    private CaseNoteEntity note;
}
