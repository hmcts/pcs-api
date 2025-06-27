package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyAgreementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "pcs_case")
@Setter
@Getter
public class PcsCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long caseReference;

    private String applicantForename;

    private String applicantSurname;

    @OneToOne(cascade = ALL)
    private AddressEntity propertyAddress;

    @OneToMany(cascade = ALL, mappedBy = "pcsCase", orphanRemoval = true)
    private Set<CasePossessionGround> possessionGrounds = new HashSet<>();

    @Enumerated(value = EnumType.STRING)
    private TenancyAgreementType tenancyAgreementType;

    private LocalDate tenancyStartDate;

    private Boolean mediationAttempted;

    private Boolean settlementAttempted;

    private BigDecimal rentAmount;

    @Enumerated(value = EnumType.STRING)
    private RentFrequency rentFrequency;

    public void replacePossessionGrounds(Set<CasePossessionGround> possessionGrounds) {
        this.possessionGrounds.forEach(possessionGround -> possessionGround.setPcsCase(null));
        this.possessionGrounds.clear();

        possessionGrounds.forEach(possessionGround -> possessionGround.setPcsCase(this));
        this.possessionGrounds.addAll(possessionGrounds);
    }

}
