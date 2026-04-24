package uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.IncomeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.RecurrenceFrequency;

import java.math.BigDecimal;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "regular_income_item")
public class RegularIncomeItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "regular_income_id")
    @JsonBackReference
    private RegularIncomeEntity regularIncome;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private IncomeType incomeType;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private RecurrenceFrequency frequency;
}
