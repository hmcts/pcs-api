package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MoneyJudgment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/** A sum an issued order says its debtors pay its creditors, recorded as court staff issue it. */
@Entity
@Table(name = "money_judgment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyJudgmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private OrderEntity order;

    private BigDecimal arrears;
    private BigDecimal otherAmount;
    private BigDecimal costs;
    private BigDecimal oneOffAmount;
    private LocalDate oneOffDate;
    private LocalDate payBy;
    private BigDecimal instalmentAmount;
    @Enumerated(EnumType.STRING)
    private MoneyJudgment.InstalmentFrequency instalmentFrequency;
    private LocalDate firstInstalmentDate;
    private boolean suspended;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    /** When it stopped being active; none while it is. */
    private LocalDate statusDate;

    /** Each party it is between, by id, as a debtor or a creditor. */
    @ElementCollection
    @CollectionTable(name = "money_judgment_party", joinColumns = @JoinColumn(name = "judgment_id"))
    @MapKeyColumn(name = "party_id")
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private Map<UUID, Role> parties = new HashMap<>();

    public static MoneyJudgmentEntity of(OrderEntity order, MoneyJudgment judgment) {
        MoneyJudgmentEntity entity = MoneyJudgmentEntity.builder()
            .order(order)
            .arrears(judgment.arrears())
            .otherAmount(judgment.otherAmount())
            .costs(judgment.costs())
            .oneOffAmount(judgment.oneOffAmount())
            .oneOffDate(judgment.oneOffDate())
            .payBy(judgment.payBy())
            .instalmentAmount(judgment.instalmentAmount())
            .instalmentFrequency(judgment.instalmentFrequency())
            .firstInstalmentDate(judgment.firstInstalmentDate())
            .suspended(judgment.suspended())
            .build();
        judgment.debtors().forEach(party -> entity.parties.put(party, Role.DEBTOR));
        judgment.creditors().forEach(party -> entity.parties.put(party, Role.CREDITOR));
        return entity;
    }

    public enum Status {
        ACTIVE,
        SATISFIED,
        CANCELLED,
        SET_ASIDE
    }

    public enum Role {
        DEBTOR,
        CREDITOR
    }
}
