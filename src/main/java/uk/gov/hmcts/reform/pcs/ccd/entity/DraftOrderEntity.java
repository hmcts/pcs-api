package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.idam.IdamUserIdUuidConverter;

import java.util.Map;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/** A judge's draft of an order, until it is issued; its history is in the case event audit, not timestamps. */
@Entity
@Table(name = "draft_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private PcsCaseEntity pcsCase;

    @Column(nullable = false, updatable = false)
    @Convert(converter = IdamUserIdUuidConverter.class)
    private String authorIdamUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftOrderState state;

    @Version
    private long version;

    @Column(nullable = false)
    private String orderType;

    /** The make order form as the judge last submitted it. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> formData;

    /** The order document as the judge last edited it in the frontend's docweave editor. */
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode docweaveSnapshot;
}
