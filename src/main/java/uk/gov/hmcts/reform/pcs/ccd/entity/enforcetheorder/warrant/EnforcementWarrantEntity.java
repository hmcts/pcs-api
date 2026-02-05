package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "enf_warrant")
public class EnforcementWarrantEntity {

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
    @Column(name = "show_change_name_address_page")
    private VerticalYesNo showChangeNameAddressPage;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "show_people_who_will_be_evicted_page")
    private VerticalYesNo showPeopleWhoWillBeEvictedPage;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "show_people_you_want_to_evict_page")
    private VerticalYesNo showPeopleYouWantToEvictPage;

    // Language & Status
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "is_suspended_order")
    private VerticalYesNo isSuspendedOrder;

    // Additional Information
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "additional_information_select")
    private VerticalYesNo additionalInformationSelect;

    @Column(name = "additional_information_details")
    private String additionalInformationDetails;

    // NameAndAddressForEviction
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "correct_name_and_address")
    private VerticalYesNo correctNameAndAddress;

    // PeopleToEvict
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "evict_everyone")
    private VerticalYesNo evictEveryone;

    // PropertyAccessDetails
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "is_difficult_to_access_property")
    private VerticalYesNo isDifficultToAccessProperty;

    @Column(name = "clarification_on_access_difficulty_text")
    private String clarificationOnAccessDifficultyText;

    // Legal Costs & Finances
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "are_legal_costs_to_be_claimed")
    private VerticalYesNo areLegalCostsToBeClaimed;

    @Column(name = "amount_of_legal_costs", precision = 10, scale = 2)
    private BigDecimal amountOfLegalCosts;

    @Column(name = "amount_owed", precision = 10, scale = 2)
    private BigDecimal amountOwed;

    // Land Registry
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "have_land_registry_fees_been_paid")
    private VerticalYesNo haveLandRegistryFeesBeenPaid;

    @Column(name = "amount_of_land_registry_fees", precision = 10, scale = 2)
    private BigDecimal amountOfLandRegistryFees;

    // Repayment
    @Column(name = "repayment_choice")
    private String repaymentChoice;

    @Column(name = "amount_of_repayment_costs", precision = 10, scale = 2)
    private BigDecimal amountOfRepaymentCosts;

    @Column(name = "repayment_summary_markdown")
    private String repaymentSummaryMarkdown;

    // Defendants DOB
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "defendants_dob_known")
    private VerticalYesNo defendantsDOBKnown;

    @Column(name = "defendants_dob_details")
    private String defendantsDOBDetails;
}
