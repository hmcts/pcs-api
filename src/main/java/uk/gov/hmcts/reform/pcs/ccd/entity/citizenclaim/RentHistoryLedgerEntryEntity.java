package uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim;

import jakarta.persistence.Entity;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "rent_history_ledger_entry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentHistoryLedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private ClaimEntity claim;

    private int rank;

    private LocalDate transactionDate;

    private boolean rentCharge;

    private boolean tenantPayment;

    private String description;

    private BigDecimal amountCharged;

    private BigDecimal amountPaid;

}
