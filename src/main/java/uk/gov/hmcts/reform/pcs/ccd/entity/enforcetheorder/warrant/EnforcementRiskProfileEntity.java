package uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.warrant;

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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;

import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "enf_risk_profile")
@Getter
@Setter
public class EnforcementRiskProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "enf_case_id", nullable = false)
    @JsonBackReference
    private EnforcementOrderEntity enforcementOrder;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesNoNotSure anyRiskToBailiff;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private YesNoNotSure vulnerablePeoplePresent;

    @Enumerated(EnumType.STRING)
    private VulnerableCategory vulnerableCategory;

    private String vulnerableReasonText;

    private String violentDetails;

    private String firearmsDetails;

    private String criminalDetails;

    private String verbalThreatsDetails;

    private String protestGroupDetails;

    private String policeSocialServicesDetails;

    private String animalsDetails;
}
