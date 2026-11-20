package uk.gov.hmcts.reform.pcs.ccd.entity.claim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "claim_payment_history_line")
public class ClaimPaymentHistoryLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    @JsonBackReference
    private ClaimEntity claim;

    private Integer rank;

    private LocalDate paymentDueDate;

    private BigDecimal amountDue;

    private BigDecimal amountPaid;

    private BigDecimal cumulativeArrears;

    @UpdateTimestamp
    private LocalDateTime lastModified;

}
