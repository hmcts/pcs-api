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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory;

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

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "enf_case_id", nullable = false)
    @JsonBackReference
    private EnforcementOrderEntity enforcementOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "any_risk_to_bailiff")
    private YesNoNotSure anyRiskToBailiff;

    @Enumerated(EnumType.STRING)
    @Column(name = "vulnerable_people_present")
    private YesNoNotSure vulnerablePeoplePresent;

    @Enumerated(EnumType.STRING)
    @Column(name = "vulnerable_category")
    private VulnerableCategory vulnerableCategory;

    @Column(name = "vulnerable_reason_text")
    private String vulnerableReasonText;

    @Column(name = "violent_details")
    private String violentDetails;

    @Column(name = "firearms_details")
    private String firearmsDetails;

    @Column(name = "criminal_details")
    private String criminalDetails;

    @Column(name = "verbal_threats_details")
    private String verbalThreatsDetails;

    @Column(name = "protest_group_details")
    private String protestGroupDetails;

    @Column(name = "police_social_services_details")
    private String policeSocialServicesDetails;

    @Column(name = "animals_details")
    private String animalsDetails;
}
