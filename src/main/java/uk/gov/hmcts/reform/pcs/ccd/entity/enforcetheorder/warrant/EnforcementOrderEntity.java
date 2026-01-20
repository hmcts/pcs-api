package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(schema = "enforcement", name = "enf_case")
@Getter
@Setter
public class EnforcementOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    @JsonBackReference
    private ClaimEntity claim;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enforcement_order")
    private EnforcementOrder enforcementOrder;

}
