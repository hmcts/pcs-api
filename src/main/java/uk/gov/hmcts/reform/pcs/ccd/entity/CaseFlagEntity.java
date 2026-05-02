package uk.gov.hmcts.reform.pcs.ccd.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "case_flag")
@Setter
@Getter
@AllArgsConstructor
@Builder
public class CaseFlagEntity extends BaseCaseFlag {

}


