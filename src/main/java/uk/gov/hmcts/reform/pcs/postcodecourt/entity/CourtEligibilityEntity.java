package uk.gov.hmcts.reform.pcs.postcodecourt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "eligibility_whitelisted_epim")
@Getter
@Setter
public class CourtEligibilityEntity {

    @Id
    @Column(name = "epims_id")
    private Integer epimsId;

    private LocalDate eligibleFrom;

}
