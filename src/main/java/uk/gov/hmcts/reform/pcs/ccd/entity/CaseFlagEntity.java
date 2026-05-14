package uk.gov.hmcts.reform.pcs.ccd.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "case_flag")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CaseFlagEntity extends BaseCaseFlag {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pcs_case_id")
    private PcsCaseEntity pcsCase;
}

