package uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "counter_claim")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Integer version;

    @OneToOne(cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "sot_id")
    @JsonManagedReference
    private StatementOfTruthEntity statementOfTruth;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private PartyEntity party;

    @Enumerated(EnumType.STRING)
    private CounterClaimType claimType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo isClaimAmountKnown;

    private BigDecimal claimAmount;

    private BigDecimal estimatedMaxClaimAmount;

    @Column(name = "counterclaim_for")
    private String counterClaimFor;

    @Column(name = "counterclaim_reasons")
    private String counterClaimReasons;

    private String otherOrderRequestDetails;

    private String otherOrderRequestFacts;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo needHelpWithFees;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo appliedForHwf;

    private String hwfReferenceNumber;

    private String status;

    private LocalDateTime claimSubmittedDate;

    private LocalDateTime claimIssuedDate;

    private LocalDateTime lastModifiedDate;

    @Enumerated(EnumType.STRING)
    private LanguageUsed languageUsed;

    @OneToMany(mappedBy = "counterClaim", cascade = ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<CounterClaimPartyEntity> counterClaimParties = new ArrayList<>();

}
