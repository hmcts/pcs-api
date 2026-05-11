package uk.gov.hmcts.reform.pcs.ccd.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "case_flag")
@Setter
@Getter
@AllArgsConstructor
@SuperBuilder
public class CaseFlagEntity extends BaseCaseFlag {

}


