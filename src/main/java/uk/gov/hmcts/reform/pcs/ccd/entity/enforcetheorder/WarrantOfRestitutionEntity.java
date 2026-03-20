package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "enf_warrant_of_restitution")
public class WarrantOfRestitutionEntity {

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

    // PeopleToEvict
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo evictEveryone;

    // Defendants Returned
    private String howDefendantsReturned;

    // Additional Information
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo additionalInformationSelect;

    private String additionalInformationDetails;

    // PropertyAccessDetails
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo isDifficultToAccessProperty;

    private String clarificationOnAccessDifficultyText;
}
