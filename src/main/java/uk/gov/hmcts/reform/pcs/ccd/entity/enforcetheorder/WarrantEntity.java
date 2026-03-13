package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "enf_warrant")
public class WarrantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "enf_case_id", nullable = false)
    @JsonBackReference
    private EnforcementOrderEntity enforcementOrder;

    // UI Control Flags
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo showPeopleWhoWillBeEvictedPage;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo showPeopleYouWantToEvictPage;

    // Language & Status
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo isSuspendedOrder;

    // Additional Information
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo additionalInformationSelect;

    private String additionalInformationDetails;

    // NameAndAddressForEviction
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo correctNameAndAddress;

    // PeopleToEvict
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo evictEveryone;

    // PropertyAccessDetails
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo isDifficultToAccessProperty;

    private String clarificationOnAccessDifficultyText;

    // Legal Costs & Finances
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo areLegalCostsToBeClaimed;

    private BigDecimal amountOfLegalCosts;

    private BigDecimal amountOwed;

    // Land Registry
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo haveLandRegistryFeesBeenPaid;

    private BigDecimal amountOfLandRegistryFees;

    // Repayment
    private String repaymentChoice;

    private BigDecimal amountOfRepaymentCosts;

    private String repaymentSummaryMarkdown;

    // Defendants DOB
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "defendants_dob_known")
    private VerticalYesNo defendantsDOBKnown;

    @Column(name = "defendants_dob_details")
    private String defendantsDOBDetails;

    // Statement of Truth
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StatementOfTruthCompletedBy completedBy;

    private String agreementClaimant;

    private String fullNameClaimant;

    private String positionClaimant;

    private String agreementLegalRep;

    private String fullNameLegalRep;

    private String firmNameLegalRep;

    private String positionLegalRep;

    private String certification;

    @Enumerated(EnumType.STRING)
    private LanguageUsed languageUsed;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant created;

}
