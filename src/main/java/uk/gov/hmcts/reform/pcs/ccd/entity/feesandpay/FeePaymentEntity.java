package uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HelpWithFeesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fee_payment")
public class FeePaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "possession_claim_id", nullable = false)
    @JsonBackReference
    private ClaimEntity claim;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    @JsonBackReference
    private PartyEntity party;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime requestDate;

    // Service Request Reference from the createRequest
    private String serviceRequestReference;

    // This is the same as what the user sees - we receive it in the callback so behaves like a correlation id
    private String externalReference;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus paymentStatus;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "hwf_id")
    private HelpWithFeesEntity helpWithFees;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentCallbackHandlerType paymentCallbackHandlerType;

    @JdbcTypeCode(SqlTypes.JSON)
    private String taskData;

    private UUID relatedEntityId;

}
