package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Entity
@Table(schema = "ccd", name = "case_data")
@Getter
public class CcdCaseDataEntity {

    @Id
    private Long reference;

    @Enumerated(value = EnumType.STRING)
    private State state;

}
